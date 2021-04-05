package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;


import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Builder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @author pengzhen
 * @email pengzhen
 * @date 2019-11-20 10:51:56
 */
@TableName(value = "item_shopee_shop_category")
@ApiModel
@Getter
@Setter
public class ShopeeShopCategory extends BaseEntity {
    private static final long serialVersionUID = 1L;
    @Id
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @Min(value = 0)
    private Long id;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @NotBlank
    @Length(min = 0, max = 100)
    private String name;

    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private Long shopCategoryId;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @Min(value = 0)
    private Integer sort;

    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    private Byte hasChild;

    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private String loginId;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @Min(value = 0)
    private Integer deleted;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @NotBlank
    @Length(min = 0, max = 100)
    private String feature;

    /**
     * 0 正常  1 正在同步  2 同步成功   3 同步失败
     */
    @ApiModelProperty(value = " 0 正常  1 正在同步  2 同步成功   3 同步失败 ", example = "null")
    @NotNull
    @Min(value = 0)
    private Integer status;
    /**
     * 最后异步的  序列号
     */
    @ApiModelProperty(value = " 最后异步的  序列号", example = "null")
    @NotNull
    @Min(value = 0)
    private Long lastSyncIndex;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @Min(value = 0)
    private Long shopId;

    public ShopeeShopCategory() {
    }



    @Override
    @SneakyThrows
    public ShopeeShopCategory clone() {
        return (ShopeeShopCategory) super.clone();
    }

}
