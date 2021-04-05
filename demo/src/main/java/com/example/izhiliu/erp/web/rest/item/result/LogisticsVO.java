package com.izhiliu.erp.web.rest.item.result;

import com.izhiliu.open.shopee.open.sdk.entity.logistics.LogisticsResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2021/1/28 13:42
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogisticsVO {
    private Long shopId;
    private List<LogisticsResult.LogisticsBean> logisticsList;
}
