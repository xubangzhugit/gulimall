package com.izhiliu.erp.web.rest.item.vm;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 15:02
 */
@Data
public class VariationMV_V2 implements Serializable {

    private static final long serialVersionUID = 1351579836920842457L;
    //属性值名称
    private String name;
    private String skuCode;
    private String currency;
    @JsonSerialize(using = ToStringSerializer.class)
    private Float price;
    @JsonSerialize(using = ToStringSerializer.class)
    private Float  originalPrice ;
    private Integer stock;
    private String specId;
    private Long productId;
    private Long shopeeItemId;
    private Long shopeeVariationId;
}
