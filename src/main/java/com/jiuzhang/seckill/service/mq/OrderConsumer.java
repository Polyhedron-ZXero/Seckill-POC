package com.jiuzhang.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.jiuzhang.seckill.db.dao.OrderDao;
import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.Order;
import com.jiuzhang.seckill.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * "@Component"：用于标记类为 Spring 组件，可被扫描并加载到 Spring 容器中。
 * <p>
 * MQ 的两种消费模式：
 * 1. 广播消息消费 - 所有订阅的消费者都能消费
 * 2. 集群消息消费 - 在同一个消费者组 (consumerGroup) 里只能有一个消费者消费（每个消费者组都会由组里的任意一个消费者消费一次）
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = Constant.MQ_TOPIC_ORDER, consumerGroup = Constant.MQ_GROUP_DEFAULT)
public class OrderConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Override
    @Transactional // 参与 Spring 的事务管理（使用默认配置）：方法抛出异常后，Spring 会自动回滚事务，数据不会插入到数据库
    public void onMessage(MessageExt messageExt) {
        // 解析创建订单请求消息
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("接收到创建订单请求：" + message);
        Order order = JSON.parseObject(message, Order.class);
        order.setCreateTime(new Date());

        // 扣减库存
        boolean stockLocked = seckillActivityDao.lockStock(order.getSeckillActivityId());
        if (stockLocked) {
            order.setOrderStatus(Constant.ORDER_STATUS_UNPAID);
            log.info("库存锁定成功");
        } else {
            order.setOrderStatus(Constant.ORDER_STATUS_NO_STOCK);
            log.info("库存不足，无法锁定库存");
        }

        // 插入订单
        orderDao.insertOrder(order);
    }

}
