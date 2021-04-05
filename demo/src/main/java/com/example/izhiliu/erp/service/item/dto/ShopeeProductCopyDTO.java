package com.izhiliu.erp.service.item.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: louis
 * @Date: 2020/8/14 16:36
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopeeProductCopyDTO {
    private Long productId;
    private Long copyOfProductId;
    private Long discountActivityId;
    private String toCurrency;
    private boolean toTw;

}
