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
 * @date 2019/4/9 11:40
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaveToShop extends BaseRequest {
    private static final long serialVersionUID = -1265959469239638205L;

    @NotNull
    private Long productId;
    @NotEmpty
    private List<Long> shopIds;
}
