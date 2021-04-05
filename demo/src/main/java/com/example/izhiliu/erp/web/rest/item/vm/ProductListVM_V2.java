package com.izhiliu.erp.web.rest.item.vm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 15:04
 */
@Data
public class ProductListVM_V2{
    private static final long serialVersionUID = -2531200939925429546L;

    private String shopName;
    private List<VariationMV_V2> variations;
    private List<ShopVM> shops;
    private List<ProductListVM_V2> childs;
    /**
     *  是否打折
     */
    private boolean isDiscount  =  false;

    /**
     * 是否是店铺搬家
     */
    private boolean isStoreMove = false;

    private Integer hasChild;




    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String description;

    private String collectUrl;

    private String collect;

    private String sourceUrl;

    private String onlineUrl;

    private String currency;

    private Integer stock;

    private Integer sold;

    private String skuCode;

    private Integer sendOutTime;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private String failReason;

    private Long shopeeItemId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private Long categoryId;

    private Long shopeeCategoryId;

    private Integer variationTier;

    private Integer oldVariationTier;

    private List<String> images;

    private List<ShopeeProductDTO.Logistic> logistics;

    private String status;

    private String shopeeItemStatus;

    private String loginId;

    private String metaDataId;

    private Long oldPlatformId;

    private Long oldPlatformNodeId;

    @NotNull(groups = ShopeeProductDTO.PlatformNodeProduct.class)
    private Long platformNodeId;

    @NotNull(groups = ShopeeProductDTO.PlatformProduct.class)
    private Long platformId;

    @NotNull(groups = ShopeeProductDTO.ShopProduct.class)
    private Long shopId;

    private Integer type;

    /**
     * search 关键字
     */
    private String keyword;

    /**
     * 乾坤大挪移
     */
    @JsonProperty("vPrice")
    private Long price;

    @JsonProperty("vWeight")
    private Long weight;

    @JsonProperty("weight")
    private Float vWeight;

    @JsonProperty("price")
    private Float vPrice;

    private Float minPrice;
    private Float maxPrice;

    private String warning;

    private Boolean crossBorder;

    private Boolean ignoreCheck;

    private List<Long> shopIds;

    /**
     *   原价
     */
    private Long  originalPrice ;
}
