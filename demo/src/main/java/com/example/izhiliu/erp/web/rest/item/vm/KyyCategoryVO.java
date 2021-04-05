package com.izhiliu.erp.web.rest.item.vm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author Twilight
 * @date 2021/1/19 11:12
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KyyCategoryVO {

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
