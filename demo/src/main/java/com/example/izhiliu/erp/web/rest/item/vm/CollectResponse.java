package com.izhiliu.erp.web.rest.item.vm;

import java.io.Serializable;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/3/1 14:31
 */
public class CollectResponse implements Serializable {

    private String requestId;
    private boolean success;
    private String error;

    private CollectResponse(String requestId, boolean success) {
        this.requestId = requestId;
        this.success = success;
    }

    private CollectResponse(String requestId, boolean success, String error) {
        this.requestId = requestId;
        this.success = success;
        this.error = error;
    }

    public static CollectResponse ok(String requestId) {
        return new CollectResponse(requestId, true);
    }

    public static CollectResponse error(String requestId, String error) {
        return new CollectResponse(requestId, false, error);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
