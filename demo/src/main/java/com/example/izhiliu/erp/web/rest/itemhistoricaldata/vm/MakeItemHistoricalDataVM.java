package com.izhiliu.erp.web.rest.itemhistoricaldata.vm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 制造历史动销数据专用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MakeItemHistoricalDataVM {
    private Long day;
    private Instant beginTime;
    private Instant updateTime;
    private Long avgDaySales;
    private Long allSales;
    private Long daySales;
    private Double minPrice;
    private Double maxPrice;
}
