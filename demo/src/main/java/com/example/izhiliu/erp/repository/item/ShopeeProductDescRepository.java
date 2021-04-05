package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.ShopeeProductDesc;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * Spring Data  repository for the ShopeeProductDesc entity.
 */
@Mapper
public interface ShopeeProductDescRepository extends BaseMapper<ShopeeProductDesc> {

    ShopeeProductDesc selectByProductId(@Param("productId") Long productId);

    int deleteByDeleted(@Param("productId") Long productId);

    int deleteByProductId(Long productId);

    int deleteByProductIds(@Param("productIds") List<Long> productIds);

}
