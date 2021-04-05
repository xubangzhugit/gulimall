package com.izhiliu.erp.service.module.metadata.basic;

import lombok.Data;

import java.io.Serializable;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/25 15:38
 */
@Data
public abstract class BaseMetaData implements Serializable {
    private static final long serialVersionUID = -7825609526698664333L;

    protected Integer platformId;
    protected String json;
    protected String loginId;
    protected String url;
    private Long itemId;

    public BaseMetaData() {
    }

    public BaseMetaData(Integer platformId, String json, String loginId, String url) {
        this.platformId = platformId;
        this.json = json;
        this.loginId = loginId;
        this.url = url;
    }
}
