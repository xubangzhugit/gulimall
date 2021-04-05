package com.izhiliu.erp.service.module.metadata.convert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.product.param.AlibabaProductProductAttribute;
import com.izhiliu.core.common.constant.CurrencyEnum;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.module.metadata.basic.BaseMetaData;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataConvert;
import com.izhiliu.erp.service.module.metadata.dto.AlibabaProductProductInfoPlus;
import com.izhiliu.erp.service.module.metadata.map.AlibabaMetaDateMap;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/25 14:44
 */
@Component
public class PingduoduoMetaDataConvert implements MetaDataConvert<PingduoduoMetaDataConvert.PingduoduoMeteData> {

    public static final String IMAGE_PREFIX = "https://t00img.yangkeduo.com/";
    public static final String HTTPS_PREFIX = "https:";
    public static final String COM_PREFIX = "com";

    @Override
    public ProductMetaDataDTO collect(PingduoduoMeteData data) {
        final AlibabaProductProductInfoPlus productInfo = data.getProductInfo();
        final List<String> images = Arrays.stream(productInfo.getImage().getImages())
            .map(PingduoduoMetaDataConvert::getImage)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<String> descImages = new ArrayList<>();
        if (StringUtils.isNotBlank(productInfo.getDescription())) {
            descImages.addAll(JSONArray.parseArray(productInfo.getDescription(),String.class));
        }

        final ProductMetaDataDTO metaData = new ProductMetaDataDTO();
        metaData.setProductId(productInfo.getProductID());
        metaData.setName(productInfo.getSubject());
        metaData.setDescription(getDescription(productInfo));
        metaData.setPrice(getPrice(productInfo));
        metaData.setImages(images);
        metaData.setUrl(data.getUrl());

        metaData.setMainImages(images);
        metaData.setDescImages(descImages);

        metaData.setLoginId(data.getLoginId());
        metaData.setCollectTime(Instant.now());
        metaData.setCurrency(CurrencyEnum.CNY.code);
        metaData.setPlatform(PlatformEnum.PDD.getName());
        metaData.setPlatformId(PlatformEnum.PDD.getCode().longValue());
        metaData.setCategoryId(productInfo.getCategoryID());
        metaData.setJson(JSON.toJSONString(productInfo));
        return metaData;
    }

    public final static String getImage(String e) {
        if (StringUtils.isBlank(e)) {
            return null;
        }
        if (e.contains(HTTPS_PREFIX)) {
            return e;
        } else {
            if(e.contains(COM_PREFIX)){
                return  HTTPS_PREFIX + e;
            }
            return IMAGE_PREFIX + e;
        }
    }

    private long getPrice(AlibabaProductProductInfoPlus productInfo) {
        if (productInfo.getReferencePrice() == null) {
            return 0;
        }

        String[] prices;
        if (productInfo.getReferencePrice().contains("-")) {
            prices = productInfo.getReferencePrice().split("-");
        } else if (productInfo.getReferencePrice().contains("~")) {
            prices = productInfo.getReferencePrice().split("~");
        } else {
            prices = new String[1];
            prices[0] = productInfo.getReferencePrice();
        }
        String price = prices.length == 2 ? prices[1] : prices[0];

        return new Float((Float.parseFloat(price) * 100)).longValue();
    }

    private String getDescription(AlibabaProductProductInfoPlus productInfo) {
        return Arrays.stream(productInfo.getAttributes()).collect(Collectors.groupingBy(AlibabaProductProductAttribute::getAttributeName)).entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue().stream().map(AlibabaProductProductAttribute::getValue).collect(Collectors.joining(",")))
            .collect(Collectors.joining("\r\n"));
    }

    /**
     * describe:
     * <p>
     *
     * @author cheng
     * @date 2019/1/25 15:40
     */
    @Data
    public static class PingduoduoMeteData extends BaseMetaData {

        private AlibabaProductProductInfoPlus productInfo;

        public PingduoduoMeteData(Integer platformId, String json, String loginId, String url, AlibabaProductProductInfoPlus productInfo) {
            super(platformId, json, loginId, url);
            this.productInfo = productInfo;
        }
    }
}

