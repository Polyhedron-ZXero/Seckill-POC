package com.jiuzhang.seckill;

import com.jiuzhang.seckill.service.SeckillActivityService;
import com.jiuzhang.seckill.service.StaticHtmlPageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * "@SpringBootTest"：一般情况下，使用此注解后，Spring 将加载所有被管理的 Bean，基本等同于启动了整个服务，此时便可以开始功能测试。
 */
@SpringBootTest
class SeckillApplicationTest {

    private static final int SECKILL_ID = 27;

    @Resource
    SeckillActivityService seckillActivityService;

    @Resource
    StaticHtmlPageService staticHtmlPageService;

    @Test
    void preheatCacheWithSeckillData() {
        seckillActivityService.pushSeckillDataToCache(SECKILL_ID);
    }

    @Test
    void createSeckillItemStaticHtmlPage() {
        staticHtmlPageService.createSeckillItemStaticHtmlPage(SECKILL_ID);
    }

}
