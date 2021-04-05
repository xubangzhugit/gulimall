package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.ShopeeProductAttributeValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * Spring Data  repository for the ShopeeProductAttributeValue entity.
 */
@Mapper
public interface ShopeeProductAttributeValueRepository extends BaseMapper<ShopeeProductAttributeValue> {

    IPage<ShopeeProductAttributeValue> pageByAttributeId(Page page, @Param("shopeeAttributeId") Long attributeId);

    List<ShopeeProductAttributeValue> selectByProductId(@Param("productId") Long productId);

    int deleteByAttributeId(long attributeId);

    int deleteByAttributeIdAndValue(@Param("attributeId") long attributeId, @Param("value") String value);

    int deleteByProductId(Long productId);

    int deleteByProductIds(@Param("productIds") List<Long> productId);

    int deleteByDeleted(@Param("productId") Long productId);
}
