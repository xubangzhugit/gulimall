package com.izhiliu.erp.service.discount.mq;

import com.izhiliu.erp.web.rest.discount.qo.DiscountQO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/6 15:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscountDetailMessageDTO {
    private String login;
    private String taskId;
    private List<DiscountQO.SyncParam> syncParam;
}
