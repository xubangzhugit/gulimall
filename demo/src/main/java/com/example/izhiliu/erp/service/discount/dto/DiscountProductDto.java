package com.izhiliu.erp.service.discount.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiscountProductDto {
    /**
     *  非必须    no
     */
    private int purchaseLimit;

    private long itemId;

    private float itemPromotionPrice;

    /**
     *  非必须    no
     */
    private List<DiscountSkuDto> variations;
}
