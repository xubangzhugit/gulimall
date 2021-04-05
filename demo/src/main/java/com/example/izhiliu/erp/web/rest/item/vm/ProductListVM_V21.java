package com.izhiliu.erp.web.rest.item.vm;

import com.izhiliu.erp.domain.enums.enumsclasses.LocalProductStatusClass;
import com.izhiliu.erp.domain.enums.enumsclasses.ShopeeItemStatusClass;
import lombok.Data;

/**
 * describe:
 * <p>
 * 用于平滑升级 状态值
 *
 * @author cheng
 * @date 2019/2/19 15:04
 */
@Data
public class ProductListVM_V21 extends ProductListVM_V2 {


    private LocalProductStatusClass newStatus;

    private ShopeeItemStatusClass newShopeeItemStatus;


    private String startDateTime;

    private String endDateTime;


    private Byte toppingStatusCode;


    private Boolean boost = Boolean.FALSE;


    private  int likes;


    private  int  views;

    /**
     *  是否二手
     */
    private boolean isCondition;
    /**
     * 销售量
     */
    private int sales;

    /**
     * 评论数
     */
    private int cmtCount  ;

    /**
     * 星级评分
     */
    private  float ratingStar  ;
}
