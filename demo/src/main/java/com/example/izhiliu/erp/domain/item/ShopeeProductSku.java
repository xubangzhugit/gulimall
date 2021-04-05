package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeProductSku.
 */
@TableName("item_shopee_product_sku")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeProductSku extends BEntity {

    private static final long serialVersionUID = -4709886756156972000L;

    private String skuCode;

    private Long price;

    private String currency;

    private Integer stock;

    private String image;

    private Integer sendOutTime;

    private Integer skuOptionOneIndex;

    private Integer skuOptionTowIndex;

    private Long productId;

    private Long shopeeVariationId;

    private Integer sold;

    private  Long originalPrice;

    private  Long discount;

    @TableField(exist = true)
    private  Long discountId;
}
