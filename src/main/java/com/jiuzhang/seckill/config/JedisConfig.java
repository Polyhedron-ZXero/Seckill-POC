package com.jiuzhang.seckill.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * "@Configuration"：用于定义配置类，是 XML 配置文件的替代。
 * "@Value"：读取配置文件 (application.properties) 中的值，赋值给对应的变量。
 * "@Bean"：将返回的对象放入 Spring Bean 容器中。
 */
@Configuration
public class JedisConfig extends CachingConfigurerSupport {

    private final Logger logger = LoggerFactory.getLogger(JedisConfig.class);

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.timeout}")
    private int timeout;

    @Value("${spring.redis.pool.max-active}")
    private int maxActive;

    @Value("${spring.redis.pool.max-idle}")
    private int maxIdle;

    @Value("${spring.redis.pool.min-idle}")
    private int minIdle;

    @Value("${spring.redis.pool.max-wait}")
    private long maxWaitMillis;

    @Bean
    public JedisPool redisPoolFactory() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
        jedisPoolConfig.setMaxTotal(maxActive);
        jedisPoolConfig.setMinIdle(minIdle);
//        jedisPoolConfig.setJmxEnabled(false); // 或在 “运行/调试配置” 中勾选 “禁用 JMX 代理”
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, null);
        logger.info("JedisPool 注入成功！");
        logger.info(String.format("Redis 地址：%s:%d", host, port));
        return jedisPool;
    }

}
