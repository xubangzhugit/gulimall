package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.strategy.ShopeePushContent;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.BaseShopeePushService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.dto.ShopeePushResult;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.PushToShopeeTaskQO;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.Item;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.UpdateVariationPriceBatchVariation;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.UpdateVariationStockBatchVariation;
import com.izhiliu.open.shopee.open.sdk.api.item.param.UpdatePriceParam;
import com.izhiliu.open.shopee.open.sdk.api.item.param.UpdateStockParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * shopee推送商品库存
 * @Author: louis
 * @Date: 2020/7/14 16:56
 */
@Component
@Slf4j
public class ShopeePushItemStockServiceImpl implements BaseShopeePushService {

    @Resource
    private ShopeeModelChannel shopeeModelChannel;
    @Resource
    private ShopeeProductService shopeeProductService;
    @Resource
    private ItemApi itemApi;
    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;

    @PostConstruct
    public void init() {
        ShopeePushContent.pushServiceMap.put(PushToShopeeTaskQO.ITEM_STOCK, this);
    }

    @Override
    public ShopeePushResult doPush(ShopProductParam param) {
        log.info("shopee商品更新库存");
        final ShopeeProductDTO product = param.getProduct();
        final Long productId = param.getProductId();
        ShopeePushResult result = ShopeePushResult.builder()
                .productId(param.getProductId())
                .pushType(PushToShopeeTaskQO.ITEM_STOCK)
                .build();
        try {
            this.pushToApi(product, new Item());
        } catch (LuxServerErrorException e) {
            result.setErrorMessage(e.getTitle());
        } catch (Exception e) {
            log.error("shopee更新推送库存出错,productId:{}", param.getProductId(), e);
            result.setErrorMessage(CommonUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "系统繁忙");
        }
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Boolean pushToApi(ShopeeProductDTO product, Item item) {
        final Long shopId = product.getShopId();
        final Long productId = product.getId();
        final Long shopeeItemId = product.getShopeeItemId();
        if (ShopeeProduct.VariationTier.ZERO.val.equals(product.getVariationTier())) {
            //单品
            final List<ShopeeProductSkuDTO> productSkus = shopeeProductSkuService.pageByProduct(product.getId(), new Page(0, Integer.MAX_VALUE)).getRecords();
            if (CommonUtils.isNotBlank(productSkus)) {
                productSkus.stream().map(ShopeeProductSkuDTO::getStock).findFirst()
                        .ifPresent(item::setStock);
            }
            UpdateStockParam param = UpdateStockParam.builder()
                    .itemId(shopeeItemId)
                    .stock(item.getStock())
                    .shopId(shopId)
                    .build();
            CommonUtils.handleShopeeApiResult(itemApi.updateStock(param));
        } else {
            //单sku and 多sku
            List<ShopeeProductSkuDTO> skuListByProductId = shopeeProductSkuService.getSkuListByProductId(productId);
            if (CommonUtils.isBlank(skuListByProductId)) {
                return true;
            }
            List<UpdateVariationStockBatchVariation> collect = skuListByProductId.stream().map(e -> {
                UpdateVariationStockBatchVariation variation = new UpdateVariationStockBatchVariation();
                variation.setItemId(shopeeItemId);
                variation.setVariationId(e.getShopeeVariationId());
                variation.setStock(e.getStock());
                return variation;
            }).collect(Collectors.toList());
            CommonUtils.handleShopeeApiResult(itemApi.updateVariationStockBatch(shopId, collect));
        }
        return true;
    }
}
