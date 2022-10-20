package com.jiuzhang.seckill.component;

import com.jiuzhang.seckill.db.dao.SeckillActivityDao;
import com.jiuzhang.seckill.db.po.SeckillActivity;
import com.jiuzhang.seckill.service.cache.RedisService;
import com.jiuzhang.seckill.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisPreheatRunner implements ApplicationRunner {

    @Autowired
    RedisService redisService;

    @Autowired
    SeckillActivityDao seckillActivityDao;

    /**
     * 启动项目时向 Redis 存入商品库存
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitiesByStatus(1);
        for (SeckillActivity seckillActivity : seckillActivities) {
            redisService.setValue(Constant.REDIS_KEY_AVAIL_STOCK + seckillActivity.getId(), seckillActivity.getAvailableStock().toString());
        }
    }
}
