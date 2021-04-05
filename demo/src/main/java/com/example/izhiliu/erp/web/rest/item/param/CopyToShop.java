package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/8 14:12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CopyToShop extends BaseRequest {

    @NotNull
    private Long shopProductId;
    @NotNull
    private Long shopId;
    @NotNull
    private Long platformNodeId;
}
