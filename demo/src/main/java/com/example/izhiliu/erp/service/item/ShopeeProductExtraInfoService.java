package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.ShopeeProductExtraInfo;
import com.izhiliu.erp.service.item.dto.ShopeeProductExtraInfoDto;

import java.util.List;

/**
 * Service Interface for managing ShopeeProductMedia.
 */
public interface ShopeeProductExtraInfoService extends IBaseService<ShopeeProductExtraInfo, ShopeeProductExtraInfoDto> {

    ShopeeProductExtraInfoDto selectByProductId(Long productId);

    void saveOrUpdateCache(ShopeeProductExtraInfoDto shopeeProductMediaDTO);

    void updateByProductId(ShopeeProductExtraInfoDto entity);

    List<ShopeeProductExtraInfoDto> selectMainFinalInfoByProductId(List<Long> productIds);

    int deleteByProduct(Long products);
}
