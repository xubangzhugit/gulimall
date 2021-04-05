package com.izhiliu.erp.service.item.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * A DTO for the ShopeeSkuAttribute entity.
 */
@Data
@Accessors(chain = true)
public class ShopeeSkuAttributeDTO implements Serializable {

    private static final long serialVersionUID = 8521364515473823171L;

    private Long id;

    private String name;

    private String nameChinese;

    private List<String> options;

    private List<String> optionsChinese;

    /**
     * 第一个sku属性图片，比如颜色图
     */
    private List<String> imagesUrl;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long productId;
}
