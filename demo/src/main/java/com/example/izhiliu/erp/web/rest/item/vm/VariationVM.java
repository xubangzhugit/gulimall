package com.izhiliu.erp.web.rest.item.vm;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.erp.service.item.dto.ShopeeProductInsertDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/12 20:58
 */
@Data
@Accessors(chain = true)
public class VariationVM implements Serializable {

    private static final long serialVersionUID = -1092413438135476659L;

    private Boolean version ;
    @NotNull
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    @NotNull
    private Integer variationTier;

    @JsonSerialize(using = ToStringSerializer.class)
    private Float price;

    /**
     *  用户登录帐号
     */
    private String login;


    /**
     *  是否打折
     */
    private boolean isDiscount  =  false;
    /**
     *   原价
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Float  originalPrice ;

    private Long skuAttributeId;
    private String variationName;

    @Valid
    private List<Variation> variations;

    @Valid
    private List<VariationIndex> variationIndexs;

    @Data
    @Accessors(chain = true)
    public static class VariationIndex implements Serializable {
        private static final long serialVersionUID = 5855119018794159730L;
        private Long id;

        @NotNull(groups = {ShopeeProductInsertDTO.Insert.class},message = "need  price")
        @DecimalMin(value = "0.01",groups = {ShopeeProductInsertDTO.Insert.class},message = "need price >  0.01")
        private Float price;
        @NotNull(groups = {ShopeeProductInsertDTO.Insert.class},message = "insert  need  currency")
        @NotBlank(groups = {ShopeeProductInsertDTO.NewInsert.class},message = "insert  need  currency")
        private String currency;
        @NotNull(groups = {ShopeeProductInsertDTO.Insert.class},message = "insert  need  stock")
        @Min(value = 0l,groups = {ShopeeProductInsertDTO.Insert.class},message = "need stock >  0")
        private Integer stock;
        private Integer defaultStock;

        private String skuCode;
        private Long variationId;
        private List<Integer> index;
        //添加的
        private String skuImage;
        private String specId;
        private List<String> productSkuCode;//中间表插入多个商品skuCode 对应 货品skuCode
        private Integer length;
        private Integer width;
        private Integer height;
        private Float weight;
        private String sourceUrl;
        private String title;
        private Long providerId;
        private String skuTitle;//供应商标题
        private String alibabaId;
        private String skuValue;
        private String providerName;//供应商名字
        private  Float originalPrice;
        private  Float discount;
        private  Long discountId;

    }

    @Data
    @Accessors(chain = true)
    public static class Variation implements Serializable {
        private static final long serialVersionUID = 1045155664912132985L;
        private Long id;
        private String name;
        @NotNull(groups = {ShopeeProductInsertDTO.OneInsert.class},message = "need  price")
        @DecimalMin(value = "0.01",groups = {ShopeeProductInsertDTO.Insert.class},message = "need price >  0.01")
        private Float price;
        @NotNull(groups = {ShopeeProductInsertDTO.OneInsert.class},message = "insert  need  currency")
        @NotBlank(groups = {ShopeeProductInsertDTO.OneNewInsert.class},message = "insert  need  currency")
        private String currency;
        @NotNull(groups = {ShopeeProductInsertDTO.OneInsert.class},message = "insert  need  stock")
        @Min(value = 1l,groups = {ShopeeProductInsertDTO.OneInsert.class},message = "need stock >  1")
        private Integer stock;

        private String skuCode;
        private Long variationId;
        private List<String> options;
        //添加的
        private String skuImage;
        private String specId;
        private List<String> productSkuCode;//中间表插入多个商品skuCode 对应 货品skuCode
        private Integer length;
        private Integer width;
        private Integer height;
        private Float weight;
        private String sourceUrl;
        private String title;
        private Long providerId;
        private String skuTitle;//供应商标题
        private String alibabaId;
        private String skuValue;//1688货品属性
        private String providerName;//供应商名字
        private  Float originalPrice;
        private  Float discount;
        private  Long discountId;
        /**
         * 变种图片，sku属性图片 ,例如颜色图
         */
        private List<String> imageUrls;
    }
}
