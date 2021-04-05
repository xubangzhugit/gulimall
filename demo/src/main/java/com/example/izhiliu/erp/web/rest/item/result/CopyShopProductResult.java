package com.izhiliu.erp.web.rest.item.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * describe:
 * 批处理执行结果
 *
 * <p>
 *
 * @author cheng
 * @date 2019/4/8 15:10
 */
@Data
@Accessors(chain = true)
public class CopyShopProductResult {

    private boolean success;

    private boolean publish = false;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long fromId;

    private Long shopId;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long newId;
    private String message;

    private CopyShopProductResult () {}

    public static CopyShopProductResult ok() {
        CopyShopProductResult result = new CopyShopProductResult();
        result.setSuccess(true);
        return result;
    }

    public static CopyShopProductResult error() {
        CopyShopProductResult result = new CopyShopProductResult();
        result.setSuccess(false);
        return result;
    }
}
