package com.jiuzhang.seckill.service.cache;

import com.jiuzhang.seckill.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;

@Slf4j
@Service
public class RedisService {

    @Autowired
    private JedisPool jedisPool;

    public void setValue(String key, String value) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.set(key, value);
        jedisClient.close();
    }

    public String getValue(String key) {
        Jedis jedisClient = jedisPool.getResource();
        String value = jedisClient.get(key);
        jedisClient.close();
        return value;
    }

    /**
     * 判断和扣减缓存中的库存（使用 Lua 脚本）
     *
     * @return 库存是否扣减成功
     */
    public boolean deductStockFromActivity(long seckillActivityId) {
        String key = Constant.REDIS_KEY_AVAIL_STOCK + seckillActivityId;

        try (Jedis jedisClient = jedisPool.getResource()) {
            String luaScript = "if redis.call('exists', KEYS[1]) == 1 then\n" +
                    "               local stock = tonumber(redis.call('get', KEYS[1]));\n" +
                    "               if (stock <= 0) then\n" +
                    "                   return -1;\n" +
                    "               end;\n" +
                    "               redis.call('decr', KEYS[1]);\n" +
                    "               return stock - 1;\n" +
                    "           end;\n" +
                    "           return -1;";

            Long stock = (Long) jedisClient.eval(luaScript, Collections.singletonList(key), Collections.emptyList());
            if (stock < 0) {
                System.out.println("库存不足，抢购失败");
                return false;
            }

            System.out.println("抢购成功");
            return true;
        } catch (Throwable throwable) {
            System.out.println("库存扣减失败：" + throwable);
            return false;
        }
    }

    /**
     * 超时未支付，库存回滚
     */
    public void revertStock(String key) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.incr(key);
        jedisClient.close();
    }

    /**
     * 判断用户是否在某秒杀活动的已购名单中
     */
    public boolean isInPurchasedList(long seckillActivityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        boolean sismember = jedisClient.sismember(Constant.REDIS_KEY_PURCHASED_LIST + seckillActivityId, String.valueOf(userId));
        jedisClient.close();
        log.info("userId:{}, activityId:{}, 在已购名单中:{}", userId, seckillActivityId, sismember);
        return sismember;
    }

    /**
     * 添加用户至某秒杀活动的已购名单
     */
    public void addToPurchasedList(long seckillActivityId, long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.sadd(Constant.REDIS_KEY_PURCHASED_LIST + seckillActivityId, String.valueOf(userId));
        jedisClient.close();
    }

    /**
     * 将用户从某秒杀活动的已购名单中移除
     */
    public void removeFromPurchasedList(Long seckillActivityId, Long userId) {
        Jedis jedisClient = jedisPool.getResource();
        jedisClient.srem(Constant.REDIS_KEY_PURCHASED_LIST + seckillActivityId, String.valueOf(userId));
        jedisClient.close();
    }

    /**
     * 获取分布式锁
     *
     * @param lockKey  锁的 Key
     * @param requestId  请求标识（锁的值 Value）
     * @param expireTime  锁的有效时间（毫秒）
     * @return 是否加锁成功
     */
    public boolean tryGetDistributedLock(String lockKey, String requestId, int expireTime) {
        Jedis jedisClient = jedisPool.getResource();
        SetParams params = new SetParams();
        params.nx(); // 只有 Key 不存在时才 set (NX = only set if not exist, XX = only set if exist)
        params.px(expireTime); // 设置锁的有效时间（PX = 毫秒，EX = 秒）
        String result = jedisClient.set(lockKey, requestId, params);
        jedisClient.close();
        return "OK".equals(result);
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey  锁的 Key
     * @param requestId  请求标识（锁的值 Value）
     * @return 是否释放成功
     */
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        Jedis jedisClient = jedisPool.getResource();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"; // 锁的 Key 和 Value 都相同才能解锁
        Long result = (Long) jedisClient.eval(script, Collections.singletonList(lockKey) /* 对应 KEYS[] */, Collections.singletonList(requestId) /* 对应 ARGV[] */);
        jedisClient.close();
        return result == 1L;
    }

}
