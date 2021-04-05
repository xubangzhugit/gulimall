package com.izhiliu.erp.web.rest.item.param;

import com.izhiliu.erp.web.rest.common.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * describe: 认领接口参数对象
 * <p>
 *
 * @author cheng
 * @date 2019/1/12 13:17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClaimParam extends BaseRequest {

    @NotBlank(message = "源数据ID不能为空")
    private String metaDataId;

    @NotNull(message = "平台ID不能为空")
    private Long platformId;
}
