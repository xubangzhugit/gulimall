package com.izhiliu.erp.domain.discount;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseNoLogicEntity;
import lombok.Data;

/**
 * @Author: louis
 * @Date: 2020/8/3 15:52
 */
@TableName("shopee_discount_item")
@Data
public class ShopeeDiscountItem extends BaseNoLogicEntity {
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
}
