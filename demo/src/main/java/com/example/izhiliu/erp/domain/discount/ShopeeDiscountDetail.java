package com.izhiliu.erp.domain.discount;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseNoLogicEntity;
import lombok.Data;

import java.time.Instant;

/**
 * @Author: louis
 * @Date: 2020/8/3 15:52
 */
@TableName("shopee_discount_detail")
@Data
public class ShopeeDiscountDetail extends BaseNoLogicEntity {
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
