package com.izhiliu.erp.domain.item;


import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Data;
import lombok.experimental.Accessors;

@TableName(value = "item_category_map")
@Accessors(chain = true)
@Data
public class ItemCategoryMap extends BEntity {


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

