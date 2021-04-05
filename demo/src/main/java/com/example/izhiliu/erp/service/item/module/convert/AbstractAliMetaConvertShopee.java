package com.izhiliu.erp.service.item.module.convert;

import com.alibaba.fastjson.JSON;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;
import com.izhiliu.erp.service.item.module.basic.BaseModelConvert;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductSKUInfoPlus;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductSKUAttrInfoPlus;

import com.izhiliu.erp.util.FeatrueUtil;
import com.izhiliu.erp.util.SnowflakeGenerate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author Seriel
 * @create 2019-08-08 15:37
 **/
public abstract class AbstractAliMetaConvertShopee extends BaseModelConvert {

    @Autowired
    private SnowflakeGenerate snowflakeGenerate;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void map(ProductMetaDataDTO data, String loginId, MetaDataObject.CollectController collectController) {
        try {
            final AlibabaProductProductInfoPlus productInfo = JSON.parseObject(data.getJson(), AlibabaProductProductInfoPlus.class);

            final Long weight = getWeight(productInfo);
            final ShopeeProductDTO product = shopeeProductService.save(fillAndSaveShopeeProduct(data, productInfo, weight,collectController));
            final Long productId = product.getId();



            /*
             * TODO 类目属性存着无意义 留给用户自己绑定
             *
             * SKU:
             *  SKU数据结构为一个数组打包带走, 单SKU的情况 attributes 数组只有一个元素, 双SKU则有两个, 依据这个判断SKU个数
             */
            int variationTier = 0;
            String skuAttributeName = "Variation";
            if (productInfo.getSkuInfos() == null || productInfo.getSkuInfos().length == 0||productInfo.getSkuInfos()[0].getAttributes().length == 0) {
                fillAndSaveSingleProduct(productInfo, product, variationTier);
                return;
            }
            final AlibabaProductProductSKUInfoPlus[] skuInfos = productInfo.getSkuInfos();
            final int sum = Stream.of(skuInfos).map(AlibabaProductProductSKUInfoPlus::getSoldQuantity).filter(Objects::nonNull).mapToInt(value -> value).sum();

            final List<ShopeeProductSkuDTO> productSkus ;
            if (skuInfos[0].getAttributes().length == 1) {
                variationTier = 1;
                oneSku(productId, skuAttributeName, skuInfos, product.getCurrency());
            } else {
                variationTier = 2;
                /*
                 * 挑出属性 AND 属性值
                 */
                final AlibabaProductSKUAttrInfoPlus attribute = skuInfos[0].getAttributes()[0];
                boolean isImage = Objects.nonNull(attribute.getSkuImageUrl()) ;
                List<String> imagesUrl = Collections.EMPTY_LIST;
                String oneName = skuInfos[0].getAttributes()[0].getAttributeDisplayName();
                String towName = skuInfos[0].getAttributes()[1].getAttributeDisplayName();

                log.info("[oneName]: {}", oneName);
                log.info("[towName]: {}", towName);


                /*
                 * 拿到两个SKU属性的可选项数量
                 */
                final Map<String, List<AlibabaProductSKUAttrInfoPlus>> groupByName = Arrays.asList(skuInfos)
                        .parallelStream()
                        .filter(Objects::nonNull)
                        .map(alibabaProductProductSKUInfo -> Arrays.asList(alibabaProductProductSKUInfo.getAttributes()))
                        .flatMap(alibabaProductSKUAttrInfos -> alibabaProductSKUAttrInfos.stream())
                        .distinct()
                        .sorted(Comparator.comparing(value -> value.getIndex()))
                        .collect(Collectors.groupingBy(AlibabaProductSKUAttrInfoPlus::getAttributeDisplayName));
                final List<String> oneOptions = groupByName.get(oneName).stream().map(AlibabaProductSKUAttrInfoPlus::getAttributeValue).distinct().collect(toList());
                final List<String> towOptions = groupByName.get(towName).stream().map(AlibabaProductSKUAttrInfoPlus::getAttributeValue).distinct().collect(toList());

                if(isImage){
                    imagesUrl = imageSorted(oneName, groupByName, oneOptions);
                }

                log.info("[oneOptions]: [{}] \n [oneSize]: [{}]", String.join(",", oneOptions),oneOptions.size());
                log.info("[towOptions]: [{}] \n [towSize]: [{}]", String.join(",", towOptions),towOptions.size());



                /*
                 * 存一下商品SKU(local) 和所有的SKU属性(1688)
                 */
                productSkus = new ArrayList<>(skuInfos.length);

                for (AlibabaProductProductSKUInfoPlus skuInfo : skuInfos) {
                    final ShopeeProductSkuDTO productSku = getProductSku(getPrice(skuInfo),productId);

                    productSku.setImage(getImage(skuInfo.getAttributes()[0]));
                    productSku.setCurrency(product.getCurrency());
                    if(Objects.nonNull(skuInfo.getOriginalPrice())){
                        productSku.setOriginalPrice(skuInfo.getOriginalPrice().floatValue());
                    }
                    if(Objects.nonNull(skuInfo.getStock())){
                        //shopee库存最高限制999998
                        productSku.setStock(CommonUtils.getMaxStock(skuInfo.getStock()));
                    }
//                    productSku.setStock(skuInfo.getAmountOnSale());
                    //specId以Json形式存入到拓展字段
                    productSku.setFeature(FeatrueUtil.addFeature(productSku.getFeature(), "specId", skuInfo.getSpecId()));

                    final String oneAttributeValue = skuInfo.getAttributes()[0].getAttributeValue();
                    fillAttribute(oneAttributeValue, oneOptions, oneIndex -> productSku.setSkuOptionOneIndex(oneIndex));
                    final String twoAttributeValue = skuInfo.getAttributes()[1].getAttributeValue();
                    fillAttribute(twoAttributeValue, towOptions, towIndex -> productSku.setSkuOptionTowIndex(towIndex));
                    String skuCode = ConvertUtils.generate(oneOptions.get(productSku.getSkuOptionOneIndex()),towOptions.get(productSku.getSkuOptionTowIndex()));
                    productSku.setSkuCode(skuCode);
                    productSkus.add(productSku);
                }

                productSkus.stream().sorted(Comparator.comparing(ShopeeProductSkuDTO::getSkuOptionOneIndex)).forEach(productSku -> {
                    shopeeProductSkuService.save(productSku);
                });


                saveSkuAttribute(productId, oneName, oneOptions, imagesUrl);
                saveSkuAttribute(productId, towName, towOptions, null);
            }
            updateVariationTier(variationTier, productId,sum);
        } catch (Exception e) {
            log.error("[1688转虾皮异常]", e);
        }
    }

