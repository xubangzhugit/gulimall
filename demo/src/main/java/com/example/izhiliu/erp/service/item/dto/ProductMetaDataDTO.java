package com.izhiliu.erp.service.item.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * A DTO for the ProductMetaData entity.
 */
@Data
@Accessors(chain = true)
public class ProductMetaDataDTO implements Serializable {

    private static final long serialVersionUID = -7141156861608455746L;

    private String id;

    private String url;

    private String name;

    private String platform;

    private String suffix;

    private String description;

    private Long price;

    private String currency;

    private Integer sold;

    private Instant collectTime;

    private Integer status;
    /**
     *   类目id
     */
    private Long categoryId;

    @Deprecated
    /**
     * 废弃，拆成主图和详情图
     */
    private List<String> images;
    /**
     * 商品主图
     */
    private List<String> mainImages;

    /**
     * 详情图
     */
    private List<String> descImages;

    private String loginId;

    private Long platformId;

    private Long platformNodeId;

    private String key;

    private Long productId;

    private String json;
}
