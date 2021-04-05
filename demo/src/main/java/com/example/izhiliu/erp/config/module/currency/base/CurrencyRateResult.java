package com.izhiliu.erp.config.module.currency.base;

import lombok.Data;

import java.math.BigDecimal;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/3 16:35
 */
@Data
public class CurrencyRateResult {

    private boolean success;
    private BigDecimal rate;
    private String from;
    private String to;

    public boolean isSuccess() {
        return success;
    }
}
