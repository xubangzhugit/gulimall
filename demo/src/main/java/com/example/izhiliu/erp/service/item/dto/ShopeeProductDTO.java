package com.izhiliu.erp.service.item.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.enums.enumsclasses.LocalProductStatusClass;
import com.izhiliu.erp.domain.enums.enumsclasses.ShopeeItemStatusClass;
import com.izhiliu.erp.service.module.metadata.dto.PriceRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A DTO for the ShopeeProduct entity.
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopeeProductDTO implements Serializable {

    private static final long serialVersionUID = -593020037192602931L;

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

    /**
     * 尺寸图
     */
    private String sizeChart;

    private Integer variationTier;

    private Integer oldVariationTier;

    /**
     * shopee商品模型需要的图片
     */
    private List<String> images;

    /**
     * 所有采集的图片，改字段数据过大，不应该放mysql,去mangodb里取
     */
    private List<String> allCollectionImages;

    private List<Logistic> logistics;

    private LocalProductStatus status;

    private ShopeeItemStatus shopeeItemStatus;

    private String loginId;

    private String metaDataId;

    private Long oldPlatformId;

    private Long oldPlatformNodeId;

    @NotNull(groups = PlatformNodeProduct.class)
    private Long platformNodeId;

    @NotNull(groups = PlatformProduct.class)
    private Long platformId;

    @NotNull(groups = ShopProduct.class)
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

    private Integer length;

    private Integer width;

    private Integer height;

    private Float minPrice;
    private Float maxPrice;

    private String warning;

    private Boolean crossBorder;

    private Boolean ignoreCheck;

    private List<Long> shopIds;

    private String startDateTime;

    private String endDateTime;

    //   批发价  价格区间
    private List<PriceRange> priceRange;

    private String discountActivityName;

    /**
     * 原价
     */
    private Long originalPrice;


    private Long discountActivityId;

    private Boolean discountActivity;

    private Boolean release = true;

    private LocalProductStatusClass newStatus;
    /**
     * 是否二手
     */
    private Boolean isCondition;

    private Integer likes;

    private Integer views;
    /**
     * 销售量
     */
    private Integer sales;

    /**
     * 评论数
     */
    private Integer cmtCount;

    /**
     * 星级评分
     */
    private Float ratingStar;


    private ShopeeItemStatusClass newShopeeItemStatus;

    public Boolean getDiscountActivity() {
        return discountActivity;
    }

    public void setDiscountActivity(Boolean discountActivity) {
        this.discountActivity = discountActivity;
    }

    /**
     * 注：dto.setNewStatus(messageSource.country(entity.getStatus()));
     * 注：newStutus 为当前的shopee_product的status 。待发布  &  发布失败 置已发布状态为false [允许进行保存、立即发布 菜单]
     * 注：===> 修改后，&  定时发布中，
     */
    public Boolean getRelease() {

        if (Objects.nonNull(newStatus)) {
            if (Objects.equals(LocalProductStatus.PUBLISH_FAILURE.code, this.newStatus.statusCode)
                    || Objects.equals(LocalProductStatus.NO_PUBLISH.code, this.newStatus.statusCode)
                    || Objects.equals(LocalProductStatus.IN_TIMED_RELEASE.code, this.newStatus.statusCode)) {
                return false;
            }
        } else {
            return false;
        }
        return release;
    }

    public void setRelease(Boolean release) {
        this.release = release;
    }

    @Data
    @Accessors(chain = true)
    public static class Logistic implements Serializable {
        private static final long serialVersionUID = 6519470437232668370L;

        private Boolean enabled;
        private Boolean isFree;
        private Long sizeId;
        private Long logisticId;
        private String logisticName;
        private Float shippingFee;
    }

    public interface PlatformProduct {
    }

    public interface PlatformNodeProduct {
    }

    public interface ShopProduct {
    }
}
