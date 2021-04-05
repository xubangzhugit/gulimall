package com.izhiliu.erp.web.rest.itemhistoricaldata.vm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemHistoricalDataVM {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name= "itemId")
    private String itemId;

    @Column(name= "minPrice")
    private String minPrice;

    @Column(name= "maxPrice")
    private String maxPrice;

    @Column(name= "gmtDate")
    private String gmtDate;

    @Column(name= "updateTime")
    private Instant updateTime;

    @Column(name= "daySales")
    private Long daySales;

    @Column(name = "allSales")
    private Long allSales;

    private Integer avgDaySales;
}
