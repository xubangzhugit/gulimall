package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * describe: 批量拷贝到站点
 * <p>
 *
 * @author cheng
 * @date 2019/2/13 13:41
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchCopyToPlatformNodeParam extends BaseRequest {

    @NotNull(message = "平台站点ID不能为空")
    private Long platformNodeId;

    @NotEmpty(message = "至少要有一个商品ID")
    private List<Long> productIds;

    private Boolean shop = false;
}
