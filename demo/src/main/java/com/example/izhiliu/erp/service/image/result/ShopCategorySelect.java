package com.izhiliu.erp.service.image.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShopCategorySelect {



    /**
     *  要查询的 店铺
     */
     private  List<Long> shops;

     private  int page = 1;

     private  int  size = 10;

    /**
     *  要查询的 状态
     */
    private   Integer  status;

    /**
     *  要查询的列
     */
    private  Integer field;

    /**
     *  要查询的列
     */
    @JsonIgnore
    private  String sqlField;

    @JsonIgnore
    private  String[] sqlFieldString = {"","shop_category_id","name"};

    /**
     *  要 查询的 值
     */
     private  Object  keyword;


     //============================================
    /***
     *   类目id
     */
    private Long cateagoryId;

    /***
     *    商品Itemid
     */
    private Long productItemId;

}
