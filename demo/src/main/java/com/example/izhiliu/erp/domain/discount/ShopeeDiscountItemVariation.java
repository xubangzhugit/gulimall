package com.izhiliu.erp.domain.discount;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseNoLogicEntity;
import lombok.Data;

/**
 * @Author: louis
 * @Date: 2020/8/3 15:52
 */
@TableName("shopee_discount_item_variation")
@Data
public class ShopeeDiscountItemVariation extends BaseNoLogicEntity {
    private String login;

    private String discountId;

    private Long itemId;

    private long variationId;

    private String variationName;

    private long variationOriginalPrice;

    private long variationPromotionPrice;

    private int variationStock;

    private long variationInflatedOriginalPrice;

    private long variationInflatedPromotionPrice;
}
