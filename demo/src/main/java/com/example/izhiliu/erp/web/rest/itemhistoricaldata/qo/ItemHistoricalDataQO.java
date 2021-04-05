package com.izhiliu.erp.web.rest.itemhistoricaldata.qo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemHistoricalDataQO {
    private String id;

    /**
     * 商品id
     */
    private String itemId;

    private String shopId;

    private Float minPrice;

    private Float maxPrice;

    /**
     * 当天销售量
     */
    private Long daySales;

    /**
     * 总销售量
     */
    private Long allSales;


    private String url;

    /**
     * 商品历史价格更新时间
     */
    private Instant updateTime;

    /**
     * 指定开始时间，应该早于当前时间
     */
    private Instant beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+0")
    private LocalDateTime beginDateTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+0")
    private LocalDateTime endDateTime;

    /**
     * 下架标志：0为非下架，1为下架
     */
    private String status;


}
