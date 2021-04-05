package com.izhiliu.erp.service.item.module.basic;

import com.izhiliu.erp.service.item.*;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.module.convert.ShopeeMetaConvertShopee;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * describe: 源数据
 * <p>
 *
 * @author cheng
 * @date 2019/1/11 21:22
 */
public abstract class BaseModelConvert {
    protected static final Logger log = LoggerFactory.getLogger(ShopeeMetaConvertShopee.class);

    @Resource
    protected ShopeeCategoryService shopeeCategoryService;

    @Resource
    protected ShopeeProductAttributeValueService shopeeProductAttributeValueService;

    @Resource
    protected ShopeeProductService shopeeProductService;

    @Resource
    protected ShopeeSkuAttributeService shopeeSkuAttributeService;

    @Resource
    protected ShopeeProductSkuService shopeeProductSkuService;

    /**
     * 源数据转平台商品
     */
    public abstract void map(ProductMetaDataDTO t, String loginId, MetaDataObject.CollectController collectController);

    protected void updateVariationTier(int variationTier, long productId) {
        final ShopeeProductDTO updateTier = new ShopeeProductDTO();
        updateTier.setId(productId);
        updateTier.setVariationTier(variationTier);
        shopeeProductService.update(updateTier);
    }

    protected void updateVariationTier(int variationTier, long productId,int sold) {
        final ShopeeProductDTO updateTier = new ShopeeProductDTO();
        updateTier.setId(productId);
        updateTier.setVariationTier(variationTier);
        updateTier.setSold(sold);
        shopeeProductService.update(updateTier);
    }
}
