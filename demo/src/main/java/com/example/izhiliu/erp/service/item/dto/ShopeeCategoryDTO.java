package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.domain.enums.SyncBasicDataStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.io.Serializable;

/**
 * A DTO for the ShopeeCategory entity.
 */
@Data
@Accessors(chain = true)
public class ShopeeCategoryDTO implements Serializable {

    private static final long serialVersionUID = -3693333078821516092L;

    private Long id;

    private String name;

    private String local;

    private String chinese;

    private Integer sort;

    private Integer tier;

    private Integer hasChild;

    /**
     * 是否支持尺寸图 0:不支持 1: 支持
     */
    private Integer suppSizeChart;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private Long shopeeCategoryId;

    private Long shopeeParentId;

    private Long parentId;

    private Long platformNodeId;

    private SyncBasicDataStatus status;

    private Instant lastSyncTime;

    private Integer deleted;
}
