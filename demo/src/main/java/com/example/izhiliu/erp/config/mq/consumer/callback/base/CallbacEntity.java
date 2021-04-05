package com.izhiliu.erp.config.mq.consumer.callback.base;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * @author Seriel
 * @create 2019-07-25 21:01
 **/
@Data
public class CallbacEntity {
    private  JSONObject data;
    private  String key;

    public CallbacEntity(JSONObject data, String key) {
        this.data = data;
        this.key = key;
    }
}
