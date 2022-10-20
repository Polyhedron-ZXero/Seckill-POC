package com.jiuzhang.seckill.db.mappers;

import com.jiuzhang.seckill.db.po.SeckillCommodity;

public interface SeckillCommodityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SeckillCommodity row);

    int insertSelective(SeckillCommodity row);

    SeckillCommodity selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(SeckillCommodity row);

    int updateByPrimaryKey(SeckillCommodity row);
}