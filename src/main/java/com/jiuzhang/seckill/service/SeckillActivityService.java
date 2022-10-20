package com.jiuzhang.seckill.service;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.dao.SeckillCommodityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.db.po.SeckillCommodity;
import com.jiuzhang.seckill.service.cache.RedisService;
import com.jiuzhang.seckill.service.mq.RocketMQService;
import com.jiuzhang.seckill.util.Constant;
import com.jiuzhang.seckill.util.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class SeckillActivityService {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RocketMQService rocketMQService;

    @Autowired
    OrderDao orderDao;


    /**
     * ID 在分布式环境中可以从机器配置上读取，单机开发环境中先写死
     */
    private final SnowFlake snowFlake = new SnowFlake(1, 1);

    /**
     * 判断商品是否还有库存（有库存则扣减缓存内的库存）
     */
    public boolean deductStockInCache(long activityId) {
        return redisService.deductStockFromActivity(activityId);
    }

    /**
     * 创建订单
     */
    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        // 创建订单
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();

        // 雪花算法生成订单 ID
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(seckillActivity.getId());
        order.setUserId(userId);
        order.setOrderAmount(seckillActivity.getSeckillPrice().longValue());

        // 发送创建订单消息
        rocketMQService.sendMessage(Constant.MQ_TOPIC_ORDER, JSON.toJSONString(order));

        // 发送订单付款状态校验消息（延时 10 秒进行订单校验，没付款则判断为超时订单）
        rocketMQService.sendDelayMessage(Constant.MQ_TOPIC_PAYMENT, JSON.toJSONString(order), 3);

        return order;
    }

    /**
     * 简单处理抢购请求（无法正确处理并行请求，会造成库存超卖）
     */
    public String processSeckill(long activityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(activityId);
        int availableStock = seckillActivity.getAvailableStock();
        String result;

        if (availableStock > 0) {
            result = "恭喜，抢购成功！";
            System.out.println(result);
            availableStock--;
            seckillActivity.setAvailableStock(availableStock);
            seckillActivityDao.updateSeckillActivity(seckillActivity);
        } else {
            result = "抱歉，抢购失败，商品被抢完了…";
            System.out.println(result);
        }

        return result;
    }

    /**
     * 模拟订单支付
     *
     * @return 是否支付成功
     */
    public boolean payOrderProcess(String orderNo) throws Exception {
        Order order = orderDao.queryOrder(orderNo);

        if (order == null) {
            log.error("订单号对应订单不存在：" + orderNo);
            return false;
        } else if (order.getOrderStatus() == Constant.ORDER_STATUS_TIMEOUT) {
            log.info("无法支付超时订单，订单号：" + orderNo);
            return false;
        } else if (order.getOrderStatus() != Constant.ORDER_STATUS_UNPAID) {
            log.info("订单状态无效：" + orderNo);
            return false;
        }

        log.info("正在支付……");
        log.info("订单支付成功，订单号：" + orderNo);

        order.setPayTime(new Date());
        order.setOrderStatus(Constant.ORDER_STATUS_PAID);
        orderDao.updateOrder(order);

        // 发送订单付款成功消息
        rocketMQService.sendMessage(Constant.MQ_TOPIC_PAYMENT_COMPLETED, JSON.toJSONString(order));
        return true;
    }

    /**
     * 将指定秒杀活动的相关数据导入 Redis 缓存
     * （按需手动执行 SeckillApplicationTest -> preheatCacheWithSeckillData()）
     */
    public void pushSeckillDataToCache(long seckillActivityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        redisService.setValue(Constant.REDIS_KEY_ACTIVITY_DATA + seckillActivityId, JSON.toJSONString(seckillActivity));
        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        redisService.setValue(Constant.REDIS_KEY_COMMODITY_DATA + seckillActivity.getCommodityId(), JSON.toJSONString(seckillCommodity));
    }

}
