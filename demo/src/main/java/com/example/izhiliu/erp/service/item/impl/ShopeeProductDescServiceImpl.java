package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.domain.item.ShopeeProductDesc;
import com.izhiliu.erp.repository.item.ShopeeProductDescRepository;
import com.izhiliu.erp.service.item.ShopeeProductDescService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDescDTO;
import com.izhiliu.erp.service.item.mapper.ShopeeProductDescMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * Service Implementation for managing ShopeeProduct.
 */
@Primary
@Service
public class ShopeeProductDescServiceImpl extends IBaseServiceImpl<ShopeeProductDesc, ShopeeProductDescDTO, ShopeeProductDescRepository, ShopeeProductDescMapper> implements ShopeeProductDescService {

    private final Logger log = LoggerFactory.getLogger(ShopeeProductDescServiceImpl.class);
    private final static String CACHE_NAME = "ShopeeProductDescService";

    @Resource
    CacheManager cacheManager;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, unless = "#result == null")
    public ShopeeProductDescDTO selectByProductId(Long productId) {
        return mapper.toDto(repository.selectByProductId(productId));
    }

    @Override
    public void batchSaveOrUpdateAndCleanCache(List<ShopeeProductDescDTO> shopeeProductDescDTOList) {
        super.batchSaveOrUpdate(shopeeProductDescDTOList);
        clearCacheBatch(shopeeProductDescDTOList);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#p0.productId")
    public ShopeeProductDescDTO updateAndCleanCache(ShopeeProductDescDTO shopeeProductDescDTO) {
        if (super.update(shopeeProductDescDTO)) {
            return shopeeProductDescDTO;
        } else {
            return null;
        }
    }

    @Override
    public boolean deleteByProductId(Long productId) {
       return SqlHelper.delBool(repository.deleteByProductId(productId));
    }

    private void clearCacheBatch(List<ShopeeProductDescDTO> shopeeProductDescDTOList) {
        shopeeProductDescDTOList.forEach(shopeeProductMediaDTO -> {
            if (null != cacheManager.getCache(CACHE_NAME)) {
                cacheManager.getCache(CACHE_NAME).evict(shopeeProductMediaDTO.getProductId());
            }
        });
    }


}
