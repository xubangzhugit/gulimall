package com.izhiliu.erp.web.rest.item.param;

import lombok.Data;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/9/21 17:31
 */
@Data
public class ItemSyncQO extends BaseItemQO {
    private String taskId;
    private List<SyncItem> syncItems;
    private boolean needDeletedItem;

    @Data
    public static class SyncItem {
        private Long itemId;
        private Long shopId;
    }
}
