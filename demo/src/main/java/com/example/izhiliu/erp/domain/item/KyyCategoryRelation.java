package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 客优云类目商品关系
 * @author Twilight
 * @date 2021/1/18 17:45
 */
@TableName("item_kyy_category_relation")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class KyyCategoryRelation extends BaseEntity {

    private String login;

    private Long productId;

    private String kyyCategoryId;
}
