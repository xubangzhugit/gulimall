package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeAttribute.
 */
@TableName("item_shopee_attribute")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeAttribute extends BEntity {

    private static final long serialVersionUID = -2704961130471751298L;

    private String name;

    private String local;

    private String chinese;

    private String attributeType;

    private String inputType;

    private Integer essential;

    private Integer sort;

    private Long shopeeAttributeId;

    private Long categoryId;

    private Long shopeeCategoryId;
}
