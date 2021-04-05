package com.izhiliu.erp.service.item.mq;

import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.core.common.constant.ShopeeConstant;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.common.ShopInfoRedisUtils;
import com.izhiliu.erp.common.TaskExecutorUtils;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.*;
import com.izhiliu.erp.service.item.dto.*;
import com.izhiliu.erp.service.module.metadata.dto.PriceRange;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApi;
import com.izhiliu.open.shopee.open.sdk.api.item.ItemApiImpl;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetItemDetailResult;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetItemListResult;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetVariationsResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

/**
 * @Author: louis
 * @Date: 2020/9/25 11:33
 */
@Service
@Slf4j
public class PullAction {

    ForkJoinPool syncPool = new ForkJoinPool(20);

    @Resource
    private ShopeeProductService shopeeProductService;
    @Resource
    private ShopeeCategoryService shopeeCategoryService;
    @Resource
    private PlatformNodeService platformNodeService;
    @Resource
    private TaskExecutorUtils taskExecutorUtils;
    @Resource
    private ShopeeProductAttributeValueService shopeeProductAttributeValueService;
    @Resource
    private ShopeeSkuAttributeService shopeeSkuAttributeService;
    @Resource
    private ShopeeProductSkuService shopeeProductSkuService;
    @Resource
    private ShopeeProductMediaService shopeeProductMediaService;
    @Resource
    private ApplicationProperties applicationProperties;
    @Resource
    private ShopInfoRedisUtils shopInfoRedisUtils;
    @Resource
    private PullAction pullAction;

