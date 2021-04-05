package com.izhiliu.erp.web.rest.discount.vo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: louis
 * @Date: 2020/8/12 10:18
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscountVariationVO {

    private long variationId;

    private String variationName;

    private float variationOriginalPrice;

    private float variationPromotionPrice;

    private int variationStock;

    /**
     * sku显示:
     * true: 开启
     * false: 关闭
     */
    private Boolean enable;
}