    List<String> imageSorted(String oneName, Map<String, List<AlibabaProductSKUAttrInfoPlus>> groupByName, List<String> oneOptions) {
        List<String> imagesUrl;
        final Map<String, List<AlibabaProductSKUAttrInfoPlus>> collect = groupByName.get(oneName).stream().collect(Collectors.groupingBy(AlibabaProductSKUAttrInfoPlus::getAttributeValue));
        final List<AlibabaProductSKUAttrInfoPlus> collect1 = collect.values().stream().map(alibabaProductSKUAttrInfoPluses -> alibabaProductSKUAttrInfoPluses.iterator().next()).collect(toList());
        imagesUrl =  collect1.stream().sorted(Comparator.comparingInt(value -> oneOptions.indexOf(value.getAttributeValue()))).map(this::getImage).limit(35).collect(toList());
        return imagesUrl;
    }

    void fillAndSaveSingleProduct(AlibabaProductProductInfoPlus productInfo, ShopeeProductDTO product, int variationTier) {
        final ShopeeProductSkuDTO productSku = getProductSku(ShopeeUtil.outputPrice(product.getPrice(), product.getCurrency()), product.getId());
        final boolean isExist = Objects.nonNull(productInfo.getSkuInfos()) && productInfo.getSkuInfos().length > 0;
        if(isExist){
            //  todo   拼多多 单品也自带了一个 sku
            final AlibabaProductProductSKUInfoPlus skuInfo = productInfo.getSkuInfos()[0];
            productSku.setPrice(skuInfo.getPrice().floatValue());
            if(Objects.nonNull(skuInfo.getStock())){
                productSku.setStock(CommonUtils.getMaxStock(skuInfo.getStock()));
            }
        }else{
            if(Objects.nonNull(productInfo.getStock())){
                productSku.setStock(CommonUtils.getMaxStock(productInfo.getStock()));
            }
        }

        productSku.setCurrency(product.getCurrency());
        //        productSku.setSkuCode(snowflakeGenerate.skuCode().toString());
        if (null != productInfo.getImage() && productInfo.getImage().getImages().length > 0) {
            productSku.setImage(productInfo.getImage().getImages()[0]);
        }
        productSku.setFeature(FeatrueUtil.addFeature(productSku.getFeature(), "specId", Long.toString(productInfo.getProductID())));
        shopeeProductSkuService.save(productSku);
        updateVariationTier(variationTier, product.getId(),isExist?productInfo.getSkuInfos()[0].getSoldQuantity():0);
    }

