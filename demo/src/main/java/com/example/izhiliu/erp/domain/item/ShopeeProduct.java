package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import com.izhiliu.core.domain.common.BaseEntity;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * A ShopeeProduct.
 */
@TableName("item_shopee_product")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeProduct extends BEntity {

    private static final long serialVersionUID = -8845776526067123226L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String name;

    private String description;

    private String collectUrl;

    private String collect;

    private String sourceUrl;

    private String onlineUrl;

    private Long price;

    private String currency;

    protected Integer stock;

    private Integer sold;

    private String skuCode;

    private Long weight;

    private Integer length;

    private Integer width;

    private Integer height;

    private Integer sendOutTime;

    private String logistics;

    private String images;

    private Long parentId;

    private Long categoryId;

    private Integer oldVariationTier;

    private Integer variationTier;

    private Long shopeeItemId;

    private Long shopeeCategoryId;

    private LocalProductStatus status;

    private ShopeeItemStatus shopeeItemStatus;

    private String loginId;

    private String metaDataId;

    private Long platformNodeId;

    private Long platformId;

    private Long shopId;

    private Long oldPlatformId;

    private Long oldPlatformNodeId;

    private Long minPrice;
    private Long maxPrice;

    /**
     * 1 平台商品 2 站点商品 3 店铺商品
     */
    private Integer type;

    /**
     * 警告
     */
    private String warning;

    /**
     * 是否支持海外代发
     */
    private Boolean crossBorder;

    private  Long originalPrice;
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public static final String DESCRIPTION_TAG = "#";
    public static final int DESCRIPTION_MAX_TAG_COUNT = 18;
    public static final float MIN_WEIGHT = 0.01F;
    public static final int MAX_IMAGE_COUNT = 9;

    public enum Type {
        /**
         * 商品类型
         */
        PLATFORM(1),
        PLATFORM_NODE(2),
        SHOP(3);

        public Integer code;

        Type(Integer code) {
            this.code = code;
        }
    }

    public enum VariationTier {
        /**
         * SKU个数
         */
        ZERO(0),
        ONE(1),
        TWO(2);

        public Integer val;

        VariationTier(Integer val) {
            this.val = val;
        }
    }
}
