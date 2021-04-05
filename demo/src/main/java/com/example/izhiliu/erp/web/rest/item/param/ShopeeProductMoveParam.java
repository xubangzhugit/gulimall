package com.izhiliu.erp.web.rest.item.param;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 店铺搬家的 参数类
 *
 * @author Seriel
 * @create 2019-09-20 13:56
 **/
@Getter
@Setter
@Accessors(chain = true)
public class ShopeeProductMoveParam {


    @NotEmpty(groups = {Insert.class})
    @NotNull(groups = {Insert.class})
    private List<Long> productIds;

    @NotEmpty(groups = {Insert.class})
    @NotNull(groups = {Insert.class})
    private List<Long> toShopIds;

/////////////////////////////////////////////////////

    @NotEmpty(groups = {Delete.class})
    @NotNull(groups = {Delete.class})
    private List<Long> taskId;


    public interface Insert {

    }

    public interface Delete {

    }

}
