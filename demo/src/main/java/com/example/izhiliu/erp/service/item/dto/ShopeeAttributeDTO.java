package com.izhiliu.erp.service.item.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * A DTO for the ShopeeAttribute entity.
 */
@Data
@Accessors(chain = true)
public class ShopeeAttributeDTO implements Serializable {

    private static final long serialVersionUID = -8345726525650096989L;

    private Long id;

    private String name;

    private String local;

    private String chinese;

    private String attributeType;

    private String inputType;

    private Integer essential;

    private Integer sort;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private Long shopeeAttributeId;

    private Long categoryId;

    private Long shopeeCategoryId;

    private List<String> options;
}
