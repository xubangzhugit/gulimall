package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeProduct. 商品描述
 */
@TableName("item_shopee_product_desc")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeProductDesc extends BEntity {


    private static final long serialVersionUID = -3595710401097187571L;

    private Long productId;

    private String description;

}
