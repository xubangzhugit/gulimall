package com.izhiliu.erp.web.rest.item.vm;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.service.item.dto.PlatformDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * describe: 商品列表显示的数据项
 * <p>
 *
 * @author cheng
 * @date 2019/1/9 13:07
 */
@Data
@Accessors(chain = true)
public class ProductListVM implements Serializable {

    private static final long serialVersionUID = 2951146226559295740L;

    /**
     * 源数据ID
     */
    private String metaDateId;
    private String loginId;
    private Long shopId;
    private Long platformId;
    private Long platformNodeId;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long productId;

    private String name;
    private String description;

    private String collectUrl;
    private String sourceUrl;
    private String onlineUrl;

    private Integer sold;
    private Integer stock;
    private Float price;
    private String currency;
    private Float toPrice;
    private String toCurrency;
    private List<String> images;

    private String statusStr;
    private String nodeUrl;
    private String shopUrl;

    private Instant gmtCreate;
    private Instant gmtModified;

    private LocalProductStatus status;
    private ShopeeItemStatus shopeeItemStatus;
    private Long shopeeItemId;

    private Integer type;

    private String failReason;

    private Float maxPrice;
    private Float minPrice;

    /**
     * 认领到的平台
     */
    private Collection<PlatformDTO> platforms;

    private Collection<ShopVM> shops;

    /**
     * 层 0,单品, 1,一个SKU属性, 2,两个SKU属性
     */
    private Integer variationTier;

    private VariationVM variationWrapper;
}
