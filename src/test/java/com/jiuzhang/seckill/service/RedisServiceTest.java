package com.jiuzhang.seckill.service;

import com.jiuzhang.seckill.service.cache.RedisService;
import com.jiuzhang.seckill.util.Constant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 需要先启动 Redis (redis-server.exe)
 */
@SpringBootTest
public class RedisServiceTest {

    private static final int SECKILL_ID = 1;
    private static final long DEFAULT_STOCK = 10L;
    private static final String KEY_AVAIL_STOCK = Constant.REDIS_KEY_AVAIL_STOCK + SECKILL_ID;

    @Resource
    private RedisService redisService;

    @BeforeEach
    public void setup() {
        redisService.setValue(KEY_AVAIL_STOCK, String.valueOf(DEFAULT_STOCK));
    }

    @Test
    public void testSetStock() {
        redisService.setValue(KEY_AVAIL_STOCK, String.valueOf(DEFAULT_STOCK));
        String stock = redisService.getValue(KEY_AVAIL_STOCK);
        assertEquals(DEFAULT_STOCK, Long.valueOf(stock));
        System.out.println(stock);
    }

    @Test
    public void testDeductStockFromActivity() {
        boolean result = redisService.deductStockFromActivity(SECKILL_ID);
        assertTrue(result);
        String stock = redisService.getValue(KEY_AVAIL_STOCK);
        assertEquals(DEFAULT_STOCK - 1, Long.valueOf(stock));
    }

    /**
     * 测试高并发下获取锁的结果
     */
    @Test
    public void testConcurrentAcquireLock() {
        String requestId = UUID.randomUUID().toString();
        boolean lockAcquired = redisService.tryGetDistributedLock("A", requestId,1000);
        assertTrue(lockAcquired);

        for (int i = 0; i < 10; i++) {
            boolean lockNotAcquired = !redisService.tryGetDistributedLock("A", requestId,1000);
            assertTrue(lockNotAcquired);
        }

        redisService.releaseDistributedLock("A", requestId);
        lockAcquired = redisService.tryGetDistributedLock("A", requestId,1000);
        assertTrue(lockAcquired);
        boolean lockReleased = redisService.releaseDistributedLock("A", requestId);
        assertTrue(lockReleased);
    }

}
