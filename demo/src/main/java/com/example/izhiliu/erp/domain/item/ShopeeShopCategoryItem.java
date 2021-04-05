package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import lombok.experimental.Accessors;

/**
 * @author pengzhen
 * @email pengzhen
 *  关系表
 * @date 2019-11-20 10:51:56
 */
@TableName(value = "item_shopee_shop_category_product")
@ApiModel
@Getter
@Setter
@Accessors(chain = true)
public class ShopeeShopCategoryItem extends BEntity {
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @Min(value = 0)
    private Long shopeeProductId;


    /**
     *
     */
    @ApiModelProperty(value = " ", example = "null")
    @NotNull
    @Min(value = 0)
    private Integer status;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @Min(value = 0)
    private Long shopeeCategoryId;


    private Long categoryId;

    private String loginId;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    @NotNull
    @Min(value = 0)
    private Long shopeeProductItemId;

    public ShopeeShopCategoryItem() {
    }

    ;

    @Override
    @SneakyThrows
    public ShopeeShopCategoryItem clone() {
        return (ShopeeShopCategoryItem) super.clone();
    }

}
