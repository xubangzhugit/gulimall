package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeProduct. 商品媒体资源表，包括商品图片，视频，尺寸图
 */
@TableName("item_shopee_product_media")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeProductMedia extends BEntity {

    private static final long serialVersionUID = -4837404238730434180L;
    private Long productId;

    private String videoInfo;

    private String images;

    @TableField(strategy = FieldStrategy.NOT_NULL)
    private String sizeChart;

    @TableField(exist = true)
    private Long  discountActivityId;

    @TableField(exist = true)
    private boolean  discountActivity;

    @TableField(exist = false)
    private String  discountActivityName;



    //   批发价  价格区间
    private String priceRange;

    /**
     *  是否二手
     */
    private Boolean isCondition;


}
