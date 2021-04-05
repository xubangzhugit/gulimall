package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.web.rest.item.param.BaseItemQO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @Author: louis
 * @Date: 2020/9/22 10:24
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ItemSyncDTO extends BaseItemQO {
    private String taskId;
    private boolean needDeletedItem;
}
