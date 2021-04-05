package com.izhiliu.erp.service.item.impl;

import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.strategy.ShopeePushContent;
import com.izhiliu.erp.service.item.BaseShopeePushService;
import com.izhiliu.erp.service.item.ImageService;
import com.izhiliu.erp.service.item.ShopeeProductMediaService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductMediaDTO;
import com.izhiliu.erp.service.item.dto.ShopeePushResult;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.PushToShopeeTaskQO;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.Item;
import com.izhiliu.open.shopee.open.sdk.api.item.param.UpdateItemImgParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.UpdateImgResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * shopee推送商品图片
 * @Author: louis
 * @Date: 2020/7/14 16:56
 */
@Component
@Slf4j
public class ShopeePushItemImagesServiceImpl implements BaseShopeePushService {

    @Resource
    protected ImageService imageService;
    @Resource
    private ShopeeProductMediaService shopeeProductMediaService;
    @Resource
    private ItemApi itemApi;

    @PostConstruct
    public void init() {
        ShopeePushContent.pushServiceMap.put(PushToShopeeTaskQO.ITEM_IMAGE, this);
    }


    @Override
    public ShopeePushResult doPush(ShopProductParam param) {
        log.info("shopee推送商品图片");
        final ShopeeProductDTO product = param.getProduct();
        final Long productId = param.getProductId();
        ShopeePushResult result = ShopeePushResult.builder()
                .productId(param.getProductId())
                .pushType(PushToShopeeTaskQO.ITEM_IMAGE)
                .build();
        try {
            Item item = this.buildItem(product);
            this.pushToApi(product, item);
        } catch (LuxServerErrorException e) {
            result.setErrorMessage(e.getTitle());
        } catch (Exception e) {
            log.error("shopee更新推送图片出错,productId:{}", param.getProductId(), e);
            result.setErrorMessage(CommonUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "系统繁忙");
        }
        return result;
    }

    @Override
    public Item buildItem(ShopeeProductDTO product) {
        final Long shopId = product.getShopId();
        Item item = new Item();
        item.setItemId(product.getShopeeItemId());
        List<Item.Image> images = imageService.localImageTransformShopeeImage("product", shopId, product.getImages(),
                (strings -> {
                    final ShopeeProductMediaDTO entity = new ShopeeProductMediaDTO()
                            .setProductId(product.getId())
                            .setImages(strings)
                            .setDiscountActivityId(product.getDiscountActivityId())
                            .setDiscountActivity(product.getDiscountActivity());
                    product.setImages(strings);
                })).stream().map(Item.Image::new).collect(toList());
        item.setImages(images);
        return item;
    }


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Boolean pushToApi(ShopeeProductDTO product, Item item) {
        final Long shopId = product.getShopId();
        List<String> images = item.getImages().stream().map(Item.Image::getUrl).collect(toList());
        UpdateItemImgParam param = UpdateItemImgParam.builder()
                .shopId(shopId)
                .itemId(product.getShopeeItemId())
                .images(images)
                .build();
        //注意：如果相同图片更新，shopee后台会提示【No image updated. 】
        ShopeeResult<UpdateImgResult> result = itemApi.updateItemImg(param);
        if (CommonUtils.isNotBlank(result.getError()) && !result.getError().getMsg().contains("No image updated.")) {
            throw new LuxServerErrorException(result.getError().getMsg());
        }
        log.info("更新图片成功,product:{},result:{}", product.getShopeeItemId(), result);
        return true;
    }
}
