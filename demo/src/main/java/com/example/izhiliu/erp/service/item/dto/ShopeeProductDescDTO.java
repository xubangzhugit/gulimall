package com.izhiliu.erp.service.item.dto;

import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeProduct. 商品描述
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeProductDescDTO extends BaseEntity {


    private static final long serialVersionUID = -3595710401097187571L;

    private Long productId;

    private String description;

}
