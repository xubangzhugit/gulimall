package com.izhiliu.erp.service.item.dto;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the ShopeeProductSku entity.
 */
@Data
@Accessors(chain = true)
public class ShopeeProductSkuDTO implements Serializable {

    private static final long serialVersionUID = 721661357199723935L;

    private Long id;

    private String skuCode;

    /**
     * 售价
     */
    private Float price;

    private Long collectPrice;

    @TableLogic
    private  Integer deleted;

    private String currency;

    private Integer stock;

    private String image;

    private Integer sendOutTime;

    private Instant gmtCreate;

    private Instant gmtModified;

    private String feature;

    private Integer skuOptionOneIndex;

    private Integer skuOptionTowIndex;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long productId;

    private Long shopeeVariationId;

    private Integer sold;

    /**
     * 原价
     */
    private  Float originalPrice;

    /**
     * 折后价
     */
    private  Float discount;

    private  Long discountId;
}
