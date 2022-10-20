package com.jiuzhang.seckill.service;

import com.jiuzhang.seckill.service.mq.RocketMQService;
import com.jiuzhang.seckill.util.Constant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
public class RocketMQServiceTest {

    @Autowired
    RocketMQService rocketMQService;

    @Test
    public void testSendMessage() throws Exception {
        rocketMQService.sendMessage(Constant.MQ_TOPIC_ORDER, "Hello World! " + new Date());
    }

}
