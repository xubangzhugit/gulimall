package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.ShopeeProductDesc;
import com.izhiliu.erp.service.item.dto.ShopeeProductDescDTO;
import com.izhiliu.erp.service.item.impl.ShopeeProductDescServiceImpl;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

/**
 * Service Interface for managing Platform.
 */
public interface ShopeeProductDescService extends IBaseService<ShopeeProductDesc,ShopeeProductDescDTO> {

    ShopeeProductDescDTO selectByProductId(Long productId);

    void batchSaveOrUpdateAndCleanCache(List<ShopeeProductDescDTO> shopeeProductDescDTOList);

    ShopeeProductDescDTO updateAndCleanCache(ShopeeProductDescDTO shopeeProductDescDTO);

    boolean deleteByProductId(Long productId);
}
