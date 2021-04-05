package com.izhiliu.erp.service.discount.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * @Author: louis
 * @Date: 2020/8/3 15:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopeeDiscountDetailDTO implements Serializable {
    private Long id;

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
}
