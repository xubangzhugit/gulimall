package com.izhiliu.erp.service.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.ShopeeSkuAttribute;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;

import java.util.List;

/**
 * Service Interface for managing ShopeeSkuAttribute.
 */
public interface ShopeeSkuAttributeService extends IBaseService<ShopeeSkuAttribute, ShopeeSkuAttributeDTO> {

    String CACHE_ONE = "shopee-sku-attribute-one";
    String CACHE_LIST = "shopee-sku-attribute-list";
    String CACHE_PAGE = "shopee-sku-attribute-page";
    String CACHE_PAGE$ = "shopee-sku-attribute-page$";

    int deleteByProduct(long productId);

    IPage<ShopeeSkuAttributeDTO> pageByProduct(long productId, Page pageable);

    void copyShopeeSkuAttribute(long productId, long copyProductId);

    List<ShopeeSkuAttributeDTO> selectByProductIds(List<Long> productIds);
}
