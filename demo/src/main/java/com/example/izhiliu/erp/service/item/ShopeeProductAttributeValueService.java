package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.ShopeeProductAttributeValue;
import com.izhiliu.erp.service.item.dto.ShopeeProductAttributeValueDTO;
import com.izhiliu.erp.web.rest.item.param.ProductAttributeValueParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetItemDetailResult;

import java.util.List;

/**
 * Service Interface for managing ShopeeProductAttributeValue.
 */
public interface ShopeeProductAttributeValueService extends IBaseService<ShopeeProductAttributeValue, ShopeeProductAttributeValueDTO> {

    String CACHE_ONE = "shopee-product-attribute-value-one";
    String CACHE_LIST = "shopee-product-attribute-value-list";
    String CACHE_PAGE = "shopee-product-attribute-value-page";
    String CACHE_PAGE$ = "shopee-product-attribute-value-page$";

    void coverByProduct(ProductAttributeValueParam param);

    List<ShopeeProductAttributeValueDTO> selectByProduct(long productId);

    void deleteByAttribute(long attributeId);

    void deleteByAttributeAndValue(long attributeId, String value);

    int deleteByProduct(long productId);

    void copyShopeeProductAttributeValue(long productId, long copyProductId);

    void productResetShopeeCategory(long productId, long copyProductId, long categoryId, long platformNodeId);

    void checkRequired(long productId, long categoryId, long nodeId);

    void saveAttribute(GetItemDetailResult.ItemBean item, long shopeeCategoryId, Long categoryId, Long productId);
}
