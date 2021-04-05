package com.izhiliu.erp.common;

/**
 * @author Harry(yuzh)
 * @since 2019-01-19
 */
public enum ShopeeSite {

    MY("MY", "马来西亚"),
    SG("SG", "新加坡"),
    ID("ID", "印度尼西亚"),
    TW("TW", "台湾"),
    TH("TH", "泰国"),
    BR("BR", "巴西");


    ShopeeSite(String country, String description) {
        this.country = country;
        this.description = description;
    }

    private String country;
    private String description;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
