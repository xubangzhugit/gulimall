package com.izhiliu.erp.service.item.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.domain.item.ShopeeProductExtraInfo;
import com.izhiliu.erp.repository.item.ShopeeProductExtraInfoRepository;
import com.izhiliu.erp.service.item.ShopeeProductExtraInfoService;
import com.izhiliu.erp.service.item.dto.ShopeeProductExtraInfoDto;
import com.izhiliu.erp.service.item.mapper.ShopeeProductExtraInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service Implementation for managing ShopeeProductMedia.
 */
@Primary
@Service
public class ShopeeProductExtraInfoServiceImpl extends IBaseServiceImpl<ShopeeProductExtraInfo, ShopeeProductExtraInfoDto, ShopeeProductExtraInfoRepository, ShopeeProductExtraInfoMapper> implements ShopeeProductExtraInfoService {

    private final Logger log = LoggerFactory.getLogger(ShopeeProductExtraInfoServiceImpl.class);
    private final static String CACHE_NAME = "ShopeeProductMediaService";

    @Override
    @Transactional(readOnly = true)
    public ShopeeProductExtraInfoDto selectByProductId(Long productId) {
        return mapper.toDto(repository.selectByProductId(productId));
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#p0.productId")
    public void saveOrUpdateCache(ShopeeProductExtraInfoDto shopeeProductMediaDTO) {
        super.saveOrUpdate(shopeeProductMediaDTO);
    }

    @Override
    public void updateByProductId(ShopeeProductExtraInfoDto dto) {
        repository.updateByProductId(mapper.toEntity(dto));
    }

    @Override
    public List<ShopeeProductExtraInfoDto> selectMainFinalInfoByProductId(List<Long> productIds) {
        if(productIds.isEmpty()){
            return  Collections.emptyList();
        }
        return mapper.toDto(repository.selectMainFinalInfoByProductId(productIds));
    }

    @Override
    public int deleteByProduct(Long productId) {
        return repository.delete(new QueryWrapper<>(new ShopeeProductExtraInfo().setProductId(productId)));
    }
}
