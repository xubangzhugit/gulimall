package com.izhiliu.erp.service.item.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;

/**
 * A DTO for the ShopeeAttributeValueOption entity.
 */
@Data
@Accessors(chain = true)
public class ShopeeAttributeValueOptionDTO implements Serializable {

    private static final long serialVersionUID = -971619458148646029L;

    private Long id;

    private String value;

    private String local;

    private String chinese;

    private Integer sort;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private Long attributeId;

    private Long shopeeAttributeId;
}
