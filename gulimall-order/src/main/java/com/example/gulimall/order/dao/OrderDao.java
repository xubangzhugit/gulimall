package com.example.gulimall.order.dao;

import com.example.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 16:16:03
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
