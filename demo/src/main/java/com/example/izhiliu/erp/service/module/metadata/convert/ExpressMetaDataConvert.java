package com.izhiliu.erp.service.module.metadata.convert;

import com.alibaba.fastjson.JSON;
import com.izhiliu.core.common.constant.CurrencyEnum;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.module.metadata.basic.BaseMetaData;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/3 20:01
 */
@Component
public class ExpressMetaDataConvert implements MetaDataConvert<ExpressMetaDataConvert.ExpressMetaData> {

    @Override
    public ProductMetaDataDTO collect(ExpressMetaData data) {
        ProductMetaDataDTO metaData = new ProductMetaDataDTO();
        metaData.setLoginId(data.getLoginId());
        metaData.setCurrency(CurrencyEnum.USD.code);
        metaData.setCollectTime(Instant.now());
        metaData.setPlatform(PlatformEnum.ALIEXPRESS.getName());
        metaData.setPlatformId(PlatformEnum.ALIEXPRESS.getCode().longValue());

        metaData.setPrice(getPrice(data));
        metaData.setName(data.getName());
        metaData.setDescription(data.getDescription());
        metaData.setSold(data.getSold());
        metaData.setUrl(data.getUrl());
        metaData.setImages(data.getImages());

        metaData.setMainImages(data.getMianImages());
        metaData.setDescImages(data.getDescImages());
        metaData.setJson(JSON.toJSONString(data));
        metaData.setProductId(data.getProductId());
        metaData.setCategoryId(data.getCategoryId());
        return metaData;
    }

    public long getPrice(ExpressMetaData data) {
        String price = data.getPrice().split("-")[0].replace(",", "");
        return new Float(Float.parseFloat(price) * 100).longValue();
    }

    @Data
    public static class ExpressMetaData extends BaseMetaData {
        private static final long serialVersionUID = -7957232481223076877L;

        private String name;
        private String description;
        private String price;
        private int sold;
        private List<String> images;
        private List<String> mianImages;
        private List<String> descImages;
        private List<Attribute> attributes;
        private List<SkuAttribute> skuAttributes;
        private List<SkuInfo> skuInfos;
        private Long categoryId;
        private Long productId;

        public ExpressMetaData() {
        }

        public ExpressMetaData(Integer platformId, String json, String loginId, String url) {
            super(platformId, json, loginId, url);
        }

        public void setAttributes(List<Attribute> attributes) {
            if (null != attributes && attributes.size() > 0) {
                this.description = attributes.stream()
                    .map(Attribute::toString)
                    .reduce("", (i, j) -> i + j + "\r\n");
            }
            this.attributes = attributes;
        }

        @Data
        @AllArgsConstructor
        public static class Attribute {
            private String name;
            private String value;

            @Override
            public String toString() {
                return name + " " + value;
            }
        }

        @Data
        @AllArgsConstructor
        public static class SkuAttribute {
            private Long id;
            private String name;
            private List<Option> options;

            public SkuAttribute() {
            }

            @Data
            @AllArgsConstructor
            public static class Option {
                private Long id;
                private String option;
                private String skuPropertyImagePath;
                public Option() {
                }
            }
        }

        @Data
        public static class SkuInfo {

            private String skuId;
            private String skuAttr;
            private String skuPropIds;
            private SkuVal skuVal;
            private List<Integer> indexs;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;

                if (o == null || getClass() != o.getClass()) return false;

                final SkuInfo skuInfo = (SkuInfo) o;

                return new EqualsBuilder()
                    .append(indexs, skuInfo.indexs)
                    .isEquals();
            }

            @Override
            public int hashCode() {
                return new HashCodeBuilder(17, 37)
                    .append(indexs)
                    .toHashCode();
            }

            @Data
            public static class SkuVal {
                private int availQuantity;
                private int bulkOrder;
                private int inventory;                        // 库存
                private boolean isActivity;
                private String skuBulkCalPrice;
                private String skuCalPrice;
                private String skuDisplayBulkPrice;
                private String skuMultiCurrencyBulkPrice;
                private String skuMultiCurrencyCalPrice;
                private String skuMultiCurrencyDisplayPrice;
            }
        }
    }
}
