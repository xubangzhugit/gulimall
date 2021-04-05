package com.izhiliu.erp.service.item.module.convert;

import com.alibaba.fastjson.JSON;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;
import com.izhiliu.erp.service.item.module.basic.BaseModelConvert;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.LazadaMetaDataConvert;
import com.izhiliu.erp.util.FeatrueUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/1 9:33
 */
@Component
public class LazadaMetaConvertShopee extends BaseModelConvert {



    @Override
    public void map(ProductMetaDataDTO data, String loginId, MetaDataObject.CollectController collectController){
        LazadaMetaDataConvert.LazadaMetaData lazadaProduct = JSON.parseObject(data.getJson(), LazadaMetaDataConvert.LazadaMetaData.class);

        ShopeeProductDTO product = new ShopeeProductDTO();
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
        product.setCurrency(data.getCurrency());
        if(Objects.nonNull(collectController.getPricing())){
            product.setFeature(FeatrueUtil.addFeature(null,"pricingId",collectController.getPricing().toString()));
        }
        Long productId = shopeeProductService.save(product).getId();

        /*
         * TODO Lazada 商品只区分单SKU和双SKU,没办法判断单品
         */
        int variationTier = 0;
        List<LazadaMetaDataConvert.LazadaMetaData.SkuAttribute> lazadaSkuAttributes = lazadaProduct.getSkuAttributes();

        if (lazadaSkuAttributes.size() == 0) {
            LazadaMetaDataConvert.LazadaMetaData.SkuInfo skuInfo = lazadaProduct.getSkuInfos().get(0);
             LazadaMetaDataConvert.LazadaMetaData.SkuInfo.Price.PriceInfo originalPrice = skuInfo.getPrice().getOriginalPrice();
            if (Objects.isNull(originalPrice)) {
                originalPrice =skuInfo.getPrice().getSalePrice();

            }
            shopeeProductSkuService.batchSave(Arrays.asList(new ShopeeProductSkuDTO()
                    .setProductId(productId)
                    .setImage(skuInfo.getImage())
                    .setStock(CommonUtils.getMaxStock(collectController.isCollectStock() ? skuInfo.getStock() : collectController.getStock()))
                    .setPrice(collectController.isCollectDiscount() ? skuInfo.getPrice().getSalePrice().getValue().floatValue() : originalPrice.getValue().floatValue())
                    .setCurrency(product.getCurrency())));
        } else if (lazadaSkuAttributes.size() == 1) {
            variationTier = 1;
            saveSkuAttribute(productId, lazadaSkuAttributes.get(0));

            saveProductSkus(lazadaProduct, product, productId,collectController,lazadaSkuAttributes);
        } else {
            variationTier = 2;

            saveSkuAttribute(productId, lazadaSkuAttributes.get(0));
            saveSkuAttribute(productId, lazadaSkuAttributes.get(1));

            saveProductSkus(lazadaProduct, product, productId, collectController, lazadaSkuAttributes);
        }
        shopeeProductService.update(new ShopeeProductDTO()
                .setId(productId)
                .setVariationTier(variationTier));
    }



    public void saveProductSkus(LazadaMetaDataConvert.LazadaMetaData lazadaProduct, ShopeeProductDTO product, Long productId, MetaDataObject.CollectController collectController, List<LazadaMetaDataConvert.LazadaMetaData.SkuAttribute> lazadaSkuAttributes) {
        List<ShopeeProductSkuDTO> productSkus = lazadaProduct.getSkuInfos().stream().map(e -> {
            ShopeeProductSkuDTO productSku = new ShopeeProductSkuDTO();
            productSku.setProductId(productId);
            productSku.setCurrency(product.getCurrency());
            productSku.setStock(CommonUtils.getMaxStock(collectController.isCollectStock() ? e.getStock() : collectController.getStock()));
            productSku.setPrice(collectController.isCollectDiscount()?e.getPrice().getSalePrice().getValue().floatValue():e.getPrice().getOriginalPrice().getValue().floatValue());
            productSku.setSkuOptionOneIndex(e.getIndexs().get(0));
            productSku.setImage(e.getImage());

            if (e.getIndexs().size() == 2) {
                productSku.setSkuOptionTowIndex(e.getIndexs().get(1));
                productSku.setSkuCode(
                        ConvertUtils.generate(lazadaSkuAttributes.get(0).getOptions().get(e.getIndexs().get(0)),
                                lazadaSkuAttributes.get(1).getOptions().get(e.getIndexs().get(1))
                        )
                );
            }else{
                productSku.setSkuCode(
                        ConvertUtils.generate(lazadaSkuAttributes.get(0).getOptions().get(e.getIndexs().get(0)))
                );
            }


            return productSku;
        }).collect(Collectors.toList());
        shopeeProductSkuService.batchSave(productSkus);
    }

    public void saveSkuAttribute(Long productId, LazadaMetaDataConvert.LazadaMetaData.SkuAttribute lazadaSkuAttribute) {
        ShopeeSkuAttributeDTO skuAttribute = new ShopeeSkuAttributeDTO();
        skuAttribute.setProductId(productId);
        skuAttribute.setName(lazadaSkuAttribute.getName());
        skuAttribute.setOptions(lazadaSkuAttribute.getOptions());
        skuAttribute.setImagesUrl(lazadaSkuAttribute.getImage().stream().limit(35).collect(Collectors.toList()));
        shopeeSkuAttributeService.save(skuAttribute);
    }
}
