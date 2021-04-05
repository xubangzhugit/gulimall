package com.izhiliu.erp.service.itemhistoricaldata.dto;

import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemHistoricalDataStatusDTO {
    private String id;
    private String itemId;
    private String shopId;
    private String url;
    private String allSales;
}
