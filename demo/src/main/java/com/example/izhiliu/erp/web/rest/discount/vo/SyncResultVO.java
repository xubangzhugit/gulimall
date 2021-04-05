package com.izhiliu.erp.web.rest.discount.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: louis
 * @Date: 2020/8/6 10:00
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncResultVO {
    private String taskId;
    private Integer count;
}
