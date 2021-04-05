package com.izhiliu.erp.service.item.module.convert;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.product.param.AlibabaProductProductInfo;
import com.alibaba.product.param.AlibabaProductProductSKUInfo;
import com.alibaba.product.param.AlibabaProductSKUAttrInfo;
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
import com.izhiliu.erp.service.module.metadata.dto.PriceRange;
import com.izhiliu.erp.util.FeatrueUtil;
import com.izhiliu.erp.util.SnowflakeGenerate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.izhiliu.erp.service.module.metadata.convert.AlibabaMetaDataConvert.IMAGE_PREFIX;
import static java.util.stream.Collectors.toList;

/**
 * describe: 1688 采集数据模型 转 shopee 平台商品（页面展示为shopee源数据）
 * <p>
 *
 * @author cheng
 * @date 2019/1/25 16:51
 */
@Component
public class AlibabaMetaConvertShopee extends BaseModelConvert {

    @Autowired
    private SnowflakeGenerate snowflakeGenerate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void map(ProductMetaDataDTO data, String loginId, MetaDataObject.CollectController collectController) {
        try {
            final AlibabaProductProductInfoPlus productInfo = JSON.parseObject(data.getJson(), AlibabaProductProductInfoPlus.class);

            final Long weight = getWeight(productInfo);
            final ShopeeProductDTO product = new ShopeeProductDTO();
            product.setType(ShopeeProduct.Type.PLATFORM.code);
            product.setPlatformId(PlatformEnum.SHOPEE.getCode().longValue());

            product.setLoginId(data.getLoginId());
            product.setMetaDataId(data.getId());

            product.setCollect(data.getPlatform());
            product.setCollectUrl(data.getUrl());
            product.setSkuCode(Objects.nonNull(data.getProductId())?data.getProductId().toString():null);

            product.setName(data.getName());
            product.setDescription(data.getDescription());
            //默认主图,兼容处理,取前9张
            if (null == data.getMainImages() || data.getMainImages().size() < 1) {
                if(null != data.getImages() && data.getImages().size()>9){
                    product.setImages(data.getImages().subList(0,9));
                }else {
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
            if(Objects.nonNull(collectController.getPricing())){
                product.setFeature(FeatrueUtil.addFeature(null,"pricingId",collectController.getPricing().toString()));
            }
            product.setWeight(weight);
            product.setCrossBorder(productInfo.getCrossBorderOffer());
            final PriceRange[] priceRange = productInfo.getPriceRange();
            if(Objects.nonNull(priceRange) && priceRange.length>0){
                product.setPriceRange(Arrays.asList(priceRange));
            }
            final Long productId = shopeeProductService.save(product).getId();

            /*
             * TODO 类目属性存着无意义 留给用户自己绑定
             *
             * SKU:
             *  SKU数据结构为一个数组打包带走, 单SKU的情况 attributes 数组只有一个元素, 双SKU则有两个, 依据这个判断SKU个数
             */
            int variationTier = 0;
            String skuAttributeName = "Variation";
            if (productInfo.getSkuInfos() == null || productInfo.getSkuInfos().length == 0) {
                final ShopeeProductSkuDTO productSku = getProductSku(ShopeeUtil.outputPrice(product.getPrice(), product.getCurrency()), productId);
                productSku.setCurrency(product.getCurrency());
                productSku.setSkuCode(snowflakeGenerate.skuCode().toString());
                if (null != productInfo.getImage() && productInfo.getImage().getImages().length > 0) {
                    productSku.setImage(productInfo.getImage().getImages()[0]);
                }
                productSku.setFeature(FeatrueUtil.addFeature(productSku.getFeature(), "specId", Long.toString(productInfo.getProductID())));
                shopeeProductSkuService.save(productSku);
                updateVariationTier(variationTier, productId);
                return;
            }
            final AlibabaProductProductSKUInfoPlus[] skuInfos = productInfo.getSkuInfos();
            if (skuInfos[0].getAttributes().length == 1) {
                variationTier = 1;
                oneSku(productId, skuAttributeName, skuInfos, product.getCurrency());
            } else {
                variationTier = 2;

                /*
                 * 挑出属性 AND 属性值
                 */
                String oneName = skuInfos[0].getAttributes()[0].getAttributeDisplayName();
                String towName = skuInfos[0].getAttributes()[1].getAttributeDisplayName();

                log.info("[oneName]: {}", oneName);
                log.info("[towName]: {}", towName);

                /*
                 * 存一下商品SKU(local) 和所有的SKU属性(1688)
                 */
                final List<AlibabaProductSKUAttrInfo> skuAttributes = Stream.of(skuInfos)
                        .flatMap(skuInfo ->  Stream.of(skuInfo.getAttributes()) ).collect(Collectors.toList());

                /*
                 * 拿到两个SKU属性的可选项数量
                 */
                final Map<String, List<AlibabaProductSKUAttrInfo>> groupByName = skuAttributes.stream().collect(Collectors.groupingBy(AlibabaProductSKUAttrInfo::getAttributeDisplayName));
                final List<String> oneOptions = groupByName.get(oneName).stream().map(AlibabaProductSKUAttrInfo::getAttributeValue).distinct().collect(toList());
                //拿第一层sku属性的图片，例如颜色图
                Map<String,List<AlibabaProductSKUAttrInfo>> groupByValue = skuAttributes.stream().collect(Collectors.groupingByConcurrent(AlibabaProductSKUAttrInfo::getAttributeValue));
                final List<String> imagesUrl = oneOptions.stream().map(option-> getImage(groupByValue.get(option).get(0))).limit(30).collect(toList());

                final List<String> towOptions = groupByName.get(towName).stream().map(AlibabaProductSKUAttrInfo::getAttributeValue).distinct().collect(toList());
                final int oneSize = oneOptions.size();
                final int towSize = towOptions.size();

                log.info("[oneOptions]: {}", String.join(",", oneOptions));
                log.info("[towOptions]: {}", String.join(",", towOptions));

                log.info("[oneSize]: {}", oneOptions.size());
                log.info("[towSize]: {}", towOptions.size());

                final List<ShopeeProductSkuDTO> productSkus = new ArrayList<>();
                for (AlibabaProductProductSKUInfoPlus skuInfo : skuInfos) {
                    final ShopeeProductSkuDTO productSku = new ShopeeProductSkuDTO();
                    productSku.setProductId(productId);
                    productSku.setImage(getImage(skuInfo.getAttributes()[0]));
                    productSku.setCurrency(product.getCurrency());
                    productSku.setPrice(getPrice(skuInfo));
                    productSku.setCurrency(product.getCurrency());
//                    productSku.setStock(skuInfo.getAmountOnSale());
                    if(Objects.nonNull(skuInfo.getStock())){
                        productSku.setStock(CommonUtils.getMaxStock(skuInfo.getStock()));
                    }

                    /*
                     * 填充索引
                     */
                    final String oneAttributeValue = skuInfo.getAttributes()[0].getAttributeValue();
                    AbstractAliMetaConvertShopee.fillAttribute(oneAttributeValue, oneOptions, oneIndex -> productSku.setSkuOptionOneIndex(oneIndex));
                    final String twoAttributeValue = skuInfo.getAttributes()[1].getAttributeValue();
                    AbstractAliMetaConvertShopee. fillAttribute(twoAttributeValue, towOptions, towIndex -> productSku.setSkuOptionTowIndex(towIndex));
                    String skuCode = ConvertUtils.generate(oneOptions.get(productSku.getSkuOptionOneIndex()),towOptions.get(productSku.getSkuOptionTowIndex()));
                    productSku.setSkuCode(skuCode);
                    //specId以Json形式存入到拓展字段
                    productSku.setFeature(FeatrueUtil.addFeature(productSku.getFeature(), "specId", skuInfo.getSpecId()));
                    productSkus.add(productSku);

                }

                productSkus.stream().sorted(Comparator.comparing(ShopeeProductSkuDTO::getSkuOptionOneIndex)).forEach(productSkuInfo -> {
                    shopeeProductSkuService.save(productSkuInfo);
                });

                log.info("[skuSize]: {}", productSkus.size());

                saveSkuAttribute(productId, oneName, oneOptions,imagesUrl);
                saveSkuAttribute(productId, towName, towOptions,null);
            }

            updateVariationTier(variationTier, productId);
        } catch (Exception e) {
            log.error("[1688转虾皮异常]", e);
        }
    }

    public float getPrice(AlibabaProductProductSKUInfo skuInfo) {
        return skuInfo.getPrice() == null ? skuInfo.getConsignPrice().floatValue() : skuInfo.getPrice().floatValue();
    }

    public static String getImage(AlibabaProductSKUAttrInfo attribute) {
        if (StrUtil.isBlank(attribute.getSkuImageUrl())) {
            return null;
        }
        if (attribute.getSkuImageUrl().contains(IMAGE_PREFIX)) {
            return attribute.getSkuImageUrl();
        } else {
            return IMAGE_PREFIX + attribute.getSkuImageUrl();
        }
    }

    private void oneSku(Long productId, String skuAttributeName, AlibabaProductProductSKUInfoPlus[] skuInfos, String currency) {
        int i = 0;
        final List<String> options = new ArrayList<>(skuInfos.length);
        final List<String> images = new ArrayList<>(skuInfos.length);
        for (AlibabaProductProductSKUInfoPlus skuInfo : skuInfos) {
            final AlibabaProductSKUAttrInfo attribute = skuInfo.getAttributes()[0];
            if (i == 0) {
                skuAttributeName = attribute.getAttributeName();
            }

            final ShopeeProductSkuDTO productSku = getProductSku(getPrice(skuInfo), productId);
            String skuCode = ConvertUtils.generate(attribute.getAttributeValue());
            productSku.setSkuCode(skuCode);
            productSku.setFeature(FeatrueUtil.addFeature(productSku.getFeature(), "specId", skuInfo.getSpecId()));

            final String image = getImage(attribute);
            productSku.setImage(image);
            productSku.setCurrency(currency);
            productSku.setSkuOptionOneIndex(i++);
            if(Objects.nonNull(skuInfo.getStock())){
                productSku.setStock(CommonUtils.getMaxStock(skuInfo.getStock()));
            }
//            productSku.setStock(skuInfo.getAmountOnSale());
            shopeeProductSkuService.save(productSku);

            options.add(attribute.getAttributeValue());
            images.add(image);
        }

        saveSkuAttribute(productId, skuAttributeName, options,images.stream().limit(30).collect(toList()));
    }

    private void saveSkuAttribute(Long productId, String skuAttributeName, List<String> options,List<String> imagesUrl) {
        final ShopeeSkuAttributeDTO skuAttribute = new ShopeeSkuAttributeDTO();
        skuAttribute.setProductId(productId);
        skuAttribute.setName(skuAttributeName);
        skuAttribute.setOptions(options);
        skuAttribute.setImagesUrl(imagesUrl);
        shopeeSkuAttributeService.save(skuAttribute);
    }

    private long getWeight(AlibabaProductProductInfo productInfo) {
        if (productInfo.getShippingInfo() == null || productInfo.getShippingInfo().getUnitWeight() == null) {
            return 0L;
        }
        return new Double(productInfo.getShippingInfo().getUnitWeight() * 1000).longValue();
    }

    private ShopeeProductSkuDTO getProductSku(Float price, Long productId) {
        final ShopeeProductSkuDTO productSku = new ShopeeProductSkuDTO();
        productSku.setProductId(productId);
        productSku.setPrice(price);
        return productSku;
    }
}
