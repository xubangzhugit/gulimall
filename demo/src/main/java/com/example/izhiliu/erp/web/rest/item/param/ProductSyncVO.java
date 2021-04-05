package com.izhiliu.erp.web.rest.item.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/11/30 17:53
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSyncVO {
    private Integer code;
    private Integer syncShop;
    private Integer total;
    private Integer handleTotal;
    private Integer success;
    private Integer fail;
    private List<ShopSync> shopSyncList;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ShopSync {
        private String shopId;
        private String shopName;
        private Integer total;
        private Integer handleTotal;
        private Integer success;
        private Integer fail;

    }

}
