package com.izhiliu.erp.web.rest.item.param;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @Author: louis
 * @Date: 2021/1/28 13:40
 */
@Data
public class LogisticsQueryQO {

    @NotEmpty
    private List<Long> shopIds;
}
