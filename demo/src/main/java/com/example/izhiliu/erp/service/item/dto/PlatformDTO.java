package com.izhiliu.erp.service.item.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the Platform entity.
 * @author cheng
 */
@Data
@Accessors(chain = true)
public class PlatformDTO implements Serializable {

    private static final long serialVersionUID = 6148881690530300605L;

    private Long id;

    private String name;

    private String description;

    private String url;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private Integer canClaim;
}
