package com.izhiliu.erp.service.item.dto;

public interface BaseDto {

    default Long getId() {
        return null;
    }

    default void setId(Long id) {

    }
}
