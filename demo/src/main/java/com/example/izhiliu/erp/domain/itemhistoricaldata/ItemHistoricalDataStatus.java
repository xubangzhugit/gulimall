package com.izhiliu.erp.domain.itemhistoricaldata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

@Data
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemHistoricalDataStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "itemId")
    private String itemId;

    @Column(name = "shopId")
    private String shopId;

    @Column(name = "url")
    private String url;

    @Column(name = "allSales")
    private Long allSales;

    @Column(name = "status")
    private String status;

    /**
     * 处理时间
     */
    @Column(name = "updateTime")
    private Instant updateTime;
}
