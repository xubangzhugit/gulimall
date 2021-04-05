package com.izhiliu.erp.service.item.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import com.izhiliu.erp.repository.item.ShopeeProductSkuRepository;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.ShopeeSkuAttributeService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;
import com.izhiliu.erp.util.FeatrueUtil;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.errors.DataNotFoundException;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.item.vm.VariationVM;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.izhiliu.erp.service.item.impl.ShopeeProductSkuServiceImpl.*;
import static java.util.stream.Collectors.toList;

/**
 *  统一单规格与双规格的存储风格
 * @author Seriel
 * @create 2019-09-09 14:23
 **/
@Slf4j
@Service
public class CoverShopeeProductSkuService  {

    @Resource
    ShopeeProductSkuServiceImpl shopeeProductSkuService;
    @Resource
    ShopeeProductSkuRepository shopeeProductSkuRepository;

    @Resource
    private ShopeeProductService shopeeProductService;

    @Resource
    private ShopeeSkuAttributeService shopeeSkuAttributeService;

    @Resource
    private CoverShopeeProductSkuService coverShopeeProductSkuService;


    /**
     *      根据 变种参数 覆盖产品里面的数据
     * @param param
     * @param product
     */
    @Transactional(rollbackFor = Exception.class)
    public void coveringProductsAccordingToVariants(VariationVM param, ShopeeProductDTO product) {
        shopeeSkuAttributeService.deleteByProduct(param.getProductId());
        final List<ShopeeProductSkuDTO> list = shopeeProductSkuService.pageByProduct(param.getProductId(), new Page<>(0, Integer.MAX_VALUE)).getRecords();

        final List<VariationVM.Variation> variations = param.getVariations();

        final List<Long> ids;
        if (Objects.equals(ShopeeProduct.VariationTier.ZERO.val,param.getVariationTier())) {
            ids = shopeeProductSkuService.oneOrNoSkuAttribute(param, variations,product.getCurrency());
        } else {
            //如果是店铺在线商品并且创建成功的sku更新折扣 price = discount, 非在线商品设置折扣price=originalPrice
             ids = this.towSkuAttribute(param, variations, product.getCurrency());
        }
        shopeeProductSkuService.clearDirtyData(list, ids);
        shopeeProductSkuService.refreshVariationTier(param, product);
    }

    /**
     * 索引不能越界
     *
     * @param indexs
     * @param vm
     * @return
     */
    private boolean checkIndex(List<Integer> indexs, VariationVM vm) {
        if (indexs == null) {
            return true;
        }
        for (int i = 0; i < indexs.size(); i++) {
            if(indexs.get(i) < 0 || indexs.get(i) > vm.getVariations().get(i).getOptions().size() - 1){
                return  true;
            }
        }
        return false;
    }


