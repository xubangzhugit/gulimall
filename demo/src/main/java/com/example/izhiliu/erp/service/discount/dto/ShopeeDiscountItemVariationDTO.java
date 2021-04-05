package com.izhiliu.erp.service.discount.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: louis
 * @Date: 2020/8/3 15:52
 */
@Data
public class ShopeeDiscountItemVariationDTO implements Serializable {

    private Long id;

    private String login;

    private Long itemId;

    private String discountId;

    private long variationId;

    private String variationName;

    private long variationOriginalPrice;

    private long variationPromotionPrice;

    private int variationStock;

    private long variationInflatedOriginalPrice;

    private long variationInflatedPromotionPrice;
}
