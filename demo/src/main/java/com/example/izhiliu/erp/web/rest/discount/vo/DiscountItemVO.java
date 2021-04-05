package com.izhiliu.erp.web.rest.discount.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.GetDiscountDetailResult;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/10 9:47
 */
@Data
public class DiscountItemVO {

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
    private float itemOriginalPrice;

    /**
     * 折后价
     */
    private float itemPromotionPrice;

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
     * sku信息
     */
    private List<DiscountVariationVO> variations;

    private List<String> images;

    private String onlineUrl;

    private String currency;

}
