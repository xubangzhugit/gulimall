package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeAttributeValueOption.
 */
@TableName("item_shopee_attribute_value_option")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeAttributeValueOption extends BEntity {

    private static final long serialVersionUID = -905384590423662412L;

    private String value;

    private String local;

    private String chinese;

    private Integer sort;

    private Long attributeId;

    private Long shopeeAttributeId;
}
