package com.izhiliu.erp.repository.discount;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.izhiliu.erp.domain.discount.ShopeeDiscountDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Set;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:25
 */
@Mapper
public interface DiscountDetailRepository extends BaseMapper<ShopeeDiscountDetail> {

    boolean batchDeleteIds(@Param("ids") Set<Long> ids);
}
