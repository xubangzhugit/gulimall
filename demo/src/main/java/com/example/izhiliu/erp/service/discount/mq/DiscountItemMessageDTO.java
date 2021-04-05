package com.izhiliu.erp.service.discount.mq;

import com.izhiliu.erp.domain.item.ShopeeProduct;
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
public class DiscountItemMessageDTO {
    public static final String TAG = "DISCOUNT_SYNC_TAG";
    public static final String KEY = "DISCOUNT_SYNC:";

    private String login;
    private Long shopId;
    private Long productId;
    private Long shopeeItemId;
}
