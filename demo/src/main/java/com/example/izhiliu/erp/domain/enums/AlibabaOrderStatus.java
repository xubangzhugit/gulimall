package com.izhiliu.erp.domain.enums;

import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * describe: 1688订单状态
 * <p>
 *
 * @author cheng
 * @date 2019/4/27 14:13
 */
public enum AlibabaOrderStatus implements IEnum<Integer> {

    /**/
    WAIT_BUYER_PAY(1, "waitbuyerpay", "alibaba.wait_buyer_pay"),
    WAIT_SELLER_SEND(2, "waitsellersend", "alibaba.wait_seller_send"),
    WAIT_LOGISTICS_TAKEIN(3, "waitlogisticstakein", "alibaba.wait_logistics_takein"),
    WAIT_BUYER_RECEIVE(4, "waitbuyerreceive", "alibaba.wait_buyer_receive"),
    WAIT_BUYER_SIGN(5, "waitbuyersign", "alibaba.wait_buyer_sign"),
    SIGN_IN_SUCCESS(6, "signinsuccess", "alibaba.sign_in_success"),
    CONFIRM_GOODS(7, "confirm_goods", "alibaba.confirm_goods"),
    SUCCESS(8, "success", "alibaba.success"),
    CANCEL(9, "cancel", "alibaba.cancel"),
    TERMINATED(10, "terminated", "alibaba.terminated");

    private int id;
    private String status;
    private String message;



    private static final Map<String, AlibabaOrderStatus> CACHE_MAP = new HashMap<>();

    static {
        CACHE_MAP.put(WAIT_BUYER_PAY.status, WAIT_BUYER_PAY);
        CACHE_MAP.put(WAIT_SELLER_SEND.status, WAIT_SELLER_SEND);
        CACHE_MAP.put(WAIT_LOGISTICS_TAKEIN.status, WAIT_LOGISTICS_TAKEIN);
        CACHE_MAP.put(WAIT_BUYER_RECEIVE.status, WAIT_BUYER_RECEIVE);
        CACHE_MAP.put(WAIT_BUYER_SIGN.status, WAIT_BUYER_SIGN);
        CACHE_MAP.put(SIGN_IN_SUCCESS.status, SIGN_IN_SUCCESS);
        CACHE_MAP.put(CONFIRM_GOODS.status, CONFIRM_GOODS);
        CACHE_MAP.put(SUCCESS.status, SUCCESS);
        CACHE_MAP.put(CANCEL.status, CANCEL);
        CACHE_MAP.put(TERMINATED.status, TERMINATED);
    }

    public static AlibabaOrderStatus getByStatus(String status) {
        return CACHE_MAP.get(status);
    }

    private  static   ReportTypeServiceInjector reportTypeServiceInjector;

    @Component
    public static class ReportTypeServiceInjector {
        @Autowired
        @Qualifier(value = "handleProductExceptionInfo")
        private HandleProductExceptionInfo handleProductExceptionInfo;

        @PostConstruct
        public void  postConstruct(){
            reportTypeServiceInjector=this;
        }
    }



    AlibabaOrderStatus(int id, String status, String message) {
        this.id = id;
        this.status = status;
        this.message = message;
    }
    @JsonCreator
    AlibabaOrderStatus(AlibabaOrderStatusClass alibabaOrderStatusClass) {
        this.id = alibabaOrderStatusClass.getId();
        this.status = alibabaOrderStatusClass.getStatus();
        this.message = alibabaOrderStatusClass.getMessage();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonValue
    public AlibabaOrderStatusClass getObject() {
        final String s = reportTypeServiceInjector.handleProductExceptionInfo.doMessage(message);
        return new AlibabaOrderStatusClass().setId(id).setMessage(s).setStatus(status);
    }

    @Override
    public Integer getValue() {
        return id;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public class  AlibabaOrderStatusClass{
        private int id;
        private String status;
        private String message;
    }
}
