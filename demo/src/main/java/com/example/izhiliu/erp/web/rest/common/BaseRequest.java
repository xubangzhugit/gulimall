package com.izhiliu.erp.web.rest.common;

import com.izhiliu.core.config.security.SecurityUtils;
import lombok.Data;

import java.io.Serializable;


/**
 * describe: 通用请求对象
 * <p>
 *
 * @author cheng
 * @date 2019/1/24 9:29
 */
@Data
public abstract class BaseRequest implements Serializable {

    private String loginId;

    public BaseRequest() {
        loginId = SecurityUtils.currentLogin();
    }
}
