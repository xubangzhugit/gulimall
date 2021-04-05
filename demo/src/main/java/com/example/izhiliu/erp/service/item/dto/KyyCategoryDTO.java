package com.izhiliu.erp.service.item.dto;

import lombok.*;
import lombok.experimental.Accessors;

import java.time.Instant;


/**
 * @author Twilight
 * @date 2021/1/18 16:21
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KyyCategoryDTO{

    private Long id;

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

    private Instant gmtCreate;
}
