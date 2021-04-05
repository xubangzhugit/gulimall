package com.izhiliu.erp.service.module.metadata.origin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/7/20 16:35
 */
@Data
public class E7OriginSKuInfo {
    private Long total;
    private List<Sku> sku;

    @Data
    public static class Sku {
        private Integer num;
        private BigDecimal price;
        private BigDecimal price2;
        private String skuId;
        private String barcode;
        private String created;
        private String imgUrl;
        private String modified;
        private String outerId;
        private String properties;
        private String propertiesName;
    }

}
