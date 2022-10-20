package com.jiuzhang.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.service.cache.RedisService;
import com.jiuzhang.seckill.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQMessageListener(topic = Constant.MQ_TOPIC_PAYMENT, consumerGroup = Constant.MQ_GROUP_PAYMENT)
public class PayStatusCheckListener implements RocketMQListener<MessageExt> {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Resource
    private RedisService redisService;

    @Override
    @Transactional
    public void onMessage(MessageExt messageExt) {
        // 查询并判断订单是否完成支付
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("接收到订单支付状态校验请求：" + message);
        Order order = JSON.parseObject(message, Order.class);
        Order orderInfo = orderDao.queryOrder(order.getOrderNo());

        if (orderInfo.getOrderStatus() != Constant.ORDER_STATUS_PAID) {
            log.info("时限内未完成支付，关闭订单：" + orderInfo.getOrderNo());
            orderInfo.setOrderStatus(Constant.ORDER_STATUS_TIMEOUT);
            orderDao.updateOrder(orderInfo);
            // 恢复数据库库存
            seckillActivityDao.revertStock(order.getSeckillActivityId());
            // 恢复 Redis 库存
            redisService.revertStock(Constant.REDIS_KEY_AVAIL_STOCK + order.getSeckillActivityId());
            // 将用户从已购名单中移除
            redisService.removeFromPurchasedList(order.getSeckillActivityId(), order.getUserId());
        }
    }

}
