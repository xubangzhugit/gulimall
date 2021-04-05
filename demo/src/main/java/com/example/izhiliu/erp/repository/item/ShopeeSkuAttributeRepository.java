package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.ShopeeSkuAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * Spring Data  repository for the ShopeeSkuAttribute entity.
 */
@Mapper
public interface ShopeeSkuAttributeRepository extends BaseMapper<ShopeeSkuAttribute> {
    int deleteByDeleted(@Param("productId") Long productId);

    IPage<ShopeeSkuAttribute> pageByProductId(Page page, @Param("productId") Long productId);

    List<ShopeeSkuAttribute> selectByProductIds(@Param("productIds")  List<Long> productIds);
}
