package com.izhiliu.erp.service.item.impl;

import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.config.internation.HandleMessageSource;
import com.izhiliu.erp.config.strategy.ShopeePushContent;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.service.item.ImageService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductFuture;
import com.izhiliu.erp.service.item.dto.ShopeeProductMediaDTO;
import com.izhiliu.erp.service.item.dto.ShopeePushResult;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import com.izhiliu.erp.web.rest.item.vm.TaskExecuteVO;
import com.izhiliu.open.shopee.open.sdk.api.item.entity.Item;
import com.izhiliu.open.shopee.open.sdk.api.item.result.UploadImgResult;
import com.izhiliu.open.shopee.open.sdk.exception.ShopeeApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @Author: louis
 * @Date: 2020/7/16 15:21
 */
@Service
@Slf4j
public class ShopeeProductUpdateAsync {

    @Resource
    private ShopeePushContent shopeePushContent;
    @Resource
    private HandleMessageSource handleMessageSource;
    @Resource
    private TaskExecutorUtils taskExecutorUtils;
    @Resource
    @Lazy
    private ShopeeProductService shopeeProductService;
    @Resource
    private ImageService imageService;

    ForkJoinPool imagePool = new ForkJoinPool(20);


    /**
     * 商品推送更新
     * @param param
     * @param pushTypes
     */
    public void handleUpdateAsyc(ShopProductParam param, List<String> pushTypes) {
        final String taskId = param.getTaskId();
        final ShopeeProductDTO product = param.getProduct();
        final String productId = param.getProductId().toString();
        final Locale locale = param.getLocale();
        if (CommonUtils.isBlank(pushTypes)) {
            return;
        }
        List<Future<ShopeePushResult>> collect = pushTypes.stream().map(pushType -> {
            final String detailId = taskId.concat(pushType).concat(productId);
            TaskExecuteVO.TaskDetail taskDetail = TaskExecuteVO.TaskDetail.builder()
                    .taskId(taskId)
                    .detailId(detailId)
                    .code(TaskExecuteVO.CODE_PENDING)
                    .detailName(getPushItemText(productId, pushType, locale))
                    .build();
            taskExecutorUtils.handleTaskSet(taskDetail);
            return updateShopeeProductToApi(param, pushType);
        }).collect(Collectors.toList());
        while (true) {
            if (collect.stream().allMatch(Future::isDone)) {
                break;
            }
        }
        String errorMsg = collect.stream().map(f -> {
            try {
                ShopeePushResult result = f.get();
                final String errorMessage = result.getErrorMessage();
                final String pushType = result.getPushType();
                final String detailId = taskId.concat(pushType).concat(productId);
                TaskExecuteVO.TaskDetail taskDetail = TaskExecuteVO.TaskDetail.builder()
                        .taskId(taskId)
                        .detailId(detailId)
                        .errorMessage(errorMessage)
                        .code(CommonUtils.isNotBlank(errorMessage) ? TaskExecuteVO.CODE_FAILD : TaskExecuteVO.CODE_SUCCESS)
                        .detailName(getPushItemText(productId, pushType, locale))
                        .build();
                taskExecutorUtils.handleTaskSet(taskDetail);
                return errorMessage;
            } catch (Exception e) {
                //处理异常
            }
            return null;
        }).filter(CommonUtils::isNotBlank).collect(Collectors.joining(";"));
        //存在更新失败的情况
        if (CommonUtils.isNotBlank(errorMsg)) {
            shopeeProductService.pushFailHandle(productId, errorMsg, LocalProductStatus.PUSH_FAILURE);
        } else {
            shopeeProductService.pushSuccessHandle(product, LocalProductStatus.PUSH_SUCCESS);
        }
    }

    @Async
    public Future<ShopeePushResult> updateShopeeProductToApi(ShopProductParam param, String pushType) {
        return new AsyncResult<>(shopeePushContent.getContent(pushType).doPush(param));
    }

    private String getPushItemText(String productId, String pushType, Locale locale) {
        String var = "shopee.".concat(pushType);
        return handleMessageSource.getMessage(var, new String[]{productId}, locale);
    }


    public void handleShopeeIamgeAsyc(List<ShopProductParam> params) {
        ShopProductParam param = params.get(0);
        final String taskId = param.getTaskId();
        final Locale locale = param.getLocale();
        //写入缓存
        params.forEach(e -> {
            String productId = String.valueOf(e.getProductId());
            TaskExecuteVO.TaskDetail taskDetail = TaskExecuteVO.TaskDetail.builder()
                    .taskId(taskId)
                    .detailId(String.valueOf(e.getProductId()))
                    .code(TaskExecuteVO.CODE_PENDING)
                    .detailName(getImageToshopeeTaskDetail(productId, e.getLocale()))
                    .build();
            taskExecutorUtils.handleTaskSet(taskDetail);
        });
        ForkJoinTask<List<ShopeeProductFuture>> submit = imagePool.submit(() -> params.parallelStream().map(e -> {
            ShopeeProductDTO product = e.getProduct();
            product.setShopId(e.getShopId());
            return handleLocalImageToShopee(param, product);
        }).collect(toList()));

        while (!submit.isDone()){}
    }

    private ShopeeProductFuture handleLocalImageToShopee(ShopProductParam param, ShopeeProductDTO productDTO) {
        final Long productId = productDTO.getId();
        final Long shopId = productDTO.getShopId();
        final String taskId = param.getTaskId();
        final Locale locale = param.getLocale();
        List<String> imageList = productDTO.getImages();
        ShopeeProductFuture result = ShopeeProductFuture.builder()
                .productId(productDTO.getId())
                .total(imageList.size())
                .build();
        try {
            List<UploadImgResult.Image> list = imageService.localImageTransformShopeeImage("product", shopId, imageList);
            if (CommonUtils.isNotBlank(list)) {
                List<String> images = list.stream()
                        .map(e -> CommonUtils.isNotBlank(e.getShopeeImageUrl()) ? e.getShopeeImageUrl() : e.getImageUrl())
                        .filter(CommonUtils::isNotBlank)
                        .collect(toList());
                productDTO.setImages(images);
                shopeeProductService.update(productDTO);

                //装载失败结果
                List<UploadImgResult.Image> errorData = list.stream().filter(e -> CommonUtils.isNotBlank(e.getError()))
                        .collect(toList());
                result.setData(list);
                result.setFail(errorData.size());
            }
            result.setSuccess(result.getTotal() - result.getFail());
        } catch (Exception e) {
            log.error("shopee图片换链失败,productId={}", productDTO.getId(), e);
            result.setErrorMessage(CommonUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "系统繁忙");
            if (e instanceof ShopeeApiException) {
                result.setErrorMessage("shopee接口异常");
            }
        }
        TaskExecuteVO.TaskDetail taskDetail = TaskExecuteVO.TaskDetail.builder()
                .taskId(taskId)
                .detailId(String.valueOf(productId))
                .detailName(getImageToshopeeTaskDetail(String.valueOf(productId), locale))
                .errorMessage(result.getErrorMessage())
                .code(CommonUtils.isBlank(result.getErrorMessage()) && result.getFail() == 0 ? TaskExecuteVO.CODE_SUCCESS : TaskExecuteVO.CODE_FAILD)
                .total(result.getTotal())
                .success(result.getSuccess())
                .fail(result.getFail())
                .detailData(result.getData())
                .build();
        taskExecutorUtils.handleTaskSet(taskDetail);
        return result;
    }

    private String getImageToshopeeTaskDetail(String productId, Locale locale) {
        String var = "local_image_to_shopee";
        return handleMessageSource.getMessage(var, new String[]{productId}, locale);
    }




}
