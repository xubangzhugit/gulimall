package com.izhiliu.erp.domain.enums;

/**
 * @Author: louis
 * @Date: 2020/8/12 15:59
 */
public enum DiscountStatus {
    EXPIRED("expired", "已过期"),
    ONGOING("ongoing", "进行中"),
    UPCOMING("upcoming", "即将到来"),
    ;

    DiscountStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String code;
    private String name;
}
