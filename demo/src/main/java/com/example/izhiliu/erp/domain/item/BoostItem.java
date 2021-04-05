package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.izhiliu.core.domain.common.BEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;


@Getter
@Setter
@Accessors(chain = true)
@TableName(value="item_shopee_boost")
public class BoostItem extends BEntity implements Serializable {


    /**
     * 用户
     */
    private String login;

    /**
     * 商品id
     */
    private Long productId;

    /**
     * 所属店铺
     */
    private Long shopId;

    /**
     * 所属平台
     */
    private Long platformId;

    /**
     * 所属站点
     */
    private Long platformNodeId;


    /**
     * shopee 商品ID
     */
    private Long shopeeItemId;

    /**
     * 状态
     *  0  待置顶
     *  1  准备定时置顶
     *  2  正在定时置顶
     *  3  置顶完毕
     */
    private Byte status;


     public  enum  Status {
         NO_BOOST(0),
         START_BOOST(1),
         CUCCENT_BOOST(2),
         SHUTDOWN_BOOST(3),
         INVALID_BOOST(4);

         @JsonValue
         private int code;

         Status(int code) {
             this.code = code;
         }

         public int getCode() {
             return code;
         }
         public static Status getStatus(Integer code){
             for (Status value : Status.values()) {
                 if( code == value.code){
                     return  value;
                 }
             }
             return  null;
         }
     }

    private static final long serialVersionUID = 1L;
}

