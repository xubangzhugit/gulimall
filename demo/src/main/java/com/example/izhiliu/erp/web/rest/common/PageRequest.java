package com.izhiliu.erp.web.rest.common;

import javax.validation.constraints.NotNull;

/**
 * @author Harry(yuzh)
 * @since 2019-01-18
 */
public class PageRequest {

    @NotNull
    private Long page;
    @NotNull
    private Long size;
    private String sort;


    public Long getPage() {
        return page == null ? 0L : page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size == null ? 20L : size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
