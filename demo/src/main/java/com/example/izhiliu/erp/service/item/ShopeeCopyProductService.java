package com.izhiliu.erp.service.item;

import com.izhiliu.erp.web.rest.item.param.CopyToShop;
import com.izhiliu.erp.web.rest.item.param.SaveToNode;
import com.izhiliu.erp.web.rest.item.param.SaveToShop;
import com.izhiliu.erp.web.rest.item.result.CopyShopProductResult;

import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 11:34
 */
public interface ShopeeCopyProductService {

    /**
     * 拷贝到站点
     *
     * @param saveToNode
     * @return
     */
    List<Long> copyPlatformToNode(SaveToNode saveToNode);

    /**
     * 拷贝到店铺
     *
     * @param saveToShops
     * @return
     */
    void copyNodeToShop(List<SaveToShop> saveToShops);

    /**
     * 发布到店铺
     *
     * @param saveToShops
     */
    void publishNodeToShop(List<SaveToShop> saveToShops);

    /**
     * 拷贝店铺商品
     *
     * @param shopProductId
     * @param shopId
     * @return
     */
    Long copyToShop(long shopProductId, long shopId, long platformNodeId);

    /**
     * 拷贝店铺商品
     *
     * @param copyToShops
     * @return
     */
    List<CopyShopProductResult> copyToShop(List<CopyToShop> copyToShops);
}
