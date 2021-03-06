package com.izhiliu.erp.service.module.metadata.convert;

import com.alibaba.fastjson.JSON;
import com.alibaba.product.param.AlibabaProductProductAttribute;
import com.alibaba.product.param.AlibabaProductProductInfo;
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
public class AlibabaMetaDataConvert implements MetaDataConvert<AlibabaMetaDataConvert.AlibabaMetaData> {

    public static final String IMAGE_PREFIX = "https://cbu01.alicdn.com/";

    @Override
    public ProductMetaDataDTO collect(AlibabaMetaData data) {
        final AlibabaProductProductInfo productInfo = data.getProductInfo();



        final List<String> images = Arrays.stream(productInfo.getImage().getImages())
            .map(this::getImage)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<String> descImages = new ArrayList<>();
        if (StringUtils.isNotBlank(productInfo.getDescription())) {
            descImages.addAll(AlibabaMetaDateMap.getDescImage(productInfo.getDescription()));
        }

        final ProductMetaDataDTO metaData = new ProductMetaDataDTO();
        metaData.setProductId(productInfo.getProductID());
        metaData.setName(productInfo.getSubject());
        metaData.setDescription(getDescription(productInfo));
        metaData.setPrice(getPrice(productInfo));
        metaData.setImages(images);
        metaData.setMainImages(images);
        metaData.setDescImages(descImages);
        metaData.setUrl(data.getUrl());

        metaData.setLoginId(data.getLoginId());
        metaData.setCollectTime(Instant.now());
        metaData.setCurrency(CurrencyEnum.CNY.code);
        metaData.setPlatform(PlatformEnum.ALIBABA.getName());
        metaData.setPlatformId(PlatformEnum.ALIBABA.getCode().longValue());
        metaData.setJson(JSON.toJSONString(productInfo));
        metaData.setCategoryId(productInfo.getCategoryID());
        return metaData;
    }

    public String getImage(String e) {
        if (StringUtils.isBlank(e)) {
            return null;
        }
        if (e.contains(IMAGE_PREFIX)) {
            return e;
        } else {
            return IMAGE_PREFIX + e;
        }
    }

    private long getPrice(AlibabaProductProductInfo productInfo) {
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

    private String getDescription(AlibabaProductProductInfo productInfo) {
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
    public static class AlibabaMetaData extends BaseMetaData {

        private AlibabaProductProductInfoPlus productInfo;

        public AlibabaMetaData(Integer platformId, String json, String loginId, String url, AlibabaProductProductInfoPlus productInfo) {
            super(platformId, json, loginId, url);
            this.productInfo = productInfo;
        }
    }
}

