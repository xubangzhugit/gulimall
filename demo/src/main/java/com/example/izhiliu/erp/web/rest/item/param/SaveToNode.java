package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/9 13:50
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaveToNode extends BaseRequest {
    private static final long serialVersionUID = -6765506909961674254L;

    @NotNull(message = "平台站点ID不能为空")
    private Long nodeId;

    @NotEmpty(message = "至少要有一个商品ID")
    private List<Long> productIds;
}
