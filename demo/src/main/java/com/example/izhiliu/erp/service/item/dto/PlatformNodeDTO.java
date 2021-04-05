package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.domain.enums.SyncBasicDataStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;

/**
 * A DTO for the PlatformNode entity.
 */
@Data
@Accessors(chain = true)
public class PlatformNodeDTO implements Serializable {

    private static final long serialVersionUID = -9078591429973874086L;

    private Long id;

    private String name;

    private String currency;

    private String url;

    private String language;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private Long platformId;

    private String code;

    private SyncBasicDataStatus status;

    private Instant lastSyncTime;

    private String globalLanguage;
}
