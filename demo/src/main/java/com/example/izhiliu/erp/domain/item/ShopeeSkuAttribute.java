package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeSkuAttribute.
 */
@TableName("item_shopee_sku_attribute")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeSkuAttribute extends BEntity {

    private static final long serialVersionUID = -7420042924876951655L;

    private String name;

    private String nameChinese;

    private String options;

    /**
     * sku属性图片，如颜色：红色，白色
     */
    private String imagesUrl;

    private String optionsChinese;

    private Long productId;
}
