package com.izhiliu.erp.web.rest.item.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/8/3 16:49
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BaseItemQO {
    private String login;
    private Long shopId;
    private List<Long> shopIds;
    private Long shopeeItemId;
}

