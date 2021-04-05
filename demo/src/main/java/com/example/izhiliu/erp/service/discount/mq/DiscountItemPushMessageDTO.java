package com.izhiliu.erp.service.discount.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: louis
 * @Date: 2020/8/12 11:27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscountItemPushMessageDTO {
    public static final String TAG = "DISCOUNT_SYNC_ITEM_TAG";
    public static final String KEY = "DISCOUNT_SYNC_ITEM:";

    private String login;
    private Long shopId;
    private Long discountId;
    private Long shopeeItemId;
}
