package com.izhiliu.erp.service.item.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.config.module.currency.CurrencyRateApiImpl;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import com.izhiliu.erp.repository.item.ShopeeProductSkuRepository;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.ShopeeSkuAttributeService;
import com.izhiliu.erp.service.item.dto.ShopeeProductCopyDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;
import com.izhiliu.erp.service.item.mapper.ShopeeProductSkuMapper;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.erp.util.FeatrueUtil;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.erp.web.rest.errors.DataNotFoundException;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.item.vm.VariationMV_V2;
import com.izhiliu.erp.web.rest.item.vm.VariationVM;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.izhiliu.erp.service.item.mapper.ShopeeProductMapper.ZERO;
import static java.util.stream.Collectors.toList;

/**
 * Service Implementation for managing ShopeeProductSku.
 */
@Service
public class ShopeeProductSkuServiceImpl extends IBaseServiceImpl<ShopeeProductSku, ShopeeProductSkuDTO, ShopeeProductSkuRepository, ShopeeProductSkuMapper> implements ShopeeProductSkuService, ApplicationListener<ApplicationReadyEvent> {

    private final Logger log = LoggerFactory.getLogger(ShopeeProductSkuServiceImpl.class);

    public static final int SKU_COMBINATION_COUNT = 50;
//    public static final int SKU_OPTION_COUNT = 20;
    public static final int SKU_OPTION_COUNT = 50;
    public static final int SKU_NAME_LENGTH = 14;
    public static final int SKU_OPTION_LENGTH = 20;


    @Resource
    protected SnowflakeGenerate snowflakeGenerate;

    @Resource
    private CurrencyRateApiImpl currencyConvert;

    @Resource
    private ShopeeModelChannel shopeeModelChannel;

    @Resource
    private ShopeeProductService shopeeProductService;

    @Resource
    private ShopeeSkuAttributeService shopeeSkuAttributeService;

    @Resource
    private MessageSource messageSource;

