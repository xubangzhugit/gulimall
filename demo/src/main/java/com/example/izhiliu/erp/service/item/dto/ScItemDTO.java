package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.web.rest.item.vm.VariationVM;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Accessors(chain = true)
public class ScItemDTO implements Serializable {

    private static final long serialVersionUID = -3771564931442180685L;

    private Long id;
    private String loginId;
    private String title;
    private String skuCode;
    private String currency;
    private Integer variationTier;
    private String skuName;
    private String providerName;
    private Float costPrice;
    private Integer length;
    private Integer width;
    private Integer height;
    private Float weight;
    private List<String> images;
    private String skuImage;
    private VariationVM variations;
    private String sourceUrl;
    private String specId;
    private Long providerId;
    private Instant gmtCreate;
    private Instant gmtModified;
    private String feature;
    private List<ScItemDTO> scItemDTOS;
    private List<String> productSkuCode;//中间表插入多个商品skuCode 对应 货品skuCode
    private String skuTitle;
    private String skuValue;//1688货品sku属性
    private String alibabaId;
}
