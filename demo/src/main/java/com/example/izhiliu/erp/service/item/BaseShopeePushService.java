package com.izhiliu.erp.service.item;

import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeePushResult;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.Item;

/**
 *  shopee更新推送基类
 * @Author: louis
 * @Date: 2020/7/14 16:51
 */
public interface BaseShopeePushService {

    /**
     * 处理推送
     * @param param
     * @return
     */
    default ShopeePushResult doPush(ShopProductParam param) {
        return new ShopeePushResult();
    }

    /**
     * 组装参数
     * @param product
     * @return
     */
    default Item buildItem(ShopeeProductDTO product) {
        return new Item();
    }

    /**
     * 调用第三方接口推送
     * @param shopId
     * @param item
     * @return
     */
    default Boolean pushToApi(ShopeeProductDTO product, Item item) {
        return true;
    }


}

