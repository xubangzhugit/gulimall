package com.izhiliu.erp.service.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import com.izhiliu.erp.service.item.dto.ShopeeProductCopyDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.web.rest.item.vm.VariationMV_V2;
import com.izhiliu.erp.web.rest.item.vm.VariationVM;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing ShopeeProductSku.
 */
public interface ShopeeProductSkuService extends IBaseService<ShopeeProductSku, ShopeeProductSkuDTO> {

    String CACHE_ONE = "shopee-product-sku-one";
    String CACHE_LIST = "shopee-product-sku-list";
    String CACHE_PAGE = "shopee-product-sku-page";
    String CACHE_PAGE$ = "shopee-product-sku-page$";

    int deleteByProduct(long productId);

    VariationVM variationByProduct(long productId);

    List<VariationMV_V2> variationsByProduct(long productId);

    IPage<ShopeeProductSkuDTO> pageByProduct(long productId, Page pageable);

    void checkPrice(String toCurrency, CurrencyRateResult result, ShopeeProductSkuDTO productSku);

    /**
     * 覆盖对应的产品
     * @param param
     */
    void coverTheProduct(VariationVM param);

    /**
     *  创建元数据
     * @param param
     */
    void coverByProductSource(VariationVM param,ShopeeProductDTO shopeeProduct);

    ShopeeProductDTO checkParam(VariationVM param, Optional<ShopeeProductDTO> productSupplier);

    void copyShopeeProductSku(long productId, long copyProductId, String toCurrency, boolean toTw);

    void copyShopeeProductSku(ShopeeProductCopyDTO shopeeProductCopyDTO);

    Optional<ShopeeProductSkuDTO> findByVariationId(Long variationId);

    List<ShopeeProductSkuDTO> listByVariationIds(List<Long> variationIds);

    List<ShopeeProductSku> findSkuListByloginId(String loginId,String skuCode);

    List<VariationMV_V2> selectVariationMV_V2ByProductIds(List<Long> productIds);

    List<ShopeeProductSkuDTO> listByProductIds(List<Long> productIds);

    List<ShopeeProductSku> selectImagesBySkuCodes(Collection<String> skuCode, Long productId);

     boolean superBatchSave(Collection<ShopeeProductSkuDTO> entityList, int batchSize);

    void checkPublishParam(Long productId);

    List<ShopeeProductSkuDTO> selectByProductIdAndShopeeVariationIds(Long productId, List<Long> variationIds);

    IPage<ShopeeProductSkuDTO> findSkuListByloginIdAndSkuCode(Page page, String skuCode);

    void clearInvalidVariationId(List<Long> invalidVariationId);

    /**
     * 通过商品id获取对应sku
     * @param productId
     * @return
     */
    List<ShopeeProductSkuDTO> getSkuListByProductId(Long productId);

    boolean replace(ShopeeProductSkuDTO productSkuDTO);

    boolean replaceAll(List<ShopeeProductSkuDTO> list);

    boolean deleteByVariationId(long productId, List<Long> variationIds);

    boolean deleteAllNullVariationIdByProduct(long productId);
}
