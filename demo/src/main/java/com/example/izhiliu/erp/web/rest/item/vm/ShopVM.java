package com.izhiliu.erp.web.rest.item.vm;

import lombok.Data;

import java.io.Serializable;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/25 21:08
 */

@Data
public class ShopVM implements Serializable {

    private static final long serialVersionUID = 1451795911824097516L;

    private String shopName;
    private Long shopId;
    private Integer status;
}
