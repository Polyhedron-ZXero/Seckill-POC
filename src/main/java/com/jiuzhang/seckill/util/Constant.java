package com.jiuzhang.seckill.util;

public class Constant {

    public static final String REDIS_KEY_AVAIL_STOCK = "seckill_stock:";
    public static final String REDIS_KEY_PURCHASED_LIST = "seckill_purchased_users:";
    public static final String REDIS_KEY_ACTIVITY_DATA = "seckill_activity_data:";
    public static final String REDIS_KEY_COMMODITY_DATA = "seckill_commodity_data:";
    public static final String MQ_TOPIC_ORDER = "seckill-order";
    public static final String MQ_TOPIC_PAYMENT = "seckill-payment";
    public static final String MQ_TOPIC_PAYMENT_COMPLETED = "seckill-payment-completed";
    public static final String MQ_GROUP_DEFAULT = "consumer-group-jiuzhang";
    public static final String MQ_GROUP_PAYMENT = "consumer-group-payment";
    public static final String MQ_GROUP_PAYMENT_COMPLETED = "consumer-group-payment-completed";
    public static final String FLOW_RULE_SECKILL_LIST = "SeckillActivityList";
    public static final String FLOW_RULE_PLACE_ORDER = "SeckillPlaceOrder";

    /**
     * 订单无库存，已关闭
     */
    public static final int ORDER_STATUS_NO_STOCK = 0;
    /**
     * 订单待付款
     */
    public static final int ORDER_STATUS_UNPAID = 1;
    /**
     * 订单已支付
     */
    public static final int ORDER_STATUS_PAID = 2;
    /**
     * 订单超时未付款，已关闭
     */
    public static final int ORDER_STATUS_TIMEOUT = 99;
}
