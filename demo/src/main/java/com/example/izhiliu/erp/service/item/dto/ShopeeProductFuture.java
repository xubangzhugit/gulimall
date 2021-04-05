package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.web.rest.item.param.BaseTaskQO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: louis
 * @Date: 2020/7/27 11:17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShopeeProductFuture {
    private Long productId;
    private String errorImage;
    private String errorMessage;
    private List data;
    private int success;
    private int total;
    private int fail;
}
