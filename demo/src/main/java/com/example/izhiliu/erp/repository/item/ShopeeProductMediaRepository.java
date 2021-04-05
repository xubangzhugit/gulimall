package com.izhiliu.erp.repository.item;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.izhiliu.erp.domain.item.ShopeeProductMedia;
import com.izhiliu.erp.service.item.dto.ShopeeProductMediaDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * Spring Data  repository for the ShopeeProductDesc entity.
 */
@Mapper
public interface ShopeeProductMediaRepository extends BaseMapper<ShopeeProductMedia> {

    ShopeeProductMedia selectByProductId(@Param("productId") Long productId);
    ShopeeProductMedia selectImagsByProductId(@Param("productId") Long productId);

    List<ShopeeProductMedia> selectImagsByProductIds(@Param("productIds") List<Long> productIds);

    ShopeeProductMedia selectImageBySkuCode(@Param("skuCode") String skuCode);

    int deleteByDeleted(@Param("productId") Long productId);

    List<ShopeeProductMedia> selectByPriceRange(@Param("page") Integer page);

    int deleteByProductId(Long productId);

    int deleteByProductIds(@Param("productIds") List<Long> productIds);


    int updateByProductId(@Param("shopeeProductMedia") ShopeeProductMedia shopeeProductMedia);

    int updateByProductIds(@Param("shopeeProductMedias") List<ShopeeProductMedia> toEntity);
}
