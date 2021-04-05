package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A Platform.
 */
@TableName("item_platform")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class Platform extends BaseEntity {

    private static final long serialVersionUID = -2261388777041323159L;

    private String name;

    private String description;

    private String url;

    private Integer canClaim;
}
