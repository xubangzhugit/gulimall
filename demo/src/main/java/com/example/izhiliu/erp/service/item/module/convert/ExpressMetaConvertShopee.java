package com.izhiliu.erp.service.item.module.convert;

import cn.hutool.core.util.ReUtil;
import com.aliyun.openservices.shade.com.alibaba.fastjson.JSON;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;
import com.izhiliu.erp.service.item.module.basic.BaseModelConvert;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.ExpressMetaDataConvert;
import com.izhiliu.erp.util.FeatrueUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/3 9:39
 */
@Component
public class ExpressMetaConvertShopee extends BaseModelConvert {

    @Override
    public void map(ProductMetaDataDTO data, String loginId, MetaDataObject.CollectController collectController) {
        try {
            ExpressMetaDataConvert.ExpressMetaData expressMetaData = JSON.parseObject(data.getJson(), ExpressMetaDataConvert.ExpressMetaData.class);

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

            product.setSold(data.getSold());
            product.setPrice(data.getPrice());
            product.setCurrency(data.getCurrency());
            if(Objects.nonNull(collectController.getPricing())){
                product.setFeature(FeatrueUtil.addFeature(null,"pricingId",collectController.getPricing().toString()));
            }
            final Long productId = shopeeProductService.save(product).getId();

            int variationTier = 0;
            if (null == expressMetaData.getSkuAttributes() || expressMetaData.getSkuAttributes().size() == 0) {
                singleItem(expressMetaData, product, productId, variationTier);
                return;
            } else if (expressMetaData.getSkuAttributes().size() == 1) {
                variationTier = 1;

                saveSkuAttribute(expressMetaData.getSkuAttributes().get(0), productId);

                List<ShopeeProductSkuDTO> list = expressMetaData.getSkuInfos().stream().map(skuInfo -> fillProductSku(product, skuInfo, expressMetaData.getSkuAttributes().get(0))
                    .setSkuOptionOneIndex(skuInfo.getIndexs().get(0))
                        .setSkuCode(ConvertUtils.generate(expressMetaData.getSkuAttributes().get(0).getOptions().get((skuInfo.getIndexs().get(0))).getOption()))
                ) .collect(Collectors.toList());
                shopeeProductSkuService.batchSave(list);
            } else /*if (expressMetaData.getSkuAttributes().size() == 2)*/ {
                variationTier = 2;

                saveSkuAttribute(expressMetaData.getSkuAttributes().get(0), productId);
                saveSkuAttribute(expressMetaData.getSkuAttributes().get(1), productId);

                List<ShopeeProductSkuDTO> list = expressMetaData.getSkuInfos().stream().map(skuInfo -> fillProductSku(product, skuInfo, expressMetaData.getSkuAttributes().get(0))
                    .setSkuOptionOneIndex(skuInfo.getIndexs().get(0))
                    .setSkuOptionTowIndex(skuInfo.getIndexs().get(1))
                        .setSkuCode(ConvertUtils.generate(expressMetaData.getSkuAttributes().get(0).getOptions().get((skuInfo.getIndexs().get(0))).getOption()
                                ,expressMetaData.getSkuAttributes().get(1).getOptions().get(skuInfo.getIndexs().get(1)).getOption()))
                )
                    .collect(Collectors.toList());
                shopeeProductSkuService.batchSave(list);
            }
            updateVariationTier(variationTier, productId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSkuAttribute(ExpressMetaDataConvert.ExpressMetaData.SkuAttribute skuAttribute, long productId) {
        ShopeeSkuAttributeDTO shopeeSkuAttribute = new ShopeeSkuAttributeDTO()
                .setProductId(productId)
                .setName(skuAttribute.getName())
                .setImagesUrl(skuAttribute.getOptions().stream()
                        .map(ExpressMetaDataConvert.ExpressMetaData.SkuAttribute.Option::getSkuPropertyImagePath).limit(35).collect(Collectors.toList()))
                .setOptions(skuAttribute.getOptions().stream()
                        .map(ExpressMetaDataConvert.ExpressMetaData.SkuAttribute.Option::getOption)
                        .collect(Collectors.toList()));

        shopeeSkuAttributeService.save(shopeeSkuAttribute);
    }

    public void singleItem(ExpressMetaDataConvert.ExpressMetaData expressMetaData, ShopeeProductDTO product, Long productId, int variationTier) {
        final ShopeeProductSkuDTO productSku = fillProductSku(product, expressMetaData.getSkuInfos().get(0), null);
        shopeeProductSkuService.save(productSku);
        updateVariationTier(variationTier, productId);
    }

    public ShopeeProductSkuDTO fillProductSku(ShopeeProductDTO product, ExpressMetaDataConvert.ExpressMetaData.SkuInfo skuVal, ExpressMetaDataConvert.ExpressMetaData.SkuAttribute skuAttribute) {
        float price = Float.parseFloat(skuVal.getSkuVal().getSkuCalPrice().replace(",", ""));
        int stock = skuVal.getSkuVal().getBulkOrder();
        String imgUrl = product.getImages().get(0);
        if (null != skuAttribute) {
            //从skuAttr中拿到第一个属性的属性值图片作为sku的图片,"skuAttr":"14:1052;200007763:201336100"
            String attrStr = ReUtil.get("\\d+:\\d+", skuVal.getSkuAttr(), 0);
            if (StringUtils.isNotBlank(attrStr) && attrStr.split(":").length > 1) {
                if (attrStr.split(":")[0].equals(skuAttribute.getId().toString())) {
                    imgUrl = skuAttribute.getOptions().stream().filter(e -> e.getId().equals(Long.valueOf(attrStr.split(":")[1]))).findFirst().map(ExpressMetaDataConvert.ExpressMetaData.SkuAttribute.Option::getSkuPropertyImagePath).orElse("");
                }
            }
        }

        return new ShopeeProductSkuDTO()
                .setProductId(product.getId())
                .setPrice(price)
                .setStock(CommonUtils.getMaxStock(stock))
                .setCurrency(product.getCurrency())
                .setSkuCode(skuVal.getSkuId())
                .setFeature(FeatrueUtil.addFeature("", "specId", skuVal.getSkuId()))
                .setImage(imgUrl);
    }

//    public static void main(String[] args) {
//        String json = "{\"attributes\":[{\"name\":\"Gender:\",\"value\":\"Women\"},{\"name\":\"Brand Name:\",\"value\":\"NLW\"},{\"name\":\"Material:\",\"value\":\"Polyester\"},{\"name\":\"Style:\",\"value\":\"Bohemian\"},{\"name\":\"Silhouette:\",\"value\":\"Loose\"},{\"name\":\"Pattern Type:\",\"value\":\"Solid\"},{\"name\":\"Sleeve Length(cm):\",\"value\":\"Sleeveless\"},{\"name\":\"Decoration:\",\"value\":\"Ruffles\"},{\"name\":\"Dresses Length:\",\"value\":\"Above Knee, Mini\"},{\"name\":\"Sleeve Style:\",\"value\":\"Butterfly Sleeve\"},{\"name\":\"Waistline:\",\"value\":\"empire\"},{\"name\":\"Neckline:\",\"value\":\"V-Neck\"},{\"name\":\"Season:\",\"value\":\"Summer\"},{\"name\":\"Model Number:\",\"value\":\"L18DR0495\"}],\"description\":\"Gender:: Women\\r\\nBrand Name:: NLW\\r\\nMaterial:: Polyester\\r\\nStyle:: Bohemian\\r\\nSilhouette:: Loose\\r\\nPattern Type:: Solid\\r\\nSleeve Length(cm):: Sleeveless\\r\\nDecoration:: Ruffles\\r\\nDresses Length:: Above Knee, Mini\\r\\nSleeve Style:: Butterfly Sleeve\\r\\nWaistline:: empire\\r\\nNeckline:: V-Neck\\r\\nSeason:: Summer\\r\\nModel Number:: L18DR0495\\r\\n\",\"images\":[\"https://ae01.alicdn.com/kf/HTB1Ha0gAL9TBuNjy1zbq6xpepXaU/NLW-Deep-V-Neck-Yellow-Sexy-Dress-Ruffle-Bow-Women-Dress-Green-Solid-Casual-Bohemian-Beach.jpg\",\"https://ae01.alicdn.com/kf/HTB1WdkjjpooBKNjSZFPq6xa2XXas/NLW-Deep-V-Neck-Yellow-Sexy-Dress-Ruffle-Bow-Women-Dress-Green-Solid-Casual-Bohemian-Beach.jpg\",\"https://ae01.alicdn.com/kf/HTB11zNaAHSYBuNjSspiq6xNzpXa5/NLW-Deep-V-Neck-Yellow-Sexy-Dress-Ruffle-Bow-Women-Dress-Green-Solid-Casual-Bohemian-Beach.jpg\",\"https://ae01.alicdn.com/kf/HTB1lkdaAKOSBuNjy0Fdq6zDnVXa4/NLW-Deep-V-Neck-Yellow-Sexy-Dress-Ruffle-Bow-Women-Dress-Green-Solid-Casual-Bohemian-Beach.jpg\",\"https://ae01.alicdn.com/kf/HTB1uw9yjBnTBKNjSZPfq6zf1XXaJ/NLW-Deep-V-Neck-Yellow-Sexy-Dress-Ruffle-Bow-Women-Dress-Green-Solid-Casual-Bohemian-Beach.jpg\",\"https://ae01.alicdn.com/kf/HTB1oID7jDqWBKNjSZFAq6ynSpXaS/NLW-Deep-V-Neck-Yellow-Sexy-Dress-Ruffle-Bow-Women-Dress-Green-Solid-Casual-Bohemian-Beach.jpg\",\"https://ae01.alicdn.com/kf/HTB1bJQ6b1SSBuNjy0Flq6zBpVXaB.jpg\",\"https://ae01.alicdn.com/kf/HTB1Bxe3AGmWBuNjy1Xaq6xCbXXa9.jpg\",\"https://ae01.alicdn.com/kf/HTB1ep4sAN1YBuNjy1zcq6zNcXXa4.jpg\",\"https://ae01.alicdn.com/kf/HTB1gs2BbUD.BuNjt_ioq6AKEFXaa.jpg\",\"https://ae01.alicdn.com/kf/HTB1dgFyy25TBuNjSspcq6znGFXaM.jpg\",\"https://ae01.alicdn.com/kf/HTB1ySTciSMmBKNjSZTEq6ysKpXaA.jpg\",\"https://ae01.alicdn.com/kf/HTB1mBqbyY1YBuNjSszhq6AUsFXaO.jpg\",\"https://ae01.alicdn.com/kf/HTB1l9RSacUrBKNjSZPxq6x00pXaf.jpg\",\"https://ae01.alicdn.com/kf/HTB1yy5gy7OWBuNjSsppq6xPgpXaI.jpg\",\"https://ae01.alicdn.com/kf/HTB1VpEky_JYBeNjy1zeq6yhzVXa4.jpg\",\"https://ae01.alicdn.com/kf/HTB15OMWyFmWBuNjSspdq6zugXXaa.jpg\",\"https://ae01.alicdn.com/kf/HTB1GBGbyY1YBuNjSszhq6AUsFXar.jpg\",\"https://ae01.alicdn.com/kf/HTB1cPA3iH3nBKNjSZFMq6yUSFXac.jpg\",\"https://ae01.alicdn.com/kf/HTB1n4Xyy25TBuNjSspcq6znGFXaa.jpg\",\"https://ae01.alicdn.com/kf/HTB1xhdyy25TBuNjSspcq6znGFXap.jpg\",\"https://ae01.alicdn.com/kf/HTB1Km6ciSMmBKNjSZTEq6ysKpXaW.jpg\",\"https://ae01.alicdn.com/kf/HTB160BBy1GSBuNjSspbq6AiipXaB.jpg\",\"https://ae01.alicdn.com/kf/HTB1qW47ANGYBuNjy0Fnq6x5lpXas.jpg\",\"https://ae01.alicdn.com/kf/HTB19PtaAHSYBuNjSspiq6xNzpXaG.jpg\"],\"loginId\":\"admin@izhiliu.com\",\"name\":\"NLW Deep V Neck Yellow Sexy Dress Ruffle Bow Women Dress Green Solid Casual Bohemian Beach Dress Vestidos\",\"platformId\":3,\"price\":\"31.65\",\"skuAttributes\":[{\"id\":14,\"name\":\"Color:\",\"options\":[{\"id\":175,\"option\":\"Green \"},{\"id\":29,\"option\":\"WHITE\"},{\"id\":366,\"option\":\"YELLOW\"}]},{\"id\":5,\"name\":\"Size:\",\"options\":[{\"id\":100014064,\"option\":\"S\"},{\"id\":361386,\"option\":\"M\"},{\"id\":361385,\"option\":\"L\"}]}],\"skuInfos\":[{\"indexs\":[2,0],\"skuAttr\":\"14:366;5:100014064\",\"skuPropIds\":\"366,100014064\",\"skuVal\":{\"activity\":true,\"availQuantity\":896,\"bulkOrder\":0,\"inventory\":1066,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}},{\"indexs\":[2,1],\"skuAttr\":\"14:366;5:361386\",\"skuPropIds\":\"366,361386\",\"skuVal\":{\"activity\":true,\"availQuantity\":952,\"bulkOrder\":0,\"inventory\":1032,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}},{\"indexs\":[2,2],\"skuAttr\":\"14:366;5:361385\",\"skuPropIds\":\"366,361385\",\"skuVal\":{\"activity\":true,\"availQuantity\":969,\"bulkOrder\":0,\"inventory\":1011,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}},{\"indexs\":[0,0],\"skuAttr\":\"14:175;5:100014064\",\"skuPropIds\":\"175,100014064\",\"skuVal\":{\"activity\":true,\"availQuantity\":943,\"bulkOrder\":0,\"inventory\":1074,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}},{\"indexs\":[0,1],\"skuAttr\":\"14:175;5:361386\",\"skuPropIds\":\"175,361386\",\"skuVal\":{\"activity\":true,\"availQuantity\":949,\"bulkOrder\":0,\"inventory\":1032,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}},{\"indexs\":[0,2],\"skuAttr\":\"14:175;5:361385\",\"skuPropIds\":\"175,361385\",\"skuVal\":{\"activity\":true,\"availQuantity\":978,\"bulkOrder\":0,\"inventory\":1017,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}},{\"indexs\":[1,0],\"skuAttr\":\"14:29;5:100014064\",\"skuPropIds\":\"29,100014064\",\"skuVal\":{\"activity\":true,\"availQuantity\":963,\"bulkOrder\":0,\"inventory\":1027,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}},{\"indexs\":[1,1],\"skuAttr\":\"14:29;5:361386\",\"skuPropIds\":\"29,361386\",\"skuVal\":{\"activity\":true,\"availQuantity\":969,\"bulkOrder\":0,\"inventory\":999,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}},{\"indexs\":[1,2],\"skuAttr\":\"14:29;5:361385\",\"skuPropIds\":\"29,361385\",\"skuVal\":{\"activity\":true,\"availQuantity\":986,\"bulkOrder\":0,\"inventory\":999,\"skuCalPrice\":\"31.65\",\"skuMultiCurrencyCalPrice\":\"2416.34\",\"skuMultiCurrencyDisplayPrice\":\"2,416.34\"}}],\"sold\":768,\"url\":\"https://www.aliexpress.com/item/NLW-Deep-V-Neck-Yellow-Sexy-Dress-Ruffle-Bow-Women-Dress-Green-Solid-Casual-Bohemian-Beach/32886776171.html?spm=2114.search0103.3.3.31aa7007wNuMUQ&ws_ab_test=searchweb0_0,searchweb201602_5_10065_10068_319_10059_10884_317_10887_10696_321_322_10084_453_10083_454_10103_10618_10307_537_536,searchweb201603_6,ppcSwitch_0&algo_expid=7d57a2e8-032f-4883-92ac-7d63d3909e5f-0&algo_pvid=7d57a2e8-032f-4883-92ac-7d63d3909e5f\"}";
//        System.out.println(JSON.parseObject(json, ExpressMetaDataConvert.ExpressMetaData.class).getSkuAttributes());
//    }
}
