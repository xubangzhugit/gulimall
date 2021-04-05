package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeProduct. 商品媒体资源表，包括商品图片，视频，尺寸图
 */
@TableName("item_shopee_product_extra_info")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeProductExtraInfo extends BEntity {

    private static final long serialVersionUID = -4837404238730434180L;


    private Long productId;

    private String login;
    /**
     *    清除  BEntity  带来的影响
     */
    @TableField(exist = false)
    private String feature;

    @Override
    public String getFeature() {
        return feature;
    }

    @Override
    public void setFeature(String feature) {
        this.feature = feature;
    }

    /**
     * 销售量
     */
    private Integer sales;

    /**
     * 评论数
     */
    private Integer cmtCount  ;

    /**
     * 星级评分
     */
    private  Integer ratingStar  ;

    /**
     *  浏览数
     */
    private Integer views;

    /**
     *   点赞数
     */
    private Integer likes;


}
