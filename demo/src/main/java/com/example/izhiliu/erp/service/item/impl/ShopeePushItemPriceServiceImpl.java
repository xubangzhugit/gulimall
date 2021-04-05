package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.config.BaseVariable;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.mq.vo.ShopeeActionVO;
import com.izhiliu.erp.config.strategy.ShopeePushContent;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.BaseShopeePushService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.dto.ShopeePushResult;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.erp.service.mq.producer.MQProducerService;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.PushToShopeeTaskQO;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.Item;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.UpdateVariationPriceBatchVariation;
import com.izhiliu.open.shopee.open.sdk.api.item.param.UpdatePriceParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * shopee推送商品价格
 * @Author: louis
 * @Date: 2020/7/14 16:56
 */
@Component
@Slf4j
public class ShopeePushItemPriceServiceImpl implements BaseShopeePushService {

    @Resource
    private ShopeeModelChannel shopeeModelChannel;
    @Resource
    private ShopeeProductService shopeeProductService;
    @Resource
    private ItemApi itemApi;
    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;
    @Resource
    private MQProducerService mqProducerService;

    @PostConstruct
    public void init() {
        ShopeePushContent.pushServiceMap.put(PushToShopeeTaskQO.ITEM_PRICE, this);
    }

    @Override
    public ShopeePushResult doPush(ShopProductParam param) {
        log.info("shopee商品更新价格,productId:{}", param.getProductId());
        final ShopeeProductDTO product = param.getProduct();
        final Long productId = param.getProductId();
        ShopeePushResult result = ShopeePushResult.builder()
                .productId(param.getProductId())
                .pushType(PushToShopeeTaskQO.ITEM_PRICE)
                .build();
        try {
            this.pushToApi(product, new Item());
        } catch (LuxServerErrorException e) {
            result.setErrorMessage(e.getTitle());
        } catch (Exception e) {
            log.error("shopee更新推送价格出错,productId:{}", param.getProductId(), e);
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
        final String loginId = product.getLoginId();
        if (CommonUtils.isNotBlank(product.getDiscountActivityId()) && product.getDiscountActivityId() != 0) {
            //发送需要折扣信息  mq
            ShopeeActionVO action = ShopeeActionVO.builder()
                    .action(ShopeeActionVO.Action.DISCOUNT.getCode())
                    .shopId(shopId)
                    .productId(productId)
                    .loginId(loginId)
                    .build();
            mqProducerService.sendMQ(BaseVariable.ShopeeActionTagVariable.SHOPEE_ACTION_DISCOUNT, shopId + ":" + productId, action);
            return true;
        }
        if (ShopeeProduct.VariationTier.ZERO.val.equals(product.getVariationTier())) {
            //单品
            final List<ShopeeProductSkuDTO> productSkus = shopeeProductSkuService.pageByProduct(product.getId(), new Page(0, Integer.MAX_VALUE)).getRecords();
            if (CommonUtils.isNotBlank(productSkus)) {
                productSkus.stream().map(ShopeeProductSkuDTO::getPrice).findFirst()
                        .ifPresent(item::setPrice);
            }
            UpdatePriceParam param = UpdatePriceParam.builder()
                    .itemId(shopeeItemId)
                    .price(item.getPrice())
                    .shopId(shopId)
                    .build();
            CommonUtils.handleShopeeApiResult(itemApi.updatePrice(param));
        } else {
            //单sku and 多sku
            List<ShopeeProductSkuDTO> skuListByProductId = shopeeProductSkuService.getSkuListByProductId(productId);
            if (CommonUtils.isBlank(skuListByProductId)) {
                return true;
            }
            List<UpdateVariationPriceBatchVariation> collect = skuListByProductId.stream().map(e -> {
                UpdateVariationPriceBatchVariation variation = new UpdateVariationPriceBatchVariation();
                variation.setItemId(shopeeItemId);
                variation.setVariationId(e.getShopeeVariationId());
                variation.setPrice(e.getPrice());
                return variation;
            }).collect(Collectors.toList());
            CommonUtils.handleShopeeApiResult(itemApi.updateVariationPriceBatch(shopId, collect));
        }
        return true;
    }

}
