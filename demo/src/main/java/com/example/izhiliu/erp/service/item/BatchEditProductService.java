package com.izhiliu.erp.service.item;

import com.izhiliu.erp.service.item.dto.BatchEditProductDTO;

import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/8 21:00
 */
public interface BatchEditProductService {

    /**
     * 获取商品详情列表
     *
     * @param productIds
     * @return
     */
    List<BatchEditProductDTO> getProducts(List<Long> productIds);

    List<BatchEditProductDTO> getProductsV2(List<Long> productIds);

    /**
     * 保存类目和属性
     *
     * @param products
     */
    void categoryAndAttribute(List<BatchEditProductDTO> products);

    /**
     * 保存基本信息
     *
     * @param products
     */
    void basicInfo(List<BatchEditProductDTO> products);

    /**
     * 保存价格库存
     *
     * @param products
     */
    void priceAndStock(List<BatchEditProductDTO> products);

    void priceAndStockForBatch(List<BatchEditProductDTO> products);

    /**
     * 保存物流信息
     *
     * @param products
     */
    void logisticInfo(List<BatchEditProductDTO> products);
}
