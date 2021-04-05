package com.izhiliu.erp.web.rest.item.param;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Twilight
 * @date 2021/1/18 16:53
 */
@Data
public class KyyCategoryQO {

    private String login;

    /**
     * 客优云erp类目id
     */
    private String kyyCategoryId;

    /**
     * 分类名称
     */
    @NotBlank
    private String categoryName;

    /**
     * 分类级别(1-一级分类 2-二级分类 3-三级分类)
     */
    @NotNull
    private Integer categoryLevel;

    /**
     * 父分类id
     */
    @NotBlank
    private String parentId;

    /**
     * 排序
     */
    private Integer categoryRank;

    /**
     *
     */
    @NotEmpty(groups = {DeleteKyyCategory.class})
    private List<String> kyyCategoryIdList;

    /**
     * 批量删除验证
     */
    public interface DeleteKyyCategory {
    }
}
