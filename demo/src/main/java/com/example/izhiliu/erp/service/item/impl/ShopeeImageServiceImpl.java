package com.izhiliu.erp.service.item.impl;

import com.google.common.collect.Lists;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.service.item.ImageService;
import com.izhiliu.erp.service.item.config.ImageProperties;
import com.izhiliu.erp.service.item.config.SpringTaskRetry;
import com.izhiliu.erp.service.item.dto.ImageBO;
import com.izhiliu.erp.service.item.dto.ImageResult;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.param.UploadImgParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.UploadImgResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeError;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.open.shopee.open.sdk.exception.ShopeeApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author alan
 * @create 2019-09-11 11:47
 **/
@Service
@Slf4j
public class ShopeeImageServiceImpl implements ImageService {

    public static final String FILTER_IMAGE_URL = "shopee";

    private ForkJoinPool forkJoinPool = new ForkJoinPool(20);

    @Resource
    ItemApi itemApi;

    @Resource
    ImageProperties imageProperties;

    @Resource
    SpringTaskRetry retryable;

    /**
     * 去shopee换链
     */
    public ImageResult localImageUrlToShopeeImageUrl(Long shopId, List<String> localImageUrls) {
        ShopeeResult<UploadImgResult> uploadImgResultShopeeResult = itemApi.uploadImg(UploadImgParam.builder().images(localImageUrls).shopId(shopId).build());
        //非超时原因,比如 未授权店铺，isResult 为false,直接返回localImageUrls
        if (uploadImgResultShopeeResult.isResult()) {
            final List<String> collect = uploadImgResultShopeeResult.getData().getImages().stream().map(image -> StringUtils.isBlank(image.getShopeeImageUrl()) ? image.getImageUrl() : image.getShopeeImageUrl()).collect(toList());
            return new ImageResult().setImageUrl(collect);
        } else {
            log.error("  uploadImgResultShopeeResult.getError  {}", uploadImgResultShopeeResult.getError());
            return new ImageResult().setImageUrl(localImageUrls).setError(true).setErrorInfo(uploadImgResultShopeeResult.getError().getError() + ":" + uploadImgResultShopeeResult.getError().getMsg());
        }
    }

    /**
     * 去shopee换链
     */
    public ImageBO localImageUrlToShopeeImageUrl(Long shopId, ImageBO imageBO) {
        if (!isNotShopeeImageUrl(imageBO.getUrl())) {
            return imageBO;
        }
        ShopeeResult<UploadImgResult> uploadImgResultShopeeResult = itemApi.uploadImg(UploadImgParam.builder().images(Lists.newArrayList(imageBO.getUrl())).shopId(shopId).build());
        //非超时原因,比如 未授权店铺，isResult 为false,直接返回localImageUrls
        if (uploadImgResultShopeeResult.isResult()) {
            final List<String> collect = uploadImgResultShopeeResult.getData().getImages().stream().map(image -> StringUtils.isBlank(image.getShopeeImageUrl()) ? image.getImageUrl() : image.getShopeeImageUrl()).collect(toList());
            return imageBO.setUrl(collect.get(0));
        } else {
            log.error("uploadImgResultShopeeResult.getError  {}", uploadImgResultShopeeResult.getError());
            return imageBO.setError(true).setErrorInfo(uploadImgResultShopeeResult.getError().getError() + ":" + uploadImgResultShopeeResult.getError().getMsg());
        }
    }

