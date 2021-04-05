package com.izhiliu.erp.domain.enums;

import com.baomidou.mybatisplus.core.enums.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * describe: 同步基础数据状态
 * <p>
 *
 * @author cheng
 * @date 2019/2/11 10:51
 */
public enum SyncBasicDataStatus implements IEnum<Integer> {
    /**
     *
     */
    SYNC_FAIL(0, "sync.basic.data.status.sync.fail"),
    SYNC_SUCCESS(1, "sync.basic.data.status.sync.success"),
    SYNC(2, "sync.basic.data.status.sync");
    static    ReportTypeServiceInjector  reportTypeServiceInjector;
    public Integer key;


    public String val;

    SyncBasicDataStatus(Integer key, String val) {
        this.key = key;
        this.val = val;
    }

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

    @JsonValue
    public String getVal() {
        return reportTypeServiceInjector.handleProductExceptionInfo.doMessage(val);
    }

    @Override
    public Integer getValue() {
        return key;
    }}
