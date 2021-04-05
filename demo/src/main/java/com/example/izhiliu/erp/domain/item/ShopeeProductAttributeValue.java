package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeProductAttributeValue.
 */
@TableName("item_shopee_product_attribute_value")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeProductAttributeValue extends BEntity {

    private static final long serialVersionUID = -7251084552304288323L;

    private String name;

    private String nameChinese;

    private String value;

    private String valueChinese;

    private Integer sort;

    private Long attributeId;

    private Long shopeeAttributeId;

    private Long productId;
}
