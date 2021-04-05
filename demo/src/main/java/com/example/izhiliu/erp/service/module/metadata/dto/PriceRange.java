package com.izhiliu.erp.service.module.metadata.dto;

import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PriceRange implements Serializable {

    private static final long serialVersionUID = -1;

    private int min;
    private int max;
    private float price;
}