    private ItemApi syncItemApi = new ItemApiImpl(new OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build());;

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            10,
            50,
            1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2000),
            new ThreadFactoryBuilder().build());

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean pull(ShopeePullMessageDTO dto) {
        final String login = dto.getLogin();
        final String taskId = dto.getTaskId();
        final Long shopId = dto.getShopId();
        List<GetItemListResult.ItemsBean> items = dto.getItems();
        if (CommonUtils.isBlank(login) || CommonUtils.isBlank(taskId) || CommonUtils.isBlank(items)) {
            log.error("同步商品消费失败,param error,login={},taskId={}", login, taskId);
            return true;
        }

        int count = (int) items.stream().filter(e -> !ShopeeItemStatus.DELETED.getStatus().equalsIgnoreCase(e.getStatus()))
                .count();

        //总进度
        taskExecutorUtils.incrementSyncHash(taskId, ShopeePullMessageDTO.HANDLE_TOTAL, count);
        taskExecutorUtils.incrementSyncHash(taskId, "success", count);
        //店铺进度
        taskExecutorUtils.incrementSyncShopHash(taskId, ShopeePullMessageDTO.HANDLE_TOTAL, shopId, count);
        taskExecutorUtils.incrementSyncShopHash(taskId, "success", shopId, count);

        syncPool.execute(() -> {
            items.parallelStream().forEach(item -> {
                final long itemId = item.getItemId();
                final long shopid = item.getShopid();
                Optional<ShopeeProductDTO> productExist = shopeeProductService.selectByItemId(itemId);
                ShopeeProductDTO shopeeProductDTO = ShopeeProductDTO.builder()
                        .shopeeItemId(itemId)
                        .shopId(shopid)
                        .loginId(login)
                        .build();
                if (productExist.isPresent() && Objects.equals(productExist.get().getLoginId(), login)) {
                    shopeeProductDTO = productExist.get();
                    if (LocalProductStatus.IN_PULL.equals(shopeeProductDTO.getStatus()) && shopeeProductDTO.getGmtModified().plus(3, ChronoUnit.MINUTES).isAfter(Instant.now())) {
                        return;
                    }
                    //如果shopee更新时间未变，则直接返回成功
                    shopeeProductService.updateStatus(shopeeProductDTO.getId(), LocalProductStatus.IN_PULL);
                }
                try {
                    saveShopeeItem(taskId, shopeeProductDTO);
                } catch (Exception e) {
                    log.error("同步商品详情出错,itemId={},shopId={},login={}", itemId, shopid, login, e);
                }
            });
        });


        return true;
    }

    private boolean saveShopeeItem(String taskId, ShopeeProductDTO shopeeProductDTO) {
        final long itemId = shopeeProductDTO.getShopeeItemId();
        final long shopid = shopeeProductDTO.getShopId();
        ShopeeResult<GetItemDetailResult> itemDetail = syncItemApi.getItemDetail(shopid, itemId);
        if (CommonUtils.isNotBlank(itemDetail.getError()) && CommonUtils.isNotBlank(itemDetail.getError().getMsg())) {
            log.error("shopee获取商品详情出错,itemId={},errorMsg={}", itemId, itemDetail.getError().getMsg());
            throw new LuxServerErrorException("shopee获取商品详情出错");
        }
        GetItemDetailResult.ItemBean item = itemDetail.getData().getItem();
        final long shopeeCategoryId = Integer.toUnsignedLong(item.getCategoryId());
        final String currency = item.getCurrency();
        final Long price = ShopeeUtil.apiInputPrice(item.getPrice(), currency);
        final Integer stock = item.getStock();
        final String status = item.getStatus();
        final Long categoryId = getCategoryId(shopeeCategoryId, currency);

        log.info("[同步商品] - 商品状态: {}:{}", itemId, item.getStatus());
        if (ShopeeItemStatus.DELETED.getStatus().equalsIgnoreCase(status)) {
            shopeeProductService.deleteByItemId(itemId);
            return false;
        }
        threadPoolExecutor.execute(() -> pullAction.saveProduct(shopeeProductDTO, item));
        return true;
    }




    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShopeeProductDTO saveProduct(ShopeeProductDTO dto, GetItemDetailResult.ItemBean item) {
        final long shopeeCategoryId = Integer.toUnsignedLong(item.getCategoryId());
        final String name = item.getName();
        final String description = item.getDescription();
        final String currency = item.getCurrency();
        final Long price = ShopeeUtil.apiInputPrice(item.getPrice(), currency);
        final Integer stock = item.getStock();
        final String skuCode = item.getItemSku();
        final Integer sendOutTime = item.getDaysToShip();
        final Long weight = ShopeeUtil.inputWeight(item.getWeight());
        final Integer sold = item.getSales();
        final String images = JSON.toJSONString(item.getImages());
        final String sizeChart = item.getSizeChart();
        final String status = item.getStatus();
        final Long categoryId = getCategoryId(shopeeCategoryId, currency);
        final List<GetItemDetailResult.ItemBean.WholesaleBean> wholesales = item.getWholesales();
        final String loginId = dto.getLoginId();
        final Long shopId = dto.getShopId();
        final Long shopeeItemId = item.getItemId();


        ShopeeProductDTO shopeeProductDTO = ShopeeProductDTO.builder()
                .id(dto.getId())
                .type(ShopeeProduct.Type.SHOP.code)
                .loginId(loginId)
                .shopId(shopId)
                .shopeeItemId(shopeeItemId)
                .name(name)
                .description(description)
                .currency(currency)
                .price(price)
                .stock(stock)
                .skuCode(skuCode)
                .sendOutTime(sendOutTime)
                .weight(weight)
                .sold(sold)
                .categoryId(categoryId)
                .shopeeCategoryId(shopeeCategoryId)
                .platformNodeId(ShopeeUtil.nodeId(currency))
                .platformId(PlatformEnum.SHOPEE.getCode().longValue())
                .images(JSON.parseArray(images, String.class))
                .shopeeItemStatus(ShopeeUtil.itemStatus(status, stock))
                .warning(dto.getWarning())
                .onlineUrl(getOnlineUrl(currency, name, shopId, shopeeItemId))
                .collect(PlatformEnum.SHOPEE.getName())
                .likes(item.getLikes())
                .views(item.getViews())
                .sales(item.getSales())
                .isCondition(Objects.equals(item.getCondition(), "USED"))
                .ratingStar(item.getRatingStar())
                .cmtCount(item.getCmtCount())
                .discountActivityId(Integer.valueOf(item.getDiscountId()).longValue())
                .sizeChart(sizeChart)
                .build();
        if (null != item.getPackageLength() && item.getPackageLength() > 0) {
            shopeeProductDTO.setLength(item.getPackageLength() * 10);
        }
        if (null != item.getPackageWidth() && item.getPackageWidth() > 0) {
            shopeeProductDTO.setWidth(item.getPackageWidth() * 10);
        }
        if(null != item.getPackageHeight() && item.getPackageHeight()>0){
            shopeeProductDTO.setHeight(item.getPackageHeight()*10);
        }
        shopeeProductDTO.setLogistics(item.getLogistics().stream().map(v -> {
            ShopeeProductDTO.Logistic logistic = new ShopeeProductDTO.Logistic();
            logistic.setLogisticId((long) v.getLogisticId());
            logistic.setLogisticName(v.getLogisticName());
            logistic.setIsFree(v.isFree());
            logistic.setEnabled(v.isEnabled());
            return logistic;
        }).collect(toList()));
        if (!CollectionUtils.isEmpty(wholesales)) {
            List<PriceRange> priceRange = wholesales.stream().map(e -> {
                PriceRange build = PriceRange.builder()
                        .max(e.getMax())
                        .min(e.getMin())
                        .price(e.getUnitPrice())
                        .build();
                return build;
            }).collect(toList());
            shopeeProductDTO.setPriceRange(priceRange);
        }
        if (!Objects.equals(item.getOriginalPrice(), item.getPrice())) {
            final Long originalPrice = ShopeeUtil.apiInputPrice(item.getOriginalPrice(), currency);
            shopeeProductDTO.setOriginalPrice(originalPrice);
        }
        if (CommonUtils.isNotBlank(shopeeProductDTO.getId())) {
            shopeeProductService.update(shopeeProductDTO);
        } else {
            ShopeeProductDTO save = shopeeProductService.save(shopeeProductDTO);
            shopeeProductDTO.setId(save.getId());
        }
        final Long productId = shopeeProductDTO.getId();
        if (categoryId != null && categoryId != 0L) {
            shopeeProductAttributeValueService.saveAttribute(item, shopeeCategoryId, categoryId, productId);
        }

        saveVariation(shopId, item, currency, price, stock, productId);

        success(productId, Optional.empty(), Optional.empty(), LocalProductStatus.PULL_SUCCESS);

        return shopeeProductDTO;
    }


    private Long getCategoryId(long shopeeCategoryId, String currency) {
        ShopeeCategoryDTO shopeeCategoryDTO = shopeeCategoryService.findByPlatformNodeAndShopeeCategory(ShopeeUtil.nodeId(currency), shopeeCategoryId)
                .orElseGet(() -> new ShopeeCategoryDTO());
        return CommonUtils.isNotBlank(shopeeCategoryDTO.getId()) ? shopeeCategoryDTO.getId() : 0L;
    }

    /**
     * 获取在线地址
     *
     * @param currency 货币代码
     * @param name     商品标题
     * @param shopId   虾皮店铺ID
     * @param itemId   虾皮商品ID
     * @return 商品在线URL
     */
    protected String getOnlineUrl(String currency, String name, Long shopId, Long itemId) {
        final Long id = ShopeeUtil.nodeId(currency);
        Optional<PlatformNodeDTO> platformNodeDTO = platformNodeService.find(id);
        return String.format(ShopeeConstant.ITEM_DETAILED_URL, platformNodeDTO.get().getUrl(), name.replaceAll(" ", "-").replaceAll("%", ""), shopId, itemId);
    }


    public void saveVariation(Long shopId, GetItemDetailResult.ItemBean item, String currency, Long price, Integer stock, long productId) {
        /*
         * 清除本地
         */
        shopeeSkuAttributeService.deleteByProduct(productId);

        final List<GetItemDetailResult.ItemBean.VariationBean> variations = item.getVariations();

        int variationTier = 0;
        if (!item.isHasVariation()) {
            log.info("[单品]: {}", item.getItemId());

            shopeeProductSkuService.deleteByProduct(productId);
            singleProduct(item, currency, price, stock, productId);
        } else {
            final List<Long> allIds = shopeeProductSkuService.pageByProduct(
                    productId,
                    new Page(0, Integer.MAX_VALUE))
                    .getRecords()
                    .stream()
                    .map(ShopeeProductSkuDTO::getShopeeVariationId)
                    .collect(toList());

            /*
             * TODO 只存在一个 ',' 分隔符 必定是双SKU
             *  若存在多个则需要调接口检查才行
             */
            ShopeeResult<GetVariationsResult> getVariationsApiResult = syncItemApi.getVariations(shopId, item.getItemId());
            int count = ReUtil.count(",", variations.get(0).getName());
            if (count > 1) {
                if (getVariationsApiResult.isResult()) {
                    count = 1;
                }
            }
            if (count == 0) {
                log.info("[单SKU]: {}", item.getItemId());
                variationTier = 1;
                // getVariationsApiResult = itemApi.getVariations(shopId, item.getItemId());
                if (getVariationsApiResult.isResult()) {
                    allIds.removeAll(twoSku(shopId, item, currency, productId, variations, getVariationsApiResult));
                } else if (getVariationsApiResult.getError().getMsg().contains("This api can only support item which has 2-tier variations")) {
                    allIds.removeAll(oneSku(currency, productId, variations));
                }
            } else {
                log.info("[双SKU]: {}", item.getItemId());
                variationTier = 2;
                allIds.removeAll(twoSku(shopId, item, currency, productId, variations, getVariationsApiResult));
            }
            shopeeProductSkuService.deleteByVariationId(productId, allIds.stream().filter(Objects::nonNull).collect(toList()));
            shopeeProductSkuService.deleteAllNullVariationIdByProduct(productId);
        }

        updateVariationTier(productId, variationTier);
    }


    private void updateVariationTier(Long productId, int variationTier) {
        final ShopeeProductDTO update = new ShopeeProductDTO();
        update.setId(productId);
        update.setVariationTier(variationTier);
        shopeeProductService.update(update);
    }

    /**
     * 单品处理
     */
    private void singleProduct(GetItemDetailResult.ItemBean item, String currency, Long price, Integer stock, Long productId) {
        final ShopeeProductSkuDTO productSkuDTO = new ShopeeProductSkuDTO();
        productSkuDTO.setProductId(productId);
        productSkuDTO.setCurrency(currency);
        productSkuDTO.setPrice(ShopeeUtil.outputPrice(price, currency));
        productSkuDTO.setStock(stock);
        productSkuDTO.setSkuCode(item.getItemSku());
        if (!Objects.equals(item.getOriginalPrice(), item.getPrice())) {
            productSkuDTO.setOriginalPrice(item.getOriginalPrice());
            productSkuDTO.setDiscount(productSkuDTO.getPrice());
        } else {
            //折扣变成非折扣，需要还原
            productSkuDTO.setOriginalPrice(0F);
            productSkuDTO.setDiscount(0F);
        }
        productSkuDTO.setDiscountId(Integer.valueOf(item.getDiscountId()).longValue());
//        shopeeProductSkuService.findByProductId(productId).ifPresent(e -> productSkuDTO.setId(e.getId()));
        shopeeProductSkuService.replace(productSkuDTO);
        setShopProductDiscountId(productId, false, productSkuDTO.getDiscountId());
        refreshProductPrice(price, productId, currency, null, null);
    }

    private boolean setShopProductDiscountId(long productId, boolean isStop, Long discountId) {
        if (!isStop && Objects.nonNull(discountId) && discountId > 0L) {
            isStop = true;
            final ShopeeProductMediaDTO shopeeProductMediaDTO = new ShopeeProductMediaDTO();
            shopeeProductMediaDTO.setProductId(productId);
            shopeeProductMediaService.updateByProductId(shopeeProductMediaDTO.setDiscountActivityId(discountId));
        }
        return isStop;
    }

    private boolean setShopProductDiscountId(long productId, boolean isStop, Long discountId, List<ShopeeProductMediaDTO> list) {
        if (!isStop && Objects.nonNull(discountId) && discountId > 0L) {
            isStop = true;
            final ShopeeProductMediaDTO shopeeProductMediaDTO = new ShopeeProductMediaDTO();
            shopeeProductMediaDTO.setProductId(productId);
            shopeeProductMediaDTO.setDiscountActivityId(discountId);
            list.add(shopeeProductMediaDTO);
            /* shopeeProductMediaService.updateByProductId();*/
        }
        return isStop;
    }

    private void refreshProductPrice(Long price, Long productId, String currency, Float minPrice, Float maxPrice) {
        final ShopeeProductDTO product = new ShopeeProductDTO();
        product.setId(productId);
        product.setCurrency(currency);

        if (minPrice == null || maxPrice == null) {
            product.setPrice(price);
        } else {
            if (minPrice.equals(maxPrice)) {
                product.setVPrice(minPrice);
            } else {
                product.setMinPrice(minPrice);
                product.setMaxPrice(maxPrice);
            }
        }
        shopeeProductService.update(product);
    }

    private List<Long> twoSku(Long shopId, GetItemDetailResult.ItemBean item, String currency, long productId, List<GetItemDetailResult.ItemBean.VariationBean> variations, ShopeeResult<GetVariationsResult> getVariationsApiResult) {
        final List<Long> ids = variations.stream().map(GetItemDetailResult.ItemBean.VariationBean::getVariationId).collect(toList());

        // final ShopeeResult<GetVariationsResult> getVariationsApiResult = itemApi.getVariations(shopId, item.getItemId());
        if (!getVariationsApiResult.isResult() || getVariationsApiResult.getData() == null) {
            return ids;
        }

        final List<GetVariationsResult.VariationsBean> variationIndexs = getVariationsApiResult.getData().getVariations();
        final List<GetVariationsResult.TierVariationBean> variationAttributes = getVariationsApiResult.getData().getTierVariation();

        if (variationIndexs == null || variationAttributes == null || variationIndexs.size() == 0 || variationAttributes.size() == 0) {
            log.error("[双SKU数据异常]");
            return ids;
        }

        boolean isStop = false;
        /*
         * TODO 双SKU实质是单品的情况
         *  当单SKU存即可
         */
        final boolean isTwo = variationAttributes.size() > ShopeeProduct.VariationTier.ONE.val;

        saveSkuAttribute(productId, variationAttributes.get(0));
        if (isTwo) {
            saveSkuAttribute(productId, variationAttributes.get(1));
        }
        List<ShopeeProductSkuDTO> list = new ArrayList<>();
        List<ShopeeProductMediaDTO> shopeeProductMediaDTOList = new ArrayList<>();
        for (int i = 0; i < variationIndexs.size(); i++) {
            final GetVariationsResult.VariationsBean variationIndex = variationIndexs.get(i);
            final ShopeeProductSkuDTO productSku = getProductSku(currency, productId, variationIndex, variations.get(i));
            if (isTwo) {
                productSku.setSkuOptionTowIndex(variationIndex.getTierIndex().get(1));
            }
            isStop = setShopProductDiscountId(productId, isStop, productSku.getDiscountId(), shopeeProductMediaDTOList);
            list.add(productSku);
        }
        shopeeProductMediaService.updateByProductIds(shopeeProductMediaDTOList);
        if (list.size() > 0) shopeeProductSkuService.replaceAll(list);
        refreshPrice(productId, currency, variations);
        return ids;
    }

    private void saveSkuAttribute(Long productId, GetVariationsResult.TierVariationBean variationAttribute) {
        final ShopeeSkuAttributeDTO skuAttributeDTO = new ShopeeSkuAttributeDTO();
        skuAttributeDTO.setProductId(productId);
        skuAttributeDTO.setName(variationAttribute.getName());
        skuAttributeDTO.setOptions(variationAttribute.getOptions());
        skuAttributeDTO.setImagesUrl(variationAttribute.getImagesUrl());
        shopeeSkuAttributeService.save(skuAttributeDTO);
    }

    private ShopeeProductSkuDTO getProductSku(String currency, Long productId, GetVariationsResult.VariationsBean variationIndex, GetItemDetailResult.ItemBean.VariationBean variation) {
        final ShopeeProductSkuDTO productSku = new ShopeeProductSkuDTO();
        productSku.setProductId(productId);
        productSku.setShopeeVariationId(variationIndex.getVariationId());
        productSku.setSkuCode(variation.getVariationSku());
        productSku.setSkuOptionOneIndex(variationIndex.getTierIndex().get(0));
        productSku.setStock(variation.getStock());
        productSku.setCurrency(currency);
        productSku.setPrice(variation.getPrice());
        productSku.setDiscountId(variation.getDiscountId());
        if (!Objects.equals(variation.getOriginalPrice(), variation.getPrice())) {
            productSku.setOriginalPrice(variation.getOriginalPrice());
            productSku.setDiscount(variation.getPrice());
        } else {
            //折扣变成非折扣，需要还原
            productSku.setOriginalPrice(0F);
            productSku.setDiscount(0F);
        }
        return productSku;
    }

    private void refreshPrice(Long productId, String currency, List<GetItemDetailResult.ItemBean.VariationBean> variations) {
        final List<Float> allPrice = variations.stream().map(GetItemDetailResult.ItemBean.VariationBean::getPrice).collect(toList());
        final Float max = Collections.max(allPrice);
        final Float min = Collections.min(allPrice);

        refreshProductPrice(null, productId, currency, min, max);
    }

    private List<Long> oneSku(String currency, Long productId, List<GetItemDetailResult.ItemBean.VariationBean> variations) {
        int i = 0;
        final List<String> options = new ArrayList<>(variations.size());
        //todo open-api 单sku 目前不支持sku属性图
        for (GetItemDetailResult.ItemBean.VariationBean variation : variations) {
            final ShopeeProductSkuDTO productSku = new ShopeeProductSkuDTO();
            productSku.setSkuOptionOneIndex(i++);
            productSku.setPrice(variation.getPrice());
            productSku.setShopeeVariationId(variation.getVariationId());
            productSku.setProductId(productId);
            productSku.setCurrency(currency);
            productSku.setStock(variation.getStock());
            productSku.setSkuCode(variation.getVariationSku());
            productSku.setOriginalPrice(variation.getOriginalPrice());
            productSku.setDiscountId(variation.getDiscountId());
            if (!Objects.equals(variation.getOriginalPrice(), variation.getPrice())) {
                productSku.setDiscount(productSku.getPrice());
            }
//            shopeeProductSkuService.findByVariationId(variation.getVariationId()).ifPresent(e -> productSku.setId(e.getId()));
            shopeeProductSkuService.replace(productSku);

            options.add(variation.getName());
        }

        final ShopeeSkuAttributeDTO skuAttributeDTO = new ShopeeSkuAttributeDTO();
        skuAttributeDTO.setProductId(productId);
        skuAttributeDTO.setOptions(options);
        skuAttributeDTO.setName("Variations");
        shopeeSkuAttributeService.save(skuAttributeDTO);

        refreshPrice(productId, currency, variations);

        return variations.stream().map(GetItemDetailResult.ItemBean.VariationBean::getVariationId).collect(toList());
    }

    /**
     * 执行成功
     *
     * @param productId 商品ID
     * @param itemId    虾皮商品ID
     * @param onlineUrl 在线链接
     */
    protected void success(long productId, Optional<Long> itemId, Optional<String> onlineUrl, LocalProductStatus status) {
        final ShopeeProductDTO product = new ShopeeProductDTO();
        itemId.ifPresent(product::setShopeeItemId);
        onlineUrl.ifPresent(product::setOnlineUrl);
        product.setId(productId);
        product.setFeature(JSONObject.toJSONString(new HashMap<String, String>() {{
            put("error", "success");
        }}));
        product.setGmtModified(Instant.now());
        product.setStatus(status);
        shopeeProductService.update(product);
    }

}
