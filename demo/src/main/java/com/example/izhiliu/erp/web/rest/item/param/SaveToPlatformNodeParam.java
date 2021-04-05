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
 * @date 2019/1/26 17:06
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaveToPlatformNodeParam extends BaseRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "平台站点ID不能为空")
    private Long platformNodeId;
}
