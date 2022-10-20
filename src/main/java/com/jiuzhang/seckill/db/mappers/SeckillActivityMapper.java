package com.jiuzhang.seckill.db.mappers;

import com.jiuzhang.seckill.db.po.SeckillActivity;

import java.util.List;

public interface SeckillActivityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SeckillActivity row);

    int insertSelective(SeckillActivity row);

    SeckillActivity selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(SeckillActivity row);

    int updateByPrimaryKey(SeckillActivity row);

    List<SeckillActivity> querySeckillActivitiesByStatus(int activityStatus);

    /**
     * 扣减并锁定库存
     *
     * @return MyBatis 中，Update 操作的返回值表示的是 Update 的 Where 条件匹配的个数
     */
    int lockStock(Long seckillActivityId);

    int deductLockedStock(Long seckillActivityId);

    void revertStock(Long seckillActivityId);
}