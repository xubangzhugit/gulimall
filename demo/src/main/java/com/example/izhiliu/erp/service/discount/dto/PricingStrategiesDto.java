package com.izhiliu.erp.service.discount.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.erp.service.item.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 *
 *
 * @author pengzhen
 * @email pengzhen
 * @date 2019-10-17 15:08:37
 */
@Getter
@Setter
public class PricingStrategiesDto  implements BaseDto {
    private static final long serialVersionUID = 1L;

    /**
     *
     */

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /**
     * 名字
     */
    @NotNull
    @NotBlank
    @Length(min=0, max=100)
    private String name;
    /**
     *  商品重量
     */

    private Float productWeight;
    /**
     *  商品成本
     */

    private Float productCost;
    /**
     *  运费成本
     */

    private Float freightCost;
    /**
     * 首重
     */
    private Float firstWeight;
    /**
     * 首重价格
     */
    private Float firstPrice;
    /**
     * 续重
     */
    private Float continuedWeight;
    /**
     * 续重价格
     */
    private Float continuedWeightPrice;
    /**
     * login_id
     */
    private String login;

    /**
     *  运费id
     */
    @JsonProperty("logisticsType")
    @NotNull
    private Long logisticsId;

    /**
     * 折扣
     */
    private Float discount;
    /**
     * 利润
     */
    private Float profit;
    /**
     *   固定费用
     */
    private Float fixedCost;
    /**
     * 其他费用
     */
    private Float otherPrice;
    /**
     * 原始货币类型
     */
    @NotNull
    @NotBlank
    @Length(min=0, max=100)
    private String fromCurrency;
    /**
     * 转换货币类型
     */

    private String toCurrency;
    /**
     * 是否免首重
     */
    @NotNull
    private Boolean isFirstWeight;
    /**
     * 所属平台
     */
    @NotNull
    @Min(value=0)
    private Long platformId;
    /**
     * 所属站点
     */
    @NotNull
    @Min(value=0)
    private Long platformNodeId;

    /**
     *
     */

    @JSONField(format = "yyyy-MM-dd")
    private Instant gmtCreate;
    /**
     *
     */
    @JSONField(format = "yyyy-MM-dd")
    private Instant gmtModified;


}
