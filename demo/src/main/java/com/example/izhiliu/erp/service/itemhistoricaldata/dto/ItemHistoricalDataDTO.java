package com.izhiliu.erp.service.itemhistoricaldata.dto;

import com.izhiliu.erp.web.rest.itemhistoricaldata.vm.ItemHistoricalDataVM;
import lombok.*;

import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemHistoricalDataDTO {
    private String itemId;
    private String shopId;
    private String url;
    private Long allSales;
    private List<ItemHistoricalDataVM> data;
    private String status;
}
