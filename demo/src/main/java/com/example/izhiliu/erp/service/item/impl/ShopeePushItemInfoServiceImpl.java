package com.izhiliu.erp.service.item.impl;

import cn.hutool.core.util.StrUtil;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.item.ItemDtsConfig;
import com.izhiliu.erp.config.strategy.ShopeePushContent;
import com.izhiliu.erp.service.item.*;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductMediaDTO;
import com.izhiliu.erp.service.item.dto.ShopeePushResult;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.PushToShopeeTaskQO;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.Item;
import com.izhiliu.open.shopee.open.sdk.api.item.result.UpdateItemResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * shopee推送商品基本信息
 * @Author: louis
 * @Date: 2020/7/14 16:56
 */
@Component
@Slf4j
public class ShopeePushItemInfoServiceImpl implements BaseShopeePushService {

    public static final String INVALID_CATEGORY_ID = "invalid category id";

    public static final String INVALID_VARIATION_ID = "invalid variation id";

    public static final String INVALID_ATTRIBUTE = "Contains invalid attribute";

    public static final String NO_PRE_ORDER = "no pre order";

    @Resource
    private ItemApi itemApi;
    @Resource
    private ShopeeCategoryService shopeeCategoryService;
    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;
    @Resource
    protected ImageService imageService;
    @Resource
    private ShopeeProductMediaService shopeeProductMediaService;
    @Resource
    private ItemDtsConfig itemDtsConfig;


    @PostConstruct
    public void init() {
        ShopeePushContent.pushServiceMap.put(PushToShopeeTaskQO.ITEM_INFO, this);
    }

    @Override
    public ShopeePushResult doPush(ShopProductParam param) {
        log.info("shopee推送商品信息");
        final Long productId = param.getProductId();
        final ShopeeProductDTO product = param.getProduct();
        ShopeePushResult result = ShopeePushResult.builder()
                .productId(param.getProductId())
                .pushType(PushToShopeeTaskQO.ITEM_INFO)
                .build();
        try {
            Item item = this.buildItem(product);
            this.pushToApi(product, item);
        } catch (LuxServerErrorException e) {
            result.setErrorMessage(e.getTitle());
        } catch (Exception e) {
            log.error("shopee更新推送基础出错,productId:{}", param.getProductId(), e);
            result.setErrorMessage(CommonUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "系统繁忙");
        }
        return result;
    }

    @Override
    public Item buildItem(ShopeeProductDTO product) {
        Item item = new Item();
        final Optional<ShopeeCategoryDTO> dto = shopeeCategoryService.find(product.getCategoryId());
        if (!dto.isPresent()) {
            throw new LuxServerErrorException("请填写类目及属性信息后再操作");
        }
        if (product.getShopeeCategoryId() == null || product.getShopeeCategoryId().equals(0L)) {
            item.setCategoryId(dto.get().getShopeeCategoryId());
        } else {
            item.setCategoryId(product.getShopeeCategoryId());
        }

        if (StrUtil.isNotBlank(product.getSkuCode())) {
            item.setItemSku(product.getSkuCode());
        }

        if (product.getShopeeItemId() != null && product.getShopeeItemId() != 0) {
            item.setItemId(product.getShopeeItemId());
        }
        item.setName(product.getName());
        item.setDescription(product.getDescription());
        item.setStock(product.getStock());
        if(null != product.getLength()){
            item.setPackageLength(product.getLength()/10);
        }
        if(null != product.getWidth()){
            item.setPackageWidth( product.getWidth()/10);
        }
        if(null != product.getHeight()){
            item.setPackageHeight(product.getHeight()/10);
        }
        if(product.getSendOutTime() > ShopeePushItemLogisticsServiceImpl.DTS_DAY){
            item.setIsPreOrder(true);
        }
        //检查是否开启acm配置文件获取dts
        Integer sendOutTime = product.getSendOutTime();
        String dtsByConfig = itemDtsConfig.getDtsByConfig();
        if (CommonUtils.isNotBlank(dtsByConfig) && sendOutTime < ShopeePushItemLogisticsServiceImpl.DTS_DAY){
            sendOutTime = Integer.valueOf(dtsByConfig);
        }
        item.setDaysToShip(sendOutTime);
        item.setWeight(ShopeeUtil.outputWeight(product.getWeight()));

        //  是否支持尺寸图 0:不支持 1: 支持
        if (Objects.equals(dto.get().getSuppSizeChart(), 1)) {
            if (null != product.getSizeChart()) {
                fillSizeChart(product, item, product.getShopId());
            }
        }
        //  批发价格支持
        if (CommonUtils.isNotBlank(product.getPriceRange())) {
            final List<Item.Wholesale> collect = product.getPriceRange().stream().map(priceRange -> {
                final Item.Wholesale wholesale = new Item.Wholesale();
                wholesale.setMin(priceRange.getMin());
                wholesale.setMax(priceRange.getMax());
                wholesale.setUnitPrice(priceRange.getPrice());
                return wholesale;
            }).collect(toList());
            item.setWholesales(collect);
        } else {
            item.setWholesales(new ArrayList<>());
        }
        return item;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Boolean pushToApi(ShopeeProductDTO product, Item item) {
        final Long shopId = product.getShopId();
        ShopeeResult<UpdateItemResult> result = itemApi.updateItem(shopId, item);
        //判断是否是因为dts原因
        if (!result.isResult() && CommonUtils.isNotBlank(result.getError().getMsg()) && result.getError().getMsg().contains(NO_PRE_ORDER)){
            String msg = result.getError().getMsg();
            Integer dts = Integer.valueOf(msg.substring(msg.length() - 1));
            item.setDaysToShip(dts);
            result = itemApi.updateItem(shopId, item);
        }
        if (!result.isResult()) {
            if (result.getError().getMsg() == null) {
                throw new LuxServerErrorException(result.getError().getError());
            } else if (result.getError().getMsg().contains(INVALID_CATEGORY_ID)) {
                log.error("[删除无效类目]: {}", product.getCategoryId());
                shopeeCategoryService.delete(product.getCategoryId());
            } else if (result.getError().getMsg().contains(INVALID_ATTRIBUTE)) {
                log.error("[清除过时类目属性值]: {}", item.getCategoryId());
                //todo ,清除过时的类目属性
            } else if (result.getError().getMsg().contains(INVALID_VARIATION_ID)) {
                log.error("[清除无效变体ID]");
                final List<Long> invalidVariationId = item.getVariations().stream().filter(Objects::nonNull).map(Item.Variation::getVariationId).filter(variation -> !Objects.equals(variation,0L)).collect(Collectors.toList());
                shopeeProductSkuService.clearInvalidVariationId(invalidVariationId);
            }
            throw new LuxServerErrorException(result.getError().getMsg());
        }
        return true;
    }


    private void fillSizeChart(ShopeeProductDTO product, Item item, Long shopId) {
        final String sizeChart = product.getSizeChart();
        if (StringUtils.isNotBlank(sizeChart)) {
            imageService.localImageTransformShopeeImage("size", shopId, Collections.singletonList(sizeChart), (strings -> {
                strings.stream().findFirst().ifPresent(imageUrl -> {
                    final ShopeeProductMediaDTO entity = new ShopeeProductMediaDTO().setProductId(product.getId()).setSizeChart(imageUrl);
                    product.setSizeChart(imageUrl);
                });
            })).stream().findFirst().ifPresent(item::setSizeChart);
        }
    }

}
