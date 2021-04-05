package com.izhiliu.erp.web.rest.discount.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @Author: louis
 * @Date: 2020/8/4 13:42
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscountDeatilVO {
    private String login;

    private String shopId;

    private String shopName;

    /**
     * 活动id
     */
    private String discountId;

    /**
     * 活动名称
     */
    private String discountName;
    /**
     * 状态:
     * expired:已过期;
     * ongoing:进行中;
     * upcoming:即将来临;
     */
    private String status;

    private Instant startTime;

    private Instant endTime;

    private long itemCount;
}
