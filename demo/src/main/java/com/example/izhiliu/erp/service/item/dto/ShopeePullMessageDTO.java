package com.izhiliu.erp.service.item.dto;

import com.izhiliu.open.shopee.open.sdk.api.item.result.GetItemListResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/9/25 11:03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopeePullMessageDTO {
    public static final String TAG = "SHOPEE_ACTION_PULL_SHOP";
    /**
     * 处理总数
     */
    public final static String HANDLE_TOTAL = "handleTotal";


    private String login;
    private String taskId;
    //跳过删除：true:跳过; false:不跳过;
    private boolean skipRemove;
    private Long shopId;
    private List<GetItemListResult.ItemsBean> items;
}