    public List<UploadImgResult.Image> localImageUrlToShopeeImageUrlV2(Long shopId, List<String> images) {
        UploadImgParam param = UploadImgParam.builder()
                .shopId(shopId)
                .images(images)
                .build();
        ShopeeResult<UploadImgResult> uploadImgResultShopeeResult = itemApi.uploadImg(param);
        if (uploadImgResultShopeeResult.isResult()) {
            return uploadImgResultShopeeResult.getData().getImages();
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> localImageTransformShopeeImage(String taskName, Long shopId, List<String> localImages, Consumer<List<String>> resultImagesConsumer) {
        long begin = System.currentTimeMillis();
        if (CollectionUtils.isEmpty(localImages)) {
            return null;
        }
        //  是否已经是shopee链接直接返回
        final List<String> collect = localImages.stream().filter(this::isNotShopeeImageUrl).collect(toList());
        if (CollectionUtils.isEmpty(collect)) {
            return localImages;
        }

        // 换成 List<ImageBO> 并发批量换链接
        List<ImageBO> imageBOList = new ArrayList<>();
        for (int i = 0; i < localImages.size(); i++) {
            ImageBO imageBO = new ImageBO();
            imageBO.setPosition(i);
            imageBO.setUrl(localImages.get(i));
            imageBOList.add(imageBO);
        }

        List<String> shopeeIamges = null;
        try {
            shopeeIamges = forkJoinPool.submit(() -> imageBOList.parallelStream().map(imageBO -> {
                try {
                    return retryable.retryable(() -> localImageUrlToShopeeImageUrl(shopId, imageBO));
                } catch (Exception e) {
                    return imageBO.setError(true).setErrorInfo("shopee network busy,please retry later.");
                }
            }).sorted().map(ImageBO::getUrl).collect(Collectors.toList())).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("local image to shopee image error ", e);
        }
        if (null != shopeeIamges) {
            //执行换链后的动作，比如存入数据库
            resultImagesConsumer.accept(shopeeIamges);
            //是否未成功换链的图片数量超过阈值,超过直接抛异常
            handlerForErrorImageSize(taskName, shopeeIamges);
        } else {
            //直接传本地图片表示全部换失败
            handlerForErrorImageSize(taskName, localImages);
        }

        log.debug("taskName:{} cost {} ms", taskName, System.currentTimeMillis() - begin);

        return shopeeIamges;
    }

    @Override
    public List<UploadImgResult.Image> localImageTransformShopeeImage(String taskName, Long shopId, List<String> localImages) {
        if (CommonUtils.isBlank(localImages)) {
            return new ArrayList<>();
        }
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < localImages.size(); i++) {
            //  是否已经是shopee链接直接返回
            if (isNotShopeeImageUrl(localImages.get(i))){
                map.put(i,localImages.get(i));
            }
        }
        List<String> collect = map.values().stream().collect(toList());
        if (CommonUtils.isBlank(map)) {
            return localImages.stream().map(e -> {
                UploadImgResult.Image image = new UploadImgResult.Image();
                image.setImageUrl(e);
                image.setShopeeImageUrl(e);
                return image;
            }).collect(toList());
        }
        List<UploadImgResult.Image> images = localImageUrlToShopeeImageUrlV2(shopId, collect);
        localImages.removeAll(collect);
        if (CommonUtils.isNotBlank(localImages)) {
            List<UploadImgResult.Image> existImages = localImages.stream().map(e -> {
                UploadImgResult.Image exist = new UploadImgResult.Image();
                exist.setImageUrl(e);
                return exist;
            }).collect(toList());
            images.forEach(e -> {
                map.entrySet().stream().forEach(m->{
                    if (Objects.equals(e.getImageUrl(),m.getValue()) && CommonUtils.isBlank(e.getError())){
                        existImages.add(m.getKey(),e);
                    }
                });
            });
            images=existImages;
        }
        return images;
    }


    private boolean isNotShopeeImageUrl(String image) {
        if (CommonUtils.isBlank(image)) {
            return true;
        }
        final int i = image.indexOf(FILTER_IMAGE_URL);
        return i == -1;
    }

    /**
     * 是否未成功换链的图片数量超过阈值,超过直接抛异常
     */
    public void handlerForErrorImageSize(String taskName, List<String> resultImages) {
        //todo error msg 国际化
        StringBuilder errorMsg = new StringBuilder(taskName).append(" picture ");
        for (int i = 0; i < resultImages.size(); i++) {
            if (isNotShopeeImageUrl(resultImages.get(i))) {
                errorMsg.append(i + 1).append(",");
            }
        }
        errorMsg.append("upload fail, please check the size < 2M and picture must valid");
        if (resultImages.stream().filter(this::isNotShopeeImageUrl).count() > imageProperties.getImageErrorSizeMax()) {
            throw new ShopeeApiException(1024, errorMsg.toString());
        }
    }

}
