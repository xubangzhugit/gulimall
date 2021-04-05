package com.izhiliu.erp.service.item.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;

/**
 * A DTO for the ShopeeProductAttributeValue entity.
 */
@Data
@Accessors(chain = true)
public class ShopeeProductAttributeValueDTO implements Serializable {

    private static final long serialVersionUID = -1722679933292481084L;

    private Long id;

    private String name;

    private String nameChinese;

    private String value;

    private String valueChinese;

    private Integer sort;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private Long attributeId;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long productId;

    private Long shopeeAttributeId;
}
