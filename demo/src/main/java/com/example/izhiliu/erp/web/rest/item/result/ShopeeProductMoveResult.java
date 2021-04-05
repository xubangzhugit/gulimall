package com.izhiliu.erp.web.rest.item.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

/**
 * 搬家 任务返回值
 *
 * @author Seriel
 * @create 2019-09-20 13:58
 **/
@Getter
@Setter
@Accessors(chain = true)
public class ShopeeProductMoveResult {

    public final static   ShopeeProductMoveResult DEFAULT_OBJECT = new ShopeeProductMoveResult().setStatus(Integer.valueOf(-1).byteValue());

    private List<String> shopName;

    @JsonSerialize(using= ToStringSerializer.class)
    private Long taskId;

    private  int  count;

    private List<String> toShopNames;

    /**
     * //  正在搬第y个商品
     */
    private Integer currentIndex;

    /**
     *         0   (执行中)
     *         1  (完成)
     *         2  (异常)
     */
    private  byte status;

    /**
     *   错误信息
     */
    private String errorMessage;
    private List<String> errorMessages;

    /**
     *   结束时间
     */
    private Instant finshedTime;

}
