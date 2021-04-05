package com.izhiliu.erp.service.item.impl;

import com.izhiliu.erp.domain.item.ItemCategoryMap;
import com.izhiliu.erp.repository.item.CategoryMapRepository;
import com.izhiliu.erp.service.item.CategoryMapService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CategoryMapServiceImpl implements CategoryMapService {

    @Resource
    CategoryMapRepository categoryMapRepository;


    @Override
    public ItemCategoryMap selectByObj(Long mateDataplatform, Long mateDataplatformNodeId, Long mateDataCategoryId, Long productPlatform, Long productPlatformNodeId) {
        return categoryMapRepository.selectOneByObj(mateDataplatform,mateDataplatformNodeId,mateDataCategoryId,productPlatform,productPlatformNodeId);
    }
}
