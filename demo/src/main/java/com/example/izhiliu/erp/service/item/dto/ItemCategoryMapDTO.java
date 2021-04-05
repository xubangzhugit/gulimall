package com.izhiliu.erp.service.item.dto;

import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;


@Data
public class ItemCategoryMapDTO extends BaseEntity {


    /**
     * 源平台ID
     */
    private Long srcPlatformId;

    /**
     * 源站点ID （如果无则为null）
     */
    private Long srcPlatfromNodeId;

    /**
     * 源类目ID
     */
    private Long srcCategroyId;

    /**
     * 类目名称
     */
    private String srcCategoryName;

    /**
     * 目标平台ID
     */
    private Long dstPlatformId;

    /**
     * 目标站点ID （如果无则为null）
     */
    private Long dstPlatfromNodeId;

    /**
     * 目标类目ID
     */
    private Long dstCategroyId;

    /**
     * 目标类目名称
     */
    private String dstCategoryName;

    /**
     * 发布成功次数
     */
    private Integer successCount;



}

