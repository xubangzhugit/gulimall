package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;


/**
 * Spring Data  repository for the ShopeeProductSku entity.
 */
@Mapper
public interface ShopeeProductSkuRepository extends BaseMapper<ShopeeProductSku> {

    IPage<ShopeeProductSku> pageByProductId(Page page, @Param("productId") Long productId);

    ShopeeProductSku findByVariationId(Long variationId);

    List<ShopeeProductSku> listByVariationIds(List<Long> list);

    List<ShopeeProductSku> findSkuListByloginId(@Param("loginId") String loginId, @Param("skuCode") String skuCode);

    List<ShopeeProductSku> selectByProductIds(@Param("productIds") List<Long> productIds);

    List<ShopeeProductSku> selectImagesBySkuCodes(@Param("skuCodes") Collection<String> skuCodes, @Param("productId") Long productId);

    int deleteByDeleted(@Param("productId") Long productId);

    int deleteByProductIds(@Param("productIds") List<Long> productIds);

    int updateByIdPlus(@Param("productSku") ShopeeProductSku productSku);

    IPage<ShopeeProductSku> findSkuListByloginIdAndSkuCode(@Param("page") Page page, @Param("skuCode") String skuCode);

    void clearInvalidVariationId(@Param("invalidVariationIds") List<Long> invalidVariationIds);

    int updateExcludeOriginalPrice(ShopeeProductSku toEntity);

    int saveExcludeOriginalPrice(ShopeeProductSku toEntity);

    List<ShopeeProductSku> selectByItemIds(@Param("list") List<Long> list);

    boolean deleteByVariationIdAndProductId(@Param("productId") Long productId, @Param("variationIds") List<Long> variationIds);

}
