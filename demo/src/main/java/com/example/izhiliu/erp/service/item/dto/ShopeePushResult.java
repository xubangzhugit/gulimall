package com.izhiliu.erp.service.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: louis
 * @Date: 2020/7/16 16:08
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopeePushResult {
    private Long productId;
    private String pushType;
    private String errorMessage;
}
