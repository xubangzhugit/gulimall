package com.izhiliu.erp.service.item.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the ShopeeProductImage entity.
 */
@Data
@Accessors(chain = true)
public class UserImageDTO implements Serializable {

    private static final long serialVersionUID = -881636794791134937L;

    private Long id;

    private String url;

    private String filename;

    private Long size;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private String loginId;
}