    /**
     * 参数校验
     */
    public ShopeeProductDTO checkParam(VariationVM param,  Optional<ShopeeProductDTO> productSupplier) {
        if (param.getProductId() == null) {
            throw new DataNotFoundException("data.not.found.exception.product.can.not.null", true);
        }
        final ShopeeProductDTO product = productSupplier
                .orElseThrow(() ->
                        new DataNotFoundException("data.not.found.exception.product.not.found", new String[]{"product id : " + param.getProductId()}));

        //todo 用户权限校验逻辑
        /*
         * 已发布的商品 不能降级
         */
        if (ShopeeProduct.Type.SHOP.code.equals(product.getType())
                &&product.getShopeeItemId() != null
                && product.getShopeeItemId() != 0) {
            if (ShopeeProduct.VariationTier.TWO.val.equals(product.getVariationTier())
                    &&!product.getVariationTier().equals(param.getVariationTier())) {
                throw new IllegalOperationException("sku.already.publish.not.down", true);
            }
        }
        if (!Objects.equals(ShopeeProduct.VariationTier.ZERO.val,param.getVariationTier())) {
            if (CollectionUtils.isEmpty(param.getVariations())) {
                throw new IllegalOperationException("sku.attribute.not.null", true);
            }
            for (VariationVM.Variation variation : param.getVariations()) {
                if (CollectionUtils.isEmpty(variation.getOptions())) {
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
            if (param.getVariationIndexs().size() > ShopeeProductSkuServiceImpl.SKU_COMBINATION_COUNT || param.getVariations().stream().map(variationIndex -> variationIndex.getOptions().size()).reduce((integer, integer2) -> integer * integer2).get() > ShopeeProductSkuServiceImpl.SKU_COMBINATION_COUNT) {
                throw new IllegalOperationException("sku.size", new String[]{Integer.toString(ShopeeProductSkuServiceImpl.SKU_COMBINATION_COUNT)});
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
                    //throw new IllegalOperationException("sku.option.size", new String[]{Integer.toString(SKU_OPTION_COUNT)});
                    throw new BadRequestAlertException("sku option illegal", "BatchEditProduct", "sku.option.size.twenty");
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
        }
        return product;
    }

    /**
     * 双SKU属性
     */
    public List<Long> towSkuAttribute(VariationVM param, List<VariationVM.Variation> variations, String currency) {
//        StopWatch stopWatch =new StopWatch("单规格");
//        stopWatch.start("变体属性");
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
            final  List<String>  collectImageUrls = imageUrls.stream().map(image -> {
                    if (image.startsWith("//")) {
                        return "https:" + image;
                    }
                    return image;
                }).collect(Collectors.toList());
                if(options.size()!=imageUrls.size()){
                    //  获取 所有的 sku  的第一个属性的下标 用来校检对象
                    final List<Integer> attrIndex = variationIndexs.stream().filter(Objects::nonNull).flatMap(variationIndex -> variationIndex.getIndex().stream().limit(1)).distinct().collect(toList());
                    final List<String> newImages = attrIndex.stream().map(index -> collectImageUrls.get(index)).collect(toList());
                    skuAttribute.setImagesUrl(newImages);
                }else{
                    skuAttribute.setImagesUrl(collectImageUrls);
                }
            }
            shopeeSkuAttributeService.saveOrUpdate(skuAttribute);
        }
//        stopWatch.stop();
//        stopWatch.start("获取sku");
        //  将传进来的 id 进行数据库的校检  是否存在
        final List<Long> oldSkuIds = variationIndexs.stream().map(VariationVM.VariationIndex::getId).collect(toList());
         List<Long> originalSkuIds =Collections.emptyList();
        if(!CollectionUtils.isEmpty(oldSkuIds)){
            originalSkuIds =   shopeeProductSkuService.pageByProduct(param.getProductId(),new Page(0,Integer.MAX_VALUE)).getRecords().stream().map(ShopeeProductSkuDTO::getId).collect(toList());
        }
        List<ShopeeProductSkuDTO> save =new ArrayList<>();
        List<ShopeeProductSkuDTO> update =new ArrayList<>();
//        stopWatch.stop();
//        stopWatch.start("处理 sku");
        for (VariationVM.VariationIndex variationIndex : variationIndexs) {
            final ShopeeProductSkuDTO productSku = shopeeProductSkuService.fillProductSku(variationIndex.getPrice(), currency, variationIndex.getSkuCode(), variationIndex.getStock(), param.getProductId(), variationIndex.getId(), variationIndex.getVariationId());
            productSku.setDiscount(variationIndex.getDiscount());
            productSku.setDiscountId(variationIndex.getDiscountId());
            productSku.setSkuOptionOneIndex(variationIndex.getIndex().get(0));
            productSku.setOriginalPrice(variationIndex.getOriginalPrice());
            final boolean isTwoIndex = variationIndex.getIndex().size() > 1;
            if(isTwoIndex){
                productSku.setSkuOptionTowIndex(variationIndex.getIndex().get(1));
            }
            productSku.setDeleted(0);
           if(Objects.nonNull(variationIndex.getId())){
               if (originalSkuIds.contains(variationIndex.getId())) {
                   update.add(productSku);
                continue;
               }
           }
            save.add(productSku);
        }
//        stopWatch.stop();
//        stopWatch.start("处理 sku  update ");
        shopeeProductSkuService.batchUpdatePlus(update,update.size(),(myProductSku) -> new UpdateWrapper(){{
            set("sku_option_tow_index",myProductSku.getSkuOptionTowIndex());
            eq("id",myProductSku.getId());
        }});
//        stopWatch.stop();
//        stopWatch.start("处理 sku  update ");
        if(!CollectionUtils.isEmpty(save)){
            shopeeProductSkuService.batchSave(save);
        }
//        stopWatch.stop();
//        stopWatch.start("处理 商品最高最低价格   ");
        shopeeProductSkuService.updatePriceIntervalAndStock(currency,param.getProductId(), variationIndexs.stream().mapToInt(VariationVM.VariationIndex::getStock).sum(),  variationIndexs.stream().map(VariationVM.VariationIndex::getPrice).collect(toList()));
//        stopWatch.stop();
//        System.out.println(stopWatch.prettyPrint());
        return variationIndexs.stream().map(VariationVM.VariationIndex::getId).filter(Objects::nonNull).collect(toList());
    }


    public void coverTheProduct(VariationVM param) {
        final ShopeeProductDTO product = checkParam(
                param,
                shopeeProductService.find(param.getProductId())
        );
        coverShopeeProductSkuService.coveringProductsAccordingToVariants(param, product);
        // 所有保存只保存到表，不push 到shopee,push 由/api/shop-products/push/v2 来决定
//        shopeeProductSkuService.updateToShopee(product);
    }

    public void coverTheProductForBatch(VariationVM param) {
        final ShopeeProductDTO product = checkParam(
                param,
                shopeeProductService.find(param.getProductId())
        );
        coverShopeeProductSkuService.coveringProductsAccordingToVariants(param, product);
        // 所有保存只保存到表，不push 到shopee,push 由/api/shop-products/push/v2 来决定
//        shopeeProductSkuService.updateToShopee(product);
    }


    public VariationVM variationByProduct(long productId, Boolean isActualStock) {
        /*
         * 获取产品、产品SKU、SKU属性
         */
        final Optional<ShopeeProductDTO> productExist = shopeeProductService.find(productId);
        if (!productExist.isPresent()) {
            return null;
        }
        final ShopeeProductDTO product = productExist.get();

        boolean isShopeeItem = Objects.nonNull(product.getShopeeItemId());
        /*
         * 同步不展示,避免看到脏数据
         */
        if (product.getStatus().equals(LocalProductStatus.IN_PULL)) {
            return null;
        }

        final VariationVM variationVo;
        try {
            List<ShopeeProductSku> productSkus = ShopeeProductSkuServiceImpl.sorted(
                    shopeeProductSkuService.getRepository().pageByProductId(new Page(0, Integer.MAX_VALUE), productId).getRecords().stream()
                    ,product.getVariationTier())
                 .collect(toList());
            final List<ShopeeSkuAttributeDTO> skuAttributes = shopeeSkuAttributeService.pageByProduct(productId, new Page(0, Integer.MAX_VALUE)).getRecords();

            variationVo = new VariationVM();
            variationVo.setVariationTier(product.getVariationTier());
            variationVo.setProductId(productId);

            if (ShopeeProduct.VariationTier.ZERO.val.equals(product.getVariationTier())) {
                final List<VariationVM.Variation> variations = getZeroVariations(product, isShopeeItem, variationVo, productSkus, skuAttributes);
                variationVo.setVariations(variations);
            } else {
                final List<VariationVM.Variation> variations = getFillAttrVariations(skuAttributes);
                variationVo.setVariations(variations);
                final List<VariationVM.VariationIndex> variationIndices = getOneOrTwoVariationIndices(productSkus);
                variationIndices.forEach(variationIndex -> {
                    if(isActualStock){
                        variationIndex.setDefaultStock(100);
                    }
                    if (isShopeeItem) {
                        if (!variationIndex.getPrice().equals(variationIndex.getOriginalPrice())) {
                            variationVo.setDiscount(true);
                        }
                    }
                });
                variationVo.setVariationIndexs(variationIndices);
            }


        } catch (Exception e) {
            log.error("[装配SKU异常] : {}", e);
            return null;
        }
        return variationVo;
    }

    private List<VariationVM.Variation> getZeroVariations(ShopeeProductDTO product, boolean isShopeeItem, VariationVM variationVo, List<ShopeeProductSku> productSkus, List<ShopeeSkuAttributeDTO> skuAttributes) {
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
            shopeeProductSkuService.fillVariation(productSku, variation);
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
        return variations;
    }

    private List<VariationVM.VariationIndex> getOneOrTwoVariationIndices( List<ShopeeProductSku> productSkus) {
        // VariationIndex
        final List<VariationVM.VariationIndex> variationIndices = new ArrayList<>();
        for (ShopeeProductSku productSku : productSkus) {
            final VariationVM.VariationIndex variationIndex = new VariationVM.VariationIndex();
            shopeeProductSkuService.fillVariationIndex(productSku, variationIndex);
            //  设定价格 将会使用这俩个价格来判定是否是 折扣价
            if(Objects.nonNull(productSku.getSkuOptionTowIndex())){
                variationIndex.setIndex(Arrays.asList(productSku.getSkuOptionOneIndex(), productSku.getSkuOptionTowIndex()));
            }else{
                variationIndex.setIndex(Arrays.asList(productSku.getSkuOptionOneIndex()));
            }
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
        return variationIndices;
    }

    private List<VariationVM.Variation> getFillAttrVariations(List<ShopeeSkuAttributeDTO> skuAttributes) {
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
        return variations;
    }

    /**
     * 仅更新表数据，不推送到shopee
     * @param param
     */
    public void  coverTheProductForDB(VariationVM param) {
        final ShopeeProductDTO product = checkParam(
                param,
                shopeeProductService.find(param.getProductId())
        );
        coverShopeeProductSkuService.coveringProductsAccordingToVariants(param, product);
    }
}