    private Executor executor;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        executor = Executors.newFixedThreadPool(20);
    }

    @Override
    public boolean batchSave(Collection<ShopeeProductSkuDTO> entityList, int batchSize) {
        refreshPrice(entityList);

        List<ShopeeProductSkuDTO> productSkus = new ArrayList<>(entityList);

        /*
         * 按照索引排序
         *
         * 单SKU
         *  0
         *  1
         *  2
         *  ...
         *
         * 双SKU
         *  0 0
         *  0 1
         *  0 2
         *  ...
         *
         * 0 1
         * 0 0
         * 0 4
         * 0 3
         * 0 2
         */
        return super.batchSave(entityList.stream().sorted(Comparator.comparing(e -> {
            /**
             * 双SKU 两个索引都不为空
             * 单SKU
             * 单品  两个索引都为空, 只存在一个, 不需要排序
             */
            if (null != e.getSkuOptionOneIndex() && null != e.getSkuOptionTowIndex()) {
                return e.getSkuOptionOneIndex() + e.getSkuOptionTowIndex();
            } else if (null != e.getSkuOptionOneIndex()) {
                return e.getSkuOptionOneIndex();
            } else {
                return 0;
            }
        })).collect(toList()), batchSize);
    }

    public void refreshPrice(Collection<ShopeeProductSkuDTO> entityList) {
        final Map<Long, List<ShopeeProductSkuDTO>> map = entityList.stream().collect(Collectors.groupingBy(ShopeeProductSkuDTO::getProductId));
        for (Map.Entry<Long, List<ShopeeProductSkuDTO>> entry : map.entrySet()) {
            final Long productId = entry.getKey();

            final List<Float> source = entry.getValue().stream().map(ShopeeProductSkuDTO::getPrice).collect(toList());
            final Float max = Collections.max(source);
            final Float min = Collections.min(source);

            updatePriceIntervalAndStock(entry.getValue().get(0).getCurrency(), productId, null, max, min);
        }
    }

    @Override
    public boolean batchUpdate(Collection<ShopeeProductSkuDTO> entityList, int batchSize) {
        refreshPrice(entityList);

        return super.batchUpdate(entityList, batchSize);
    }

    @Override
    public boolean superBatchSave(Collection<ShopeeProductSkuDTO> entityList, int batchSize) {
        refreshPrice(entityList);

        return super.batchSave(entityList, batchSize);
    }

    /**
     *    用来统计数据的
     * @param currency   商品的   货币   不能使用  sku 的 防止 出意外
     * @param productId     当前商品的 erp  id
     * @param stock   数量
     * @param max     sku 的 最高价格
     * @param min     sku 的 最低价
     */
    protected void updatePriceIntervalAndStock(String currency, Long productId, Integer stock, Float max, Float min) {
        final ShopeeProductDTO dto = new ShopeeProductDTO();
        dto.setCurrency(currency);
        dto.setId(productId);
        if (max.equals(min)) {
            dto.setVPrice(max);
            dto.setMinPrice(ZERO);
            dto.setMaxPrice(ZERO);
        } else {
            dto.setVPrice(ZERO);
            dto.setMinPrice(min);
            dto.setMaxPrice(max);
        }
        if(Objects.nonNull(stock)){
            dto.setStock(CommonUtils.getMaxStock(stock));
        }
        shopeeProductService.superUpdate(dto);
    }

    public static Stream<ShopeeProductSku> sorted(Stream<ShopeeProductSku> stream, int length) {
        if (length == 0) {
            return stream;
        } else if (length == 1) {
            return stream.sorted(Comparator.comparingInt(value -> value.getSkuOptionOneIndex()));
        } else {
            return stream.sorted(Comparator.comparingInt((ShopeeProductSku value) -> value.getSkuOptionOneIndex()).thenComparingInt(value -> value.getSkuOptionTowIndex()));
        }
    }

    @Override
    public VariationVM variationByProduct(long productId) {
        /*
         * 获取产品、产品SKU、SKU属性
         */
        final Optional<ShopeeProductDTO> productExist = shopeeProductService.find(productId);
        if (!productExist.isPresent()) {
//            throw new DataNotFoundException("product not found", "productId : " + productId);
            return null;
        }
        final ShopeeProductDTO product = productExist.get();

        boolean isShopeeItem = Objects.nonNull(product.getShopeeItemId());
        //  兼容历史数据
        if (isShopeeItem) {
            if (Objects.isNull(product.getOriginalPrice()) || (product.getOriginalPrice() != null && 0L == product.getOriginalPrice())) {
                product.setOriginalPrice(product.getPrice());
            }
        }

        /*
         * 同步不展示,避免看到脏数据
         */
        if (product.getStatus().equals(LocalProductStatus.IN_PULL)) {
            return null;
        }

        final VariationVM variationVo;
        try {
             List<ShopeeProductSku> productSkus = repository.pageByProductId(new Page(0, Integer.MAX_VALUE), productId).getRecords();
            productSkus  = ShopeeProductSkuServiceImpl.sorted(productSkus.stream(),product.getVariationTier()).collect(toList());
            final List<ShopeeSkuAttributeDTO> skuAttributes = shopeeSkuAttributeService.pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords();

            variationVo = new VariationVM();
            variationVo.setVariationTier(product.getVariationTier());
            variationVo.setProductId(productId);

            if (ShopeeProduct.VariationTier.TWO.val.equals(product.getVariationTier())) {
                // 属性 & 属性值
                final List<VariationVM.Variation> variations = new ArrayList<>();
                for (ShopeeSkuAttributeDTO skuAttribute : skuAttributes) {
                    final List<String> options = skuAttribute.getOptions();
                    final VariationVM.Variation variation = new VariationVM.Variation();
                    variation.setName(skuAttribute.getName());
                    variation.setOptions(options);
                    variation.setImageUrls(skuAttribute.getImagesUrl());
                    variations.add(variation);
                }
                variationVo.setVariations(variations);

                // VariationIndex
                final List<VariationVM.VariationIndex> variationIndices = new ArrayList<>();
                for (ShopeeProductSku productSku : productSkus) {
                    final VariationVM.VariationIndex variationIndex = new VariationVM.VariationIndex();
                    fillVariationIndex(productSku, variationIndex);
                    //  设定价格 将会使用这俩个价格来判定是否是 折扣价
                    if (isShopeeItem) {
                        if (!Objects.isNull(product.getOriginalPrice()) && 0L != product.getOriginalPrice() && !variationIndex.getPrice().equals(variationIndex.getOriginalPrice())) {
                            variationVo.setDiscount(true);
                        }
                    }
                    variationIndex.setIndex(Arrays.asList(productSku.getSkuOptionOneIndex(), productSku.getSkuOptionTowIndex()));
                    variationIndex.setSkuImage(productSku.getImage());
                    if (FeatrueUtil.isJsonString(productSku.getFeature())) {
                        JSONObject jsonObject = JSON.parseObject(productSku.getFeature());
                        if (null != jsonObject) {
                            String specId = jsonObject.getString("specId");
                            variationIndex.setSpecId(specId);
                        }
                    }
                    variationIndices.add(variationIndex);
                }
                variationVo.setVariationIndexs(variationIndices);
            } else {
                /*
                 * 单品或单SKU属性
                 */
                final List<VariationVM.Variation> variations = new ArrayList<>();
                List<String> options = null;

                /*
                 * 非 O
                 */
                if (product.getVariationTier() != 0 && !skuAttributes.isEmpty()) {
                    variationVo.setVariationName(skuAttributes.get(0).getName());
                    options = skuAttributes.get(0).getOptions();
                }

                for (ShopeeProductSku productSku : productSkus) {
                    final VariationVM.Variation variation = new VariationVM.Variation();
                    fillVariation(productSku, variation);

                    /*
                     * 非 O
                     */
                    if (product.getVariationTier() != 0 && skuAttributes.size() != 0) {
                        variation.setName(options.get(productSku.getSkuOptionOneIndex()));
                    }
                    variation.setSkuImage(productSku.getImage());
                    if (FeatrueUtil.isJsonString(productSku.getFeature())) {
                        JSONObject jsonObject = JSON.parseObject(productSku.getFeature());
                        if (null != jsonObject) {
                            String specId = jsonObject.getString("specId");
                            variation.setSpecId(specId);
                        }
                    }
                    if (isShopeeItem) {
                        //  设定价格 将会使用这俩个价格来判定是否是 折扣价
                        if (!Objects.isNull(productSku.getOriginalPrice()) && 0L != productSku.getOriginalPrice() && !productSku.getPrice().equals(productSku.getOriginalPrice())) {
                            variationVo.setDiscount(true);
                        }
                    }
                    variations.add(variation);
                }
                variationVo.setVariations(variations);
            }


        } catch (Exception e) {
            log.error("[装配SKU异常] : {}  {}",e.getMessage(), e);
            return null;
        }
        return variationVo;
    }


    @Override
    public List<VariationMV_V2> variationsByProduct(long productId) {
        final Optional<ShopeeProductDTO> productExist = shopeeProductService.find(productId);
        if (!productExist.isPresent()) {
            return null;
        }

        final ShopeeProductDTO product = productExist.get();
        /*
         * 发布中不展示,避免看到脏数据
         */
        if (LocalProductStatus.IN_PULL.equals(product.getStatus())) {
            return null;
        }

        final List<VariationMV_V2> variations = new ArrayList<>();
        final List<ShopeeProductSkuDTO> productSkus = pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords();
        final List<ShopeeSkuAttributeDTO> skuAttributes = shopeeSkuAttributeService.pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords();

        for (ShopeeProductSkuDTO productSku : productSkus) {
            final VariationMV_V2 variation = new VariationMV_V2();
            variation.setCurrency(product.getCurrency());
            variation.setPrice(productSku.getPrice());
            variation.setSkuCode(productSku.getSkuCode() == null ? "" : productSku.getSkuCode());
            variation.setStock(productSku.getStock() == null ? 0 : productSku.getStock());
            //是否为json 如果是取出specId的value存入
            if (FeatrueUtil.isJsonString(productSku.getFeature())){
                variation.setSpecId(productSku.getFeature() == null ? "" : JSON.parseObject(productSku.getFeature()).getString("specId"));
            }

            if (ShopeeProduct.VariationTier.ONE.val.equals(product.getVariationTier()) && skuAttributes.size() == 1) {
                if (productSku.getSkuOptionOneIndex() != null && skuAttributes.get(0).getOptions().size() > productSku.getSkuOptionOneIndex()) {
                    variation.setName(skuAttributes.get(0).getOptions().get(productSku.getSkuOptionOneIndex()));
                }
            } else if (ShopeeProduct.VariationTier.TWO.val.equals(product.getVariationTier()) && skuAttributes.size() == 2) {
                if (productSku.getSkuOptionOneIndex() != null && productSku.getSkuOptionTowIndex() != null && skuAttributes.get(0).getOptions().size() > productSku.getSkuOptionOneIndex() && skuAttributes.get(1).getOptions().size() > productSku.getSkuOptionTowIndex()) {
                    variation.setName(skuAttributes.get(0).getOptions().get(productSku.getSkuOptionOneIndex()) + "—" + skuAttributes.get(1).getOptions().get(productSku.getSkuOptionTowIndex()));
                }
            }
            variations.add(variation);
        }
        return variations;
    }

    void fillVariation(ShopeeProductSku productSku, VariationVM.Variation variation) {
        variation.setId(productSku.getId());
        variation.setCurrency(productSku.getCurrency());
        variation.setPrice(ShopeeUtil.outputPrice(productSku.getPrice(), productSku.getCurrency()));
        variation.setSkuCode(productSku.getSkuCode());
        variation.setStock(productSku.getStock());
        variation.setVariationId(productSku.getShopeeVariationId());
        //  兼容历史数据
        if (Objects.isNull(productSku.getOriginalPrice()) || 0 == productSku.getOriginalPrice()) {
            variation.setOriginalPrice(variation.getPrice());
        } else {
            variation.setOriginalPrice(ShopeeUtil.outputPrice(productSku.getOriginalPrice(), productSku.getCurrency()));
        }
//        variation.setDiscount(Objects.isNull(productSku.getDiscount())?0:productSku.getDiscount());
    }

    void fillVariationIndex(ShopeeProductSku productSku, VariationVM.VariationIndex variationIndex) {
        variationIndex.setId(productSku.getId());
        variationIndex.setCurrency(productSku.getCurrency());
        variationIndex.setPrice(ShopeeUtil.outputPrice(productSku.getPrice(), productSku.getCurrency()));
        variationIndex.setSkuCode(productSku.getSkuCode());
        variationIndex.setStock(productSku.getStock());
        variationIndex.setVariationId(Objects.isNull(productSku.getShopeeVariationId()) ? 0 : productSku.getShopeeVariationId());
        //  兼容历史数据
        if (Objects.isNull(productSku.getOriginalPrice()) || 0 == productSku.getOriginalPrice()) {
            variationIndex.setOriginalPrice(variationIndex.getPrice());
        } else {
            variationIndex.setOriginalPrice(ShopeeUtil.outputPrice(productSku.getOriginalPrice(), productSku.getCurrency()));
        }
//        variationIndex.setDiscount(Objects.isNull(productSku.getDiscount())?0:productSku.getDiscount());
    }


    @Override
    public IPage<ShopeeProductSkuDTO> pageByProduct(long productId, Page pageable) {
        return toDTO(repository.pageByProductId(pageable, productId));
    }

    /**
     * 删除已删除的SKU
     */

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void coverTheProduct(VariationVM param) {
        final ShopeeProductDTO product = checkParam(
                param,
                shopeeProductService.find(param.getProductId())
        );
        if (Objects.isNull(param.getOriginalPrice())) {
            product.setOriginalPrice(ShopeeUtil.apiInputPrice(param.getPrice(), product.getCurrency()));
        }
        coveringProductsAccordingToVariants(param, product);
        // 所有保存只保存到表，不push 到shopee,push 由/api/shop-products/push/v2 来决定
//        updateToShopee(product);
    }

    /**
     *      根据 变种参数 覆盖产品里面的数据
     * @param param
     * @param product
     */
    @Transactional(rollbackFor = Exception.class)
    public void coveringProductsAccordingToVariants(VariationVM param, ShopeeProductDTO product) {
        shopeeSkuAttributeService.deleteByProduct(param.getProductId());
        final List<ShopeeProductSkuDTO> list = pageByProduct(param.getProductId(), new Page(0, Integer.MAX_VALUE)).getRecords();

        final List<VariationVM.Variation> variations = param.getVariations();
        final List<Long> ids;
        if (Objects.equals(ShopeeProduct.VariationTier.TWO.val,param.getVariationTier())) {
            ids = towSkuAttribute(param, variations);
        } else {
            //  todo 这个暂时不支持图片 所以不做处理。。。
            ids = oneOrNoSkuAttribute(param, variations, product.getCurrency());
        }
        clearDirtyData(list, ids);
        refreshVariationTier(param, product);
    }


    void updateToShopee(ShopeeProductDTO product) {
        /*
         * 强校验后更新
         * 直接更新
         */
        try {
            if (product.getShopeeItemId() != null && product.getShopeeItemId() != 0) {
                shopeeProductService.checkParam(product);

                executor.execute(() -> shopeeModelChannel.push(product.getId(), product.getShopId(), product.getLoginId()));
            }
        } catch (Exception e) {
            log.error("[updateToShopee]", e);
        }
    }

    public void refreshVariationTier(VariationVM param, ShopeeProductDTO product) {
        final ShopeeProductDTO shopeeProductDTO = new ShopeeProductDTO();
        shopeeProductDTO.setId(param.getProductId());
        shopeeProductDTO.setVariationTier(param.getVariationTier());
        if (!param.getVariationTier().equals(product.getVariationTier())) {
            shopeeProductDTO.setOldVariationTier(product.getVariationTier());
        }
        shopeeProductService.superUpdate(shopeeProductDTO);
    }


    public void clearDirtyData(List<ShopeeProductSkuDTO> list, List<Long> ids) {
        final List<Long> oldId = list.stream().map(ShopeeProductSkuDTO::getId).collect(toList());
        oldId.removeAll(ids);

        log.info("[删除过时SKU]： {}", oldId);
        delete(oldId);

    }





    @Override
    @Transactional(rollbackFor = Exception.class)
    public void coverByProductSource(VariationVM param, ShopeeProductDTO product) {

        final List<VariationVM.Variation> variations = param.getVariations();
        if (ShopeeProduct.VariationTier.TWO.val.equals(param.getVariationTier())) {
            towSkuAttribute(param, variations);
        } else {
            oneOrNoSkuAttribute(param, variations, product.getCurrency());
        }
        insertVariationTier(param, product);
    }

    public void insertVariationTier(VariationVM param, ShopeeProductDTO product) {
        final ShopeeProductDTO shopeeProductDTO = new ShopeeProductDTO();
        shopeeProductDTO.setId(param.getProductId());
        shopeeProductDTO.setVariationTier(param.getVariationTier());

        if (!param.getVariationTier().equals(product.getVariationTier())) {
            shopeeProductDTO.setOldVariationTier(product.getVariationTier());
        }
        shopeeProductService.superUpdate(shopeeProductDTO);
    }

    /**
     * 索引不能越界
     *
     * @param indexs
     * @param vm
     * @return
     */
    private boolean checkIndex(List<Integer> indexs, VariationVM vm) {
        if (indexs == null || 2 != indexs.size()) {
            return true;
        }
        return (indexs.get(0) < 0 || indexs.get(0) > vm.getVariations().get(0).getOptions().size() - 1) || indexs.get(1) < 0 || indexs.get(1) > vm.getVariations().get(1).getOptions().size() - 1;
    }

    /**
     * 参数校验
     */
    @Override
    public ShopeeProductDTO checkParam(VariationVM param,  Optional<ShopeeProductDTO> productSupplier) {
        if (CommonUtils.isBlank(param) || param.getProductId() == null) {
            throw new DataNotFoundException("data.not.found.exception.product.can.not.null", true);
        }
        final ShopeeProductDTO product =productSupplier
                .orElseThrow(() ->
                        new DataNotFoundException("data.not.found.exception.product.not.found", new String[]{"product id : " + param.getProductId()}));

        //todo 用户权限校验逻辑
//        if (!product.getLoginId().equals(param.getLogin())) {
//            throw new IllegalOperationException("非法操作");
//        }

        /*
         * 台湾站不支持双SKU
         */
//        if (PlatformNodeEnum.SHOPEE_TW.id.equals(product.getPlatformNodeId()) && ShopeeProduct.VariationTier.TWO.val.equals(param.getVariationTier())) {
//            throw new IllegalOperationException("sku.tw.not.two.sku",true);
//        }

        /*
         * 已发布的商品 不能降级
         */
        if (ShopeeProduct.Type.SHOP.code.equals(product.getType()) &&
                product.getShopeeItemId() != null &&
                product.getShopeeItemId() != 0) {
            if (ShopeeProduct.VariationTier.TWO.val.equals(product.getVariationTier()) &&
                    !product.getVariationTier().equals(param.getVariationTier())) {
                throw new IllegalOperationException("sku.already.publish.not.down", true);
            }
        }

        if (ShopeeProduct.VariationTier.TWO.val.equals(param.getVariationTier())) {
            if (CollectionUtils.isEmpty(param.getVariations())) {
                throw new IllegalOperationException("sku.attribute.not.null", true);
            }
            for (VariationVM.Variation variation : param.getVariations()) {
                if(CollectionUtils.isEmpty(variation.getOptions())) {
                    throw new IllegalOperationException("sku.option.not.null", true);
                }
            }
            if (CollectionUtils.isEmpty(param.getVariationIndexs())) {
                throw new IllegalOperationException("sku.not.null", true);
            }
            if (param.getVariations().stream().map(VariationVM.Variation::getName).anyMatch(StringUtils::isBlank)) {
                throw new IllegalOperationException("sku.name.not.null", true);
            }
            if (param.getVariations().stream().map(VariationVM.Variation::getOptions).flatMap(Collection::stream).anyMatch(StringUtils::isBlank)) {
                throw new IllegalOperationException("sku.option.name.not.null", true);
            }
            if (param.getVariationIndexs().size() > SKU_COMBINATION_COUNT || param.getVariations().get(0).getOptions().size() * param.getVariations().get(1).getOptions().size() > SKU_COMBINATION_COUNT) {
                throw new IllegalOperationException("sku.size", new String[]{Integer.toString(SKU_COMBINATION_COUNT)});
            }

            if (param.getVariations().size() != param.getVariations().stream().map(VariationVM.Variation::getName).distinct().count()) {
                throw new IllegalOperationException("sku.name.repeat", true);
            }

            if (param.getVariationIndexs().size() != param.getVariationIndexs().stream().map(VariationVM.VariationIndex::getIndex).distinct().count()) {
                throw new IllegalOperationException("sku.repeat", true);
            }

            if (param.getVariationIndexs().stream().map(VariationVM.VariationIndex::getIndex).anyMatch(e -> checkIndex(e, param))) {
                throw new IllegalOperationException("sku.index", true);
            }


            for (VariationVM.Variation variation : param.getVariations()) {
                if (variation.getOptions().size() != variation.getOptions().stream().distinct().count()) {
                    throw new IllegalOperationException("sku.option.repeat", true);
                }

                if (variation.getName().length() > SKU_NAME_LENGTH) {
                    throw new IllegalOperationException("sku.name.length", new String[]{Integer.toString(SKU_NAME_LENGTH)});
                }
                if (variation.getOptions().size() > SKU_OPTION_COUNT) {
                    throw new IllegalOperationException("sku.option.size", new String[]{Integer.toString(SKU_OPTION_COUNT)});
                }
                for (String option : variation.getOptions()) {
                    if (option.length() > SKU_OPTION_LENGTH) {
                        throw new IllegalOperationException("sku.option.length", new String[]{Integer.toString(SKU_OPTION_LENGTH)});
                    }
                }
            }

            for (VariationVM.VariationIndex variationIndex : param.getVariationIndexs()) {
                if (variationIndex.getPrice() == null || variationIndex.getPrice() < 0.1F || variationIndex.getStock() == null) {
                    throw new IllegalOperationException("sku.price.stock.not.null", true);
                }
                if (StringUtils.isBlank(variationIndex.getCurrency())) {
                    variationIndex.setCurrency(product.getCurrency());
                }
            }
        } else if (ShopeeProduct.VariationTier.ONE.val.equals(param.getVariationTier())) {
            if (param.getVariations() == null || param.getVariations().size() == 0) {
                throw new IllegalOperationException("sku.not.null", true);
            }
            if (param.getVariations().size() != param.getVariations().stream().map(VariationVM.Variation::getName).distinct().count()) {
                throw new IllegalOperationException("sku.name.repeat", true);
            }

            if (StringUtils.isNotBlank(param.getVariationName()) && param.getVariationName().length() > SKU_NAME_LENGTH) {
                throw new IllegalOperationException("sku.name.length", new String[]{Integer.toString(SKU_NAME_LENGTH)});
            }
            if (param.getVariations().size() > SKU_OPTION_COUNT) {
                throw new IllegalOperationException("sku.option.size", new String[]{Integer.toString(SKU_OPTION_COUNT)});
            }
            for (VariationVM.Variation variation : param.getVariations()) {
                if (variation == null || variation.getName() == null) {
                    throw new IllegalOperationException("sku.option.name.not.null", true);
                }
                if (variation.getName().length() > SKU_OPTION_LENGTH) {
                    throw new IllegalOperationException("sku.option.length", new String[]{Integer.toString(SKU_OPTION_LENGTH)});
                }
            }

            for (VariationVM.Variation variation : param.getVariations()) {
                if (variation.getPrice() == null || variation.getPrice() < 0.1F || variation.getStock() == null) {
                    throw new IllegalOperationException("sku.price.stock.not.null", true);
                }
                if (StringUtils.isBlank(variation.getCurrency())) {
                    variation.setCurrency(product.getCurrency());
                }
            }
        }
        return product;
    }

    /**
     * 单个或没有SKU属性
     */
    public List<Long> oneOrNoSkuAttribute(VariationVM param, List<VariationVM.Variation> variations, String currency) {
        final List<String> options = new ArrayList<>(variations.size());

        /*
         * 保存商品SKU
         */
        for (int i = 0; i < variations.size(); i++) {
            final VariationVM.Variation variation = variations.get(i);
            final ShopeeProductSkuDTO productSku = fillProductSku(pageByProduct(param.getProductId(),new Page(0,Integer.MAX_VALUE)).getRecords().stream().findFirst().orElse(new ShopeeProductSkuDTO()), variation.getPrice(), currency, variation.getSkuCode(), variation.getStock(), param.getProductId(), variation.getId(), variation.getVariationId());
            productSku.setOriginalPrice(variation.getOriginalPrice());
            productSku.setDeleted(0);
            productSku.setDiscount(variation.getDiscount());
//            productSku.setDiscountId(variation.getDiscountId());
            if (ShopeeProduct.VariationTier.ONE.val.equals(param.getVariationTier())) {
                productSku.setSkuOptionOneIndex(i);
            }

            saveOrUpdate(productSku);
            options.add(variation.getName());
        }

        if (ShopeeProduct.VariationTier.ONE.val.equals(param.getVariationTier())) {
            final ShopeeSkuAttributeDTO skuAttribute = new ShopeeSkuAttributeDTO();
            Optional.ofNullable(param.getSkuAttributeId()).ifPresent(skuAttribute::setId);
            skuAttribute.setProductId(param.getProductId());
            skuAttribute.setName(param.getVariationName());
            skuAttribute.setOptions(options);
            shopeeSkuAttributeService.saveOrUpdate(skuAttribute);

        }

        updatePriceIntervalAndStock(currency,param.getProductId(),variations.stream().mapToInt(VariationVM.Variation::getStock).sum() ,  variations.stream().map(VariationVM.Variation::getPrice).collect(toList()));

        return variations.stream().map(VariationVM.Variation::getId).filter(Objects::nonNull).collect(toList());
    }

    void updatePriceIntervalAndStock(String currency,Long productId, Integer stock, List<Float> prices) {
        if (prices != null) {
            final Float max = Collections.max(prices);
            final Float min = Collections.min(prices);

            updatePriceIntervalAndStock(currency,productId, stock, max, min);
        }
    }

    /**
     * 双SKU属性
     */
    public List<Long> towSkuAttribute(VariationVM param, List<VariationVM.Variation> variations) {
        final List<VariationVM.VariationIndex> variationIndexs = param.getVariationIndexs();

        /*
         * 保存SKU属性 And 属性可选项
         */
        for (VariationVM.Variation variation : variations) {
            final Optional<Long> id = Optional.ofNullable(variation.getId());
            final String name = variation.getName();
            final List<String> options = variation.getOptions();

            final ShopeeSkuAttributeDTO skuAttribute = new ShopeeSkuAttributeDTO();
            id.ifPresent(skuAttribute::setId);
            skuAttribute.setProductId(param.getProductId());
            skuAttribute.setName(name);
            skuAttribute.setOptions(options);
            final List<String> imageUrls = variation.getImageUrls();
            if(Objects.nonNull(imageUrls)&&(!imageUrls.isEmpty())){
                if(options.size()!=imageUrls.size()){
                    //  获取 所有的 sku  的第一个属性的下标 用来校检对象
                    final List<Integer> attrIndex = variationIndexs.stream().filter(Objects::nonNull).flatMap(variationIndex -> variationIndex.getIndex().stream().limit(1)).distinct().collect(toList());
                    final List<String> newImages = attrIndex.stream().map(index -> imageUrls.get(index)).collect(toList());
                    skuAttribute.setImagesUrl(newImages);
                }else{
                    skuAttribute.setImagesUrl(imageUrls);
                }
            }
            shopeeSkuAttributeService.saveOrUpdate(skuAttribute);
        }

        for (VariationVM.VariationIndex variationIndex : variationIndexs) {
            final ShopeeProductSkuDTO productSku = fillProductSku(variationIndex.getPrice(), variationIndex.getCurrency(), variationIndex.getSkuCode(), variationIndex.getStock(), param.getProductId(), variationIndex.getId(), variationIndex.getVariationId());
            productSku.setSkuOptionOneIndex(variationIndex.getIndex().get(0));
            productSku.setSkuOptionTowIndex(variationIndex.getIndex().get(1));
            productSku.setDeleted(0);
            saveOrUpdate(productSku);
        }

        updatePriceIntervalAndStock(variations.get(0).getCurrency(),param.getProductId(),variations.stream().mapToInt(VariationVM.Variation::getStock).sum() , variationIndexs.stream().map(VariationVM.VariationIndex::getPrice).collect(toList()));

        return variationIndexs.stream().map(VariationVM.VariationIndex::getId).filter(Objects::nonNull).collect(toList());
    }

    protected ShopeeProductSkuDTO fillProductSku(ShopeeProductSkuDTO productSku,Float price, String currency, String skuCode, Integer stock, Long productId, Long id, Long variationId) {
        productSku.setId(id);
        productSku.setProductId(productId);
        productSku.setPrice(price);
        productSku.setCurrency(currency);
        productSku.setSkuCode(skuCode);
        if (Objects.nonNull(variationId) && !variationId.equals(-1L)) {
            productSku.setShopeeVariationId(variationId);
        }
        if (Objects.isNull(productSku.getSkuCode())) {
            //   13 位的  长度数据
            String skuCode1 = String.valueOf(snowflakeGenerate.nextId() / 1000L);
            productSku.setSkuCode(skuCode1);
        }
        productSku.setStock(stock);

        return productSku;
    }

    protected ShopeeProductSkuDTO fillProductSku(Float price, String currency, String skuCode, Integer stock, Long productId, Long id, Long variationId) {
        final ShopeeProductSkuDTO productSku = new ShopeeProductSkuDTO();
        productSku.setId(id);
        productSku.setProductId(productId);
        productSku.setPrice(price);
        productSku.setCurrency(currency);
        productSku.setSkuCode(skuCode);
        if (Objects.nonNull(variationId) && !variationId.equals(-1L)) {
            productSku.setShopeeVariationId(variationId);
        }
        if (Objects.isNull(productSku.getSkuCode())) {
            //   13 位的  长度数据
            String skuCode1 = String.valueOf(snowflakeGenerate.nextId() / 1000L);
            productSku.setSkuCode(skuCode1);
        }
        productSku.setStock(stock);

        return productSku;
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void copyShopeeProductSku(long productId, long copyProductId, String toCurrency, boolean toTw) {

        /*
         * 取出源商品的SKU项, 更换商品D后保存
         */
        final List<ShopeeProductSkuDTO> oldProductSkus = pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords();
        if (oldProductSkus.size() == 0) {
            log.info("当前站点商品sku可能丢失了,productId={},copyProductId={}", productId, copyProductId);
            return;
        }
        int i = deleteByProduct(copyProductId);

        CurrencyRateResult result = null;
        if (StringUtils.isNotBlank(toCurrency)) {
            result = currencyConvert.currencyConvert(oldProductSkus.get(0).getCurrency(), toCurrency);
        }

        for (ShopeeProductSkuDTO productSku : oldProductSkus) {
            productSku.setProductId(copyProductId);

            checkPrice(toCurrency, result, productSku);
        }
        log.info("保存sku信息,productId={},copyProductId={},oldProductSkus={}", productId, copyProductId, JSON.toJSONString(oldProductSkus));
        batchSave(oldProductSkus);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void copyShopeeProductSku(ShopeeProductCopyDTO shopeeProductCopyDTO) {
        final Long productId = shopeeProductCopyDTO.getProductId();
        String toCurrency = shopeeProductCopyDTO.getToCurrency();
        final Long copyProductId = shopeeProductCopyDTO.getCopyOfProductId();
        /*
         * 取出源商品的SKU项, 更换商品D后保存
         */
        final List<ShopeeProductSkuDTO> oldProductSkus = pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords();
        if (oldProductSkus.size() == 0) {
            log.info("当前站点商品sku可能丢失了,productId={},copyProductId={}", productId, copyProductId);
            return;
        }
        int i = deleteByProduct(copyProductId);

        CurrencyRateResult result = null;
        if (StringUtils.isNotBlank(toCurrency)) {
            result = currencyConvert.currencyConvert(oldProductSkus.get(0).getCurrency(), toCurrency);
        }

        for (ShopeeProductSkuDTO productSku : oldProductSkus) {
            productSku.setProductId(copyProductId);
            checkPrice(toCurrency, result, productSku);
        }
        log.info("保存sku信息,productId={},copyProductId={},oldProductSkus={}", productId, copyProductId, JSON.toJSONString(oldProductSkus));
        batchSave(oldProductSkus);
    }

    @Override
    public   void checkPrice(String toCurrency, CurrencyRateResult result, ShopeeProductSkuDTO productSku) {
        if (StringUtils.isNotBlank(toCurrency)) {

            log.debug("[原价格] {}:{}", productSku.getPrice(), productSku.getCurrency());

            productSku.setCurrency(toCurrency);
            final float price = result.getRate().multiply(new BigDecimal(productSku.getPrice())).floatValue();
            productSku.setPrice(price);

            log.debug("[新价格] {}:{}", productSku.getPrice(), productSku.getCurrency());
        }
    }

    @Override
    public int deleteByProduct(long productId) {
        return repository.delete(new QueryWrapper<>(new ShopeeProductSku().setProductId(productId)));
    }

    @Override
    public Optional<ShopeeProductSkuDTO> findByVariationId(Long variationId) {
        return Optional.ofNullable(repository.findByVariationId(variationId)).map(mapper::toDto);
    }

    @Override
    public List<ShopeeProductSkuDTO> listByVariationIds(List<Long> variationIds) {
        if (CommonUtils.isBlank(variationIds)) {
            return new ArrayList<>();
        }
        return mapper.toDto(repository.listByVariationIds(variationIds));
    }

    @Override
    public List<ShopeeProductSku> findSkuListByloginId(String loginId, String skuCode) {
        return repository.findSkuListByloginId(loginId, skuCode);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<VariationMV_V2> selectVariationMV_V2ByProductIds(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ShopeeProductSkuDTO> shopeeProductSkuDTOS = mapper.toDto(repository.selectByProductIds(productIds));

        if (!shopeeProductSkuDTOS.isEmpty()) {
            final List<ShopeeSkuAttributeDTO> shopeeSkuAttributeDTOS = shopeeSkuAttributeService.selectByProductIds(productIds);
//            Map<Long, List<ShopeeSkuAttributeDTO>> shopeeSkuAttributeDTOMap = shopeeSkuAttributeDTOS.stream().collect(groupingBy(ShopeeSkuAttributeDTO::getProductId));
            return shopeeProductSkuDTOS.stream()
                    .map((ShopeeProductSkuDTO shopeeProductSkuDTO) -> shopeeProductSkuDTOTransformVariationMV_V2(shopeeProductSkuDTO, shopeeSkuAttributeDTOS))
                    .collect(toList());
        }
        return Collections.emptyList();

    }

    @Override
    public List<ShopeeProductSkuDTO> listByProductIds(List<Long> productIds) {
        if (CommonUtils.isBlank(productIds)) {
            return new ArrayList<>();
        }
        return mapper.toDto(repository.selectByProductIds(productIds));
    }

    @Override
    public List<ShopeeProductSku> selectImagesBySkuCodes(Collection<String> skuCode, Long itemId) {
        return repository.selectImagesBySkuCodes(skuCode,itemId);
    }

    @Override
    public void checkPublishParam(Long productId) {
        final List<ShopeeProductSkuDTO> productSkus = this.pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords();
        for (ShopeeProductSkuDTO sku : productSkus) {
            final Integer stock = sku.getStock();
            if(Objects.isNull(stock)|| stock  < 0 || stock > 999999){
              throw  new IllegalOperationException("sku.stock.more.than.the",true);
            }
            final Float price = sku.getPrice();

            //20200109 因为shopee跨境店铺和本地店铺价格上限不一样，我们目前无法区分店铺类型，顾不判断价格上限
//            if(Objects.isNull(price)|| !(price != 0f && price > -1f)|| price > 1100000.0){
            if(Objects.isNull(price)|| !(price != 0f && price > -1f)){
                throw  new IllegalOperationException("sku.price.more.than.the",true);
            }
        }
    }

    @Override
    public List<ShopeeProductSkuDTO> selectByProductIdAndShopeeVariationIds(Long productId, List<Long> variationIds) {
        List<ShopeeProductSku> shopeeProductSkus = repository.selectList(new QueryWrapper<ShopeeProductSku>().eq("product_id", productId).in("shopee_variation_id", variationIds).eq("deleted",0));
        return mapper.toDto(shopeeProductSkus);
    }

    @Override
    public IPage<ShopeeProductSkuDTO> findSkuListByloginIdAndSkuCode(Page page, String skuCode) {
        final IPage<ShopeeProductSku> skuListByloginIdAndSkuCode = repository.findSkuListByloginIdAndSkuCode(page, skuCode);
        if (CollectionUtils.isEmpty(skuListByloginIdAndSkuCode.getRecords())) {
            return new Page().setRecords(Collections.EMPTY_LIST).setCurrent(page.getCurrent());
        }
        return toDTO(skuListByloginIdAndSkuCode);
    }

    @Override
    public void clearInvalidVariationId(List<Long> invalidVariationIds) {
        if (CommonUtils.isNotBlank(invalidVariationIds)) {
            return;
        }
        repository.clearInvalidVariationId(invalidVariationIds);
    }

    @Override
    public List<ShopeeProductSkuDTO> getSkuListByProductId(Long productId) {
        if (CommonUtils.isBlank(productId)) {
            return new ArrayList<>();
        }
        return mapper.toDto(repository.selectList(new QueryWrapper<ShopeeProductSku>()
                .eq("product_id", productId)));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean replace(ShopeeProductSkuDTO product) {
        int size = 0;
        boolean isSave = true ;
        if(Objects.nonNull(product.getShopeeVariationId())){
            final Optional<ShopeeProductSkuDTO> byItemIdAndShopId = this.findByVariationId(product.getShopeeVariationId());
            if (byItemIdAndShopId.isPresent()) {
                isSave = false;
                product.setId(byItemIdAndShopId.get().getId());
                size = repository.updateExcludeOriginalPrice(mapper.toEntity(product));
            }
        }
        if(isSave){
            size = repository.saveExcludeOriginalPrice(mapper.toEntity(product));
        }
        final boolean isOk = SqlHelper.retBool(size);

        return isOk;
    }

    @Override
    public boolean replaceAll(List<ShopeeProductSkuDTO> list) {
        /* List<ShopeeProductSkuDTO> updateList = new ArrayList<>();*/
        List<ShopeeProductSkuDTO> updateDTOList = new ArrayList<>();
        List<ShopeeProductSkuDTO> saveList = new ArrayList<>();
        List<Long> list1 = new ArrayList<>();
        for (ShopeeProductSkuDTO product : list) {
            if(Objects.nonNull(product.getShopeeVariationId())){
                list1.add(product.getShopeeVariationId());
            }
        }
        List<ShopeeProductSku> itemList = new ArrayList<>();
        if (list1.size() > 0){
            itemList = repository.selectByItemIds(list1);
        }


        for (ShopeeProductSkuDTO product : list) {
            boolean isSave = true ;
            if(Objects.nonNull(product.getShopeeVariationId())){

                if (itemList.size() > 0){
                    for (ShopeeProductSku productSku : itemList) {
                        if (productSku.getShopeeVariationId().longValue() == product.getShopeeVariationId()){
                            isSave = false;
                            product.setId(productSku.getId());
                            updateDTOList.add(product);
                        }
                    }

                }
            }
            if(isSave){
                saveList.add(product);
            }
        }
        if (!updateDTOList.isEmpty() && updateDTOList.size() > 0)  batchUpdate(updateDTOList);
        if (saveList.size() > 0)  batchSave(saveList);

        return true;
    }

    @Override
    public boolean deleteByVariationId(long productId, List<Long> variationIds) {
        if (CommonUtils.isBlank(productId) || CommonUtils.isBlank(variationIds)) {
            return false;
        }
        return repository.deleteByVariationIdAndProductId(productId, variationIds);
    }

    @Override
    public boolean deleteAllNullVariationIdByProduct(long productId) {
        return repository.delete(new QueryWrapper<>(new ShopeeProductSku().setProductId(productId)).isNull("shopee_variation_id")) > 0;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public VariationMV_V2 shopeeProductSkuDTOTransformVariationMV_V2(ShopeeProductSkuDTO productSku, List<ShopeeSkuAttributeDTO> shopeeSkuAttributeDTOS) {
        final VariationMV_V2 variation = new VariationMV_V2();
        variation.setProductId(productSku.getProductId());
        variation.setCurrency(productSku.getCurrency());
        variation.setPrice(productSku.getPrice());
        variation.setOriginalPrice(productSku.getOriginalPrice());
        variation.setSkuCode(productSku.getSkuCode() == null ? "" : productSku.getSkuCode());
        variation.setStock(productSku.getStock() == null ? 0 : productSku.getStock());
        variation.setShopeeVariationId(productSku.getShopeeVariationId());
        //是否为json 如果是取出specId的value存入
        if (FeatrueUtil.isJsonString(productSku.getFeature())) {
            variation.setSpecId(productSku.getFeature() == null ? "" : JSON.parseObject(productSku.getFeature()).getString("specId"));
        }
        final ArrayList<ShopeeSkuAttributeDTO> objects = new ArrayList<>(2);
        for (ShopeeSkuAttributeDTO shopeeSkuAttributeDTO : shopeeSkuAttributeDTOS) {
            if(Objects.equals(shopeeSkuAttributeDTO.getProductId(),productSku.getProductId())){
                objects.add(shopeeSkuAttributeDTO);
            };
        }
        variation.setName(generateSkuName(productSku,objects));
        return variation;
    }

    private String generateSkuName(ShopeeProductSkuDTO productSku, List<ShopeeSkuAttributeDTO> shopeeSkuAttributeDTOS) {

        if (null == shopeeSkuAttributeDTOS || shopeeSkuAttributeDTOS.isEmpty()) {
            return null;
        }

        if (shopeeSkuAttributeDTOS.size() == 1) {
            if (productSku.getSkuOptionOneIndex() != null && shopeeSkuAttributeDTOS.get(0).getOptions().size() > productSku.getSkuOptionOneIndex()) {
                return shopeeSkuAttributeDTOS.get(0).getOptions().get(productSku.getSkuOptionOneIndex());
            }
        } else if (shopeeSkuAttributeDTOS.size() == 2) {
            if (productSku.getSkuOptionOneIndex() != null && productSku.getSkuOptionTowIndex() != null && shopeeSkuAttributeDTOS.get(0).getOptions().size() > productSku.getSkuOptionOneIndex() && shopeeSkuAttributeDTOS.get(1).getOptions().size() > productSku.getSkuOptionTowIndex()) {
                return shopeeSkuAttributeDTOS.get(0).getOptions().get(productSku.getSkuOptionOneIndex()) + "—" + shopeeSkuAttributeDTOS.get(1).getOptions().get(productSku.getSkuOptionTowIndex());
            }
        }
        return null;

    }

}
