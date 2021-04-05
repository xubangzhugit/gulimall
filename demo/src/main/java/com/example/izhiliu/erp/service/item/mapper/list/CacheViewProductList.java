package com.izhiliu.erp.service.item.mapper.list;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/31 15:54
 */
public interface CacheViewProductList {

    /**
     * 依赖的关联查询
     */
    String LIST_BY_META_DATA = "metaData-list-by-mete-data";
    String TRACK_TO_THE_SHOP = "platform-trackToTheShop";

    String PRODUCT_LIST = "product-list";
    String SHOP_PRODUCT_VARIATIONS = "shop-product-variation";
    String PRODUCT_VARIATIONS = "product-variations";
}
