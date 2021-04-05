package com.izhiliu.erp.service.item;

import com.izhiliu.erp.domain.item.ItemCategoryMap;

public interface CategoryMapService {


    ItemCategoryMap selectByObj(Long mateDataplatform, Long mateDataplatformNodeId, Long mateDataCategoryId, Long productPlatform, Long productPlatformNodeId);
}
