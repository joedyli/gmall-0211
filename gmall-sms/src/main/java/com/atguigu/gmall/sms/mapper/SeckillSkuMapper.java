package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.SeckillSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀活动商品关联
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2020-07-20 14:28:31
 */
@Mapper
public interface SeckillSkuMapper extends BaseMapper<SeckillSkuEntity> {
	
}