    private ShopeeProductDTO fillAndSaveShopeeProduct(ProductMetaDataDTO data, AlibabaProductProductInfoPlus productInfo, Long weight, MetaDataObject.CollectController collectController) {
        final ShopeeProductDTO product = new ShopeeProductDTO();
        product.setType(ShopeeProduct.Type.PLATFORM.code);
        product.setPlatformId(PlatformEnum.SHOPEE.getCode().longValue());

        product.setLoginId(data.getLoginId());
        if(Objects.nonNull(collectController.getPricing())){
            product.setFeature(FeatrueUtil.addFeature(null,"pricingId",collectController.getPricing().toString()));
        }
        product.setMetaDataId(data.getId());
        product.setCollect(data.getPlatform());
        product.setCollectUrl(data.getUrl());
        product.setSkuCode(Objects.nonNull(data.getProductId())?data.getProductId().toString():null);

        product.setName(data.getName());
        product.setDescription(data.getDescription());
        //默认主图,兼容处理,取前9张
        if (null == data.getMainImages() || data.getMainImages().size() < 1) {
            if (null != data.getImages() && data.getImages().size() > 9) {
                product.setImages(data.getImages().subList(0, 9));
            } else {
                product.setImages(data.getImages());
            }
        } else {
            product.setImages(data.getMainImages());
        }
        final List<String> descImages = data.getDescImages();
        ConvertUtils.fillImage(descImages, product);
        product.setPrice(data.getPrice());
        product.setOriginalPrice(data.getPrice());
        product.setCurrency(data.getCurrency());

        product.setWeight(weight);
        product.setCrossBorder(productInfo.getCrossBorderOffer());
        return product;
    }

    /**
     *
     * @param sourceValue
     * @param options
     * @param integerConsumer
     */
    public static void fillAttribute(String sourceValue,final List<String> options, Consumer<Integer> integerConsumer){
        for (int oneIndex = 0; oneIndex < options.size(); oneIndex++) {
            if (Objects.equals(sourceValue, options.get(oneIndex))){
                integerConsumer.accept(oneIndex);
            }
        }
    }


    public float getPrice(AlibabaProductProductSKUInfoPlus skuInfo) {
//        if(collectController.isPricingStrategies()){
//            pricingStrategiesService.pricing(collectController.getPricingStrategiesDto(),
//                    skuInfo.getPrice() == null ? skuInfo.getConsignPrice().floatValue() : skuInfo.getPrice().floatValue(),
//                    collectController.getWeight(),
//                    collectController.getCurrencyRateResult());
//        }

        return skuInfo.getPrice() == null ? skuInfo.getConsignPrice().floatValue() : skuInfo.getPrice().floatValue();
    }

    public abstract String getImage(AlibabaProductSKUAttrInfoPlus attribute);

    private void oneSku(Long productId, String skuAttributeName, AlibabaProductProductSKUInfoPlus[] skuInfos, String currency) {
        int i = 0;
        final List<String> options = new ArrayList<>(skuInfos.length);
        final List<String> images = new ArrayList<>(skuInfos.length);
        for (AlibabaProductProductSKUInfoPlus skuInfo : skuInfos) {
            final AlibabaProductSKUAttrInfoPlus attribute = skuInfo.getAttributes()[0];
            if (i == 0) {
                skuAttributeName = attribute.getAttributeDisplayName();
            }

            final ShopeeProductSkuDTO productSku = getProductSku(getPrice(skuInfo), productId);
            String skuCode = ConvertUtils.generate(attribute.getAttributeValue());
            productSku.setSkuCode(skuCode);
            if(Objects.nonNull(skuInfo.getOriginalPrice())){
                productSku.setOriginalPrice(skuInfo.getOriginalPrice().floatValue());
            }
            if(Objects.nonNull(skuInfo.getStock())){
                productSku.setStock(CommonUtils.getMaxStock(skuInfo.getStock()));
            }
//                    productSku.s
            productSku.setFeature(FeatrueUtil.addFeature(productSku.getFeature(), "specId", skuInfo.getSpecId()));

            final String image = getImage(attribute);
            productSku.setImage(image);
            productSku.setCurrency(currency);
            productSku.setSkuOptionOneIndex(i++);
//            productSku.setStock(skuInfo.getAmountOnSale());
            shopeeProductSkuService.save(productSku);
            images.add(image);

            options.add(attribute.getAttributeValue());
        }

        saveSkuAttribute(productId, skuAttributeName, options,images.stream().limit(30).collect(toList()));
    }

    private void saveSkuAttribute(Long productId, String skuAttributeName, List<String> options, List<String> imagesUrl) {
        final ShopeeSkuAttributeDTO skuAttribute = new ShopeeSkuAttributeDTO();
        skuAttribute.setProductId(productId);
        skuAttribute.setName(skuAttributeName);
        skuAttribute.setImagesUrl(imagesUrl);
        skuAttribute.setOptions(options);
        shopeeSkuAttributeService.save(skuAttribute);
    }

    private long getWeight(AlibabaProductProductInfoPlus productInfo) {
        if (productInfo.getShippingInfo() == null || productInfo.getShippingInfo().getUnitWeight() == null) {
            return 0L;
        }
        return new Double(productInfo.getShippingInfo().getUnitWeight() * 1000).longValue();
    }

    private ShopeeProductSkuDTO getProductSku(Float price, Long productId) {
        final ShopeeProductSkuDTO productSku = new ShopeeProductSkuDTO();
        productSku.setProductId(productId);
        productSku.setPrice(price);
        productSku.setSkuCode(snowflakeGenerate.skuCode().toString());
        return productSku;
    }
}