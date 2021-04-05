package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.ShopeeProductMedia;
import com.izhiliu.erp.service.item.dto.ProductImageDto;
import com.izhiliu.erp.service.item.dto.ProductImageResult;
import com.izhiliu.erp.service.item.dto.ShopeeProductMediaDTO;

import java.util.List;

/**
 * Service Interface for managing ShopeeProductMedia.
 */
public interface ShopeeProductMediaService extends IBaseService<ShopeeProductMedia, ShopeeProductMediaDTO> {

    ShopeeProductMediaDTO selectByProductId(Long productId);

    ShopeeProductMediaDTO selectByProductIdNotCache(Long productId);
    List<ShopeeProductMediaDTO> selectMainImagsByProductId(List<Long> productIds);

    void batchSaveOrUpdateAndCleanCache(List<ShopeeProductMediaDTO> shopeeProductMediaDTOList);

    ShopeeProductMediaDTO updateAndCleanCache(ShopeeProductMediaDTO shopeeProductMediaDTO);

    List<ProductImageResult> selectProductImageBySkuCode(List<ProductImageDto> productImageDtos);


    List<ShopeeProductMedia> selectByPriceRange(Integer page);

    boolean deleteByProductId(Long productId);

    void updateByProductId(ShopeeProductMediaDTO dto);

    void updateByProductIds(List<ShopeeProductMediaDTO> shopeeProductMediaDTOList);
}