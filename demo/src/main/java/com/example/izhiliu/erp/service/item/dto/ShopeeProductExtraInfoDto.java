package com.izhiliu.erp.service.item.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * A ShopeeProduct. 商品媒体资源表，包括商品图片，视频，尺寸图
 */
@Data
@EqualsAndHashCode()
@Accessors(chain = true)
public class ShopeeProductExtraInfoDto {

    private static final long serialVersionUID = -4837404238730434180L;

    protected Long id;

    private String login;

    private Long productId;

    private Instant gmtCreate;

    private Instant gmtModified;

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
    private  Float ratingStar  ;

    /**
     *  浏览数
     */
    private Integer views;

    /**
     *   点赞数
     */
    private Integer likes;


}
