package com.izhiliu.erp.service.discount.dto;


import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class ShopeeDiscountParamDto {

    /**
     *   AddDiscountItem   yes
     */
    @NotNull(groups = AddDiscountItem.class)
    @NotNull(groups = DeleteDiscount.class)
    private Long discountId;
    /**
     *    AddDiscount  yes
     */
    @NotNull(groups = AddDiscount.class)
    private String discountName;

    @NotNull(groups = AddDiscount.class)
    private Long startTime;

    @NotNull(groups = AddDiscount.class)
    private Long endTime;

    @NotNull(groups = AddDiscount.class)
    @NotNull(groups = DeleteDiscount.class)
    private long shopId;

    /**
     *  非必须    no
     */
    @Valid
    private List<DiscountProductDto> items;


    public interface  AddDiscountItem{

    }
    public interface  AddDiscount{

    }

    public interface  DeleteDiscount{

    }
}
