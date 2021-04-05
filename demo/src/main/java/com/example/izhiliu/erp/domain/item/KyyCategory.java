package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


/**
 * erp客优云类目
 * @author Twilight
 * @date 2021/1/18 15:34
 */
@TableName("item_kyy_category")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class KyyCategory extends BaseEntity {

    private static final long serialVersionUID = -8845776526067123226L;

    /**
     * 客优云erp类目id
     */
    private String kyyCategoryId;


    private String login;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类级别(1-一级分类 2-二级分类 3-三级分类)
     */
    private Integer categoryLevel;

    /**
     * 父分类id
     */
    private String parentId;

    /**
     * 排序
     */
    private Integer categoryRank;
}
