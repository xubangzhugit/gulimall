package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import com.izhiliu.erp.domain.enums.SyncBasicDataStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * A PlatformNode.
 */
@TableName("item_platform_node")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class PlatformNode extends BaseEntity {

    private static final long serialVersionUID = 9134069887113478593L;

    private String name;

    private String url;

    private String currency;

    private String language;

    private Long platformId;

    private String code;

    private SyncBasicDataStatus status;

    private Instant lastSyncTime;

    private String globalLanguage;
}
