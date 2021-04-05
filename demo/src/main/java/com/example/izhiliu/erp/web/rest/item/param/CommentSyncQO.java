package com.izhiliu.erp.web.rest.item.param;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Twilight
 * @date 2021/2/7 14:48
 */
@Data
public class CommentSyncQO {

    private String login;

    @NotEmpty(groups = {SyncShopCheck.class})
    private List<Long> shopIdList;

    @NotEmpty
    private List<ItemInfo> itemInfoList;


    @Data
    public static class ItemInfo{

        @NotNull
        private Long shopId;

        @NotNull
        private Long itemId;
    }

    /**同步店铺评论参数验证*/
    public interface SyncShopCheck {
    }
}
