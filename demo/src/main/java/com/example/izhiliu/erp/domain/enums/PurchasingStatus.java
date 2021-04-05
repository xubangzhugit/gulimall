package com.izhiliu.erp.domain.enums;

import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 采购建议状态
 */
public enum PurchasingStatus implements IEnum<Integer> {
    /**/
    PENDING(1, "purchasing.status.pending"),
    COMPLETE(2, "purchasing.status.complete");

    private Integer code;
    private String msg;

    private  static ReportTypeServiceInjector reportTypeServiceInjector;
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


    PurchasingStatus(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
    @JsonValue
    public String getMsg() {
        return reportTypeServiceInjector.handleProductExceptionInfo.doMessage(msg);
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    @Override
    public Integer getValue() {
        return code;
    }
}
