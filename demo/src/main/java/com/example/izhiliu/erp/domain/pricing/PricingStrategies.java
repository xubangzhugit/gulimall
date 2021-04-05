package com.izhiliu.erp.domain.pricing;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Id;


/**
 *
 *
 * @author pengzhen
 * @email pengzhen
 * @date 2019-10-17 15:08:37
 */
@TableName(value = "item_pricing_strategies")
@Getter
@Setter
//@Accessors(chain = true)
public class PricingStrategies extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    protected Long id;
    /**
     * 名字
     */

    private String name;
    /**
     *  商品成本
     */

    private Integer productCost;
    /**
     *  运费成本
     */

    private Integer freightCost;
    /**
     *  商品重量
     */

    private Integer productWeight;
    /**
     * 首重
     */


    private Integer firstWeight;
    /**
     * 首重价格
     */


    private Integer firstPrice;

    /**
     *  运费id
     */
    private Long logisticsId;
    /**
     * 续重
     */


    private Integer continuedWeight;
    /**
     * 续重价格
     */


    private Integer continuedWeightPrice;
    /**
     * login_id
     */

    private String login;
    /**
     * 折扣
     */


    private Integer discount;
    /**
     * 利润
     */


    private Integer profit;
    /**
     * 固定费用
     */
    private Integer fixedCost;

    /**
     * 其他费用
     */


    private Integer otherPrice;
    /**
     * 原始货币类型
     */

    private String fromCurrency;
    /**
     * 转换货币类型
     */

    private String toCurrency;
    /**
     * 是否免首重
     */


    private Boolean isFirstWeight;
    /**
     * 所属平台
     */


    private Long platformId;
    /**
     * 所属站点
     */


    private Long platformNodeId;


}
