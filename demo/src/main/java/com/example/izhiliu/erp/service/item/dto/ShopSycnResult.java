package com.izhiliu.erp.service.item.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ShopSycnResult {
    private int ifSync;

    private int shopCount;

    private List<ShopSycnList> shopSycnLists = new ArrayList<>();

    public ShopSycnResult() {

    }
    public ShopSycnResult(int ifSync) {
        this.ifSync = ifSync;
    }

    @Data
    public static class ShopSycnList{
        private Long shopId;

        private String shopName;

        private int count;

        private int succeedCount;

        private int loserCount;

        private int succeedRatio;

        private int status;

        private String msg;
        private List<ShopeeSyncDTO.LoserDetails> loserDetails = new ArrayList<>();

        public ShopSycnList() {

        }


        public ShopSycnList(Long shopId, String shopName, int count, int succeedCount, int loserCount, int succeedRatio, List<ShopeeSyncDTO.LoserDetails> loserDetails, int status, String msg) {
            this.shopId = shopId;
            this.shopName = shopName;
            this.count = count;
            this.succeedCount = succeedCount;
            this.loserCount = loserCount;
            this.succeedRatio = succeedRatio;
            this.loserDetails = loserDetails;
            this.status = status;
            this.msg = msg;
        }
    }
}
