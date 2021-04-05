package com.izhiliu.erp.service.item.impl;

import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.item.ItemDtsConfig;
import com.izhiliu.erp.config.strategy.ShopeePushContent;
import com.izhiliu.erp.service.item.BaseShopeePushService;
import com.izhiliu.erp.service.item.ShopeeBasicDataService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeePushResult;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.PushToShopeeTaskQO;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.Item;
import com.izhiliu.open.shopee.open.sdk.api.item.result.UpdateItemResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.open.shopee.open.sdk.entity.logistics.LogisticsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * shopee推送商品物流信息
 * @Author: louis
 * @Date: 2020/7/14 16:56
 */
@Component
@Slf4j
public class ShopeePushItemLogisticsServiceImpl implements BaseShopeePushService {

    public static final Integer DTS_DAY = 4;

    @Resource
    private ShopeeModelChannel shopeeModelChannel;
    @Resource
    private ShopeeProductService shopeeProductService;
    @Resource
    private ItemApi itemApi;
    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;
    @Resource
    private ShopeeBasicDataService shopeeBasicDataService;
    @Resource
    private ItemDtsConfig itemDtsConfig;

    @PostConstruct
    public void init() {
        ShopeePushContent.pushServiceMap.put(PushToShopeeTaskQO.ITEM_LOGISTICS, this);
    }

    @Override
    public ShopeePushResult doPush(ShopProductParam param) {
        log.info("shopee商品更新物流信息");
        final ShopeeProductDTO product = param.getProduct();
        final Long productId = param.getProductId();
        ShopeePushResult result = ShopeePushResult.builder()
                .productId(param.getProductId())
                .pushType(PushToShopeeTaskQO.ITEM_LOGISTICS)
                .build();
        try {
            Item item = this.buildItem(product);
            this.pushToApi(product, item);
        } catch (LuxServerErrorException e) {
            result.setErrorMessage(e.getTitle());
        } catch (Exception e) {
            log.error("shopee商品更新物流信息出错,productId:{}", param.getProductId(), e);
            result.setErrorMessage(CommonUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "系统繁忙");
        }
        return result;
    }

    @Override
    public Item buildItem(ShopeeProductDTO product) {
        Item item = new Item();
        item.setItemId(product.getShopeeItemId());
        //检查是否开启acm配置文件获取dts
        Integer sendOutTime = product.getSendOutTime();
        String dtsByConfig = itemDtsConfig.getDtsByConfig();
        if (CommonUtils.isNotBlank(dtsByConfig) && sendOutTime < DTS_DAY){
            sendOutTime = Integer.valueOf(dtsByConfig);
        }
        item.setDaysToShip(sendOutTime);
        final Long shopId = product.getShopId();
        List<LogisticsResult.LogisticsBean> allLogistics = shopeeBasicDataService.getLogistics(Arrays.asList(shopId));
        List<ShopeeProductDTO.Logistic> logistics = product.getLogistics();
        if (CommonUtils.isBlank(allLogistics)) {
            throw new LuxServerErrorException("当前店铺没有物流方式");
        }
        List<Item.Logistic> itemLogistics = allLogistics.stream().map(logisticsBean -> {
            Item.Logistic itemLogistic = new Item.Logistic();
            itemLogistic.setLogisticId(Long.valueOf(logisticsBean.getLogisticId()));
            itemLogistic.setEnabled(false);
            logistics.stream().filter(logistic -> logistic.getLogisticId().equals(itemLogistic.getLogisticId()))
                    .findFirst()
                    .ifPresent(logistic -> {
                        itemLogistic.setEnabled(logistic.getEnabled());
                        itemLogistic.setIsFree(Objects.isNull(logistic.getIsFree()) ? Boolean.FALSE : logistic.getIsFree());
                        itemLogistic.setSizeId(logistic.getSizeId());
                        itemLogistic.setShippingFee(logistic.getShippingFee());
                    });
            return itemLogistic;
        }).collect(Collectors.toList());
        item.setLogistics(itemLogistics);
        if(product.getSendOutTime() > DTS_DAY){
            item.setIsPreOrder(true);
        }
        return item;
    }

    @Override
    public Boolean pushToApi(ShopeeProductDTO product, Item item) {
        final Long shopId = product.getShopId();
        ShopeeResult<UpdateItemResult> result = itemApi.updateItem(shopId, item);
        //判断是否是因为dts原因
        if (!result.isResult() && CommonUtils.isNotBlank(result.getError().getMsg()) && result.getError().getMsg().contains(ShopeePushItemInfoServiceImpl.NO_PRE_ORDER)){
            String msg = result.getError().getMsg();
            Integer dts = Integer.valueOf(msg.substring(msg.length() - 1));
            item.setDaysToShip(dts);
            result = itemApi.updateItem(shopId, item);
        }
        CommonUtils.handleShopeeApiResult(result);
        return true;
    }
}


