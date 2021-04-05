package com.izhiliu.erp.service.discount.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/3 15:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopeeDiscountItemDTO implements Serializable {
    private Long id;
    private String login;
    private String discountId;
    private Long itemId;
    private String itemName;
    /**
     *  0: 单品
     *  2: 双sku
     */
    private int variationTier;

    /**
     * 购买限制
     */
    private int purchaseLimit;

    /**
     *  原价
     */
    private Long itemOriginalPrice;

    /**
     * 折后价
     */
    private Long itemPromotionPrice;

    /**
     * 库存
     */
    private int stock;

    /**
     * 税后原价
     */
    private Long itemInflatedOriginalPrice;

    /**
     * 税后折后价
     */
    private Long itemInflatedPromotionPrice;

    /**
     * 商品冗余字段
     */
    private List<String> images;

    private Long productId;

    private String onlineUrl;

    private String currency;



}
