package com.izhiliu.erp.Enum;

import java.util.EnumSet;

public enum  ItemHistoricalDataEnum {
    UNTREATED("untreated", "待处理", "UNTREATED"),
    PROCESSING("processing","处理中","PROCESSING"),
    COMPLETE("complete","已完成","COMPLETE"),
    OFF_SHELF("off_shelf","已下架","OFF_SHELF"),
    DELETED("deleted","删除","DELETED");

    private String code;
    private String name;
    private String status;

    ItemHistoricalDataEnum(String code, String name, String status) {
        this.code = code;
        this.name = name;
        this.status = status;
    }

    ItemHistoricalDataEnum() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static String getCommonStauts(String code) {
        return EnumSet.allOf(ItemHistoricalDataEnum.class).stream()
                .filter(e -> e.getCode().equals(code)).findFirst().get().status;
    }


    @Override
    public String toString() {
        return "ItemHistoricalDataEnum{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", handleStatus='" + status + '\'' +
                '}';
    }
}
