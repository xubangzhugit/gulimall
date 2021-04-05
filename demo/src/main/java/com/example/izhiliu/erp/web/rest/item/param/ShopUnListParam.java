package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.core.common.ValidList;
import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/21 17:24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShopUnListParam extends BaseRequest {

    @Valid
    private ValidList<ShopProductParam> params;

    @NotNull(message = "上下架标识不能为空")
    private Boolean unlist;
}
