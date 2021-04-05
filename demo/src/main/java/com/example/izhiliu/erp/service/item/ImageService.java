package com.izhiliu.erp.service.item;

import com.izhiliu.open.shopee.open.sdk.api.item.result.UploadImgResult;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author alan
 * @create 2019-09-11 11:47
 **/


public interface ImageService {

    /**
     *   本地图像变换 Shopee 图像
     * @param localImages  将要 执行的 跟换的 集合
     * @param shopId   店铺id
     * @return
     */
    List<String> localImageTransformShopeeImage(String taskName, Long shopId, List<String> localImages, Consumer<List<String>> resultImagesConsumer);

    List<UploadImgResult.Image> localImageTransformShopeeImage(String taskName, Long shopId, List<String> localImages);
}
