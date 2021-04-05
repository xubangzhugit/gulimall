package com.izhiliu.erp.service.image.dto;


import java.time.Instant;
import java.util.Collections;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.izhiliu.erp.service.item.dto.BaseDto;
import com.izhiliu.erp.web.rest.item.vm.BatchEditProductVM;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V21;
import io.swagger.annotations.ApiModelProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * @author pengzhen
 * @email pengzhen
 * @date 2019-11-20 10:51:56
 */
@Getter
@Setter
@Accessors(chain = true)
public class ShopeeShopCategoryDTO  implements BaseDto {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private  Long id;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private String name;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private Integer sort;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private String loginId;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private Long shopCategoryId;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private Byte hasChild;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private Instant gmtCreate;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private Instant gmtModified;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private Integer deleted;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private String feature;

    /**
     */
    private Integer status;
    /**
     * 最后异步的  序列号
     */
    @ApiModelProperty(value = " 最后异步的  序列号", example = "null")
    private Long lastSyncIndex;
    /**
     *
     */
    @ApiModelProperty(value = "", example = "null")
    private Long shopId;

    private String shopName;


    /**
     * 现在拥有在下面的商品数量
     */
    private  int productCount;

    /**
     * 现在拥有在下面的商品数量
     */
    private  List<ProductListVM_V21> batchGetProduct = Collections.emptyList();

    @JsonProperty(value = "isSave")
    private Boolean  isSave ;

    @JsonSerialize(using = ToStringSerializer.class)
    private List<Long> productIds;

    /**
     *   新增时候的  id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private List<Long> shopIds;

    public ShopeeShopCategoryDTO() {
    }





    @Override
    @SneakyThrows
    public ShopeeShopCategoryDTO clone() {
        return (ShopeeShopCategoryDTO) super.clone();
    }

}
