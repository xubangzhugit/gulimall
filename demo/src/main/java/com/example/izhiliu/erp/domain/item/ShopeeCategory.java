package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import com.izhiliu.erp.domain.enums.SyncBasicDataStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * A ShopeeCategory. 类目需要软删除
 */
@TableName("item_shopee_category")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ShopeeCategory extends BaseEntity {

    private static final long serialVersionUID = -7590546209405755498L;

    private String name;

    private String local;

    private String chinese;

    private Integer sort;

    private Integer tier;

    private Integer hasChild;

    private Long shopeeCategoryId;

    /**
     * 是否支持尺寸图 0:不支持 1: 支持
     */
    private Integer suppSizeChart;

    private Long shopeeParentId;

    private Long parentId;

    private Long platformNodeId;

    private SyncBasicDataStatus status;

    private Instant lastSyncTime;
}
