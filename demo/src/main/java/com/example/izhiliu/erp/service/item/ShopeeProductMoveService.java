package com.izhiliu.erp.service.item;

import com.izhiliu.erp.web.rest.item.param.ShopeeProductMoveParam;
import com.izhiliu.erp.web.rest.item.result.ShopeeProductMoveResult;

/**
 *  蝦皮 店鋪商品搬家服務
 */
public interface ShopeeProductMoveService {

    int NOT_RUNNING = -1;
    int RUNNING = 0;
    int RUNTIME_END= 1;
    int RUNTIME_ERROR = 2;
    int CURRENT_INDEX_DEFAULT = 0;

    /**
     *   將商品加入到搬家隊伍中。。。
     * @return
     */
    Boolean porductPutToProductMoveTask(ShopeeProductMoveParam shopeeProductMoveParam);


    ShopeeProductMoveResult selectShopeeProductMoveTask();


    /**
     *   移除或者终止 商品搬家的任务
     * @return
     */
    Boolean removeProductMoveTask(ShopeeProductMoveParam shopeeProductMoveParam);

    public Boolean syncShopeeProductMoveTask(String key, String productId);
    public Boolean syncShopeeProductMoveTask(String key);

    Boolean deleteProductMoveTask(ShopeeProductMoveParam shopeeProductMoveParam);
}
