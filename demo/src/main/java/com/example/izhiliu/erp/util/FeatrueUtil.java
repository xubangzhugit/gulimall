package com.izhiliu.erp.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class FeatrueUtil {
    /**
     * 新增扩展字段
     *
     * @param json   扩展字段集合
     * @param key    扩展字段的key
     * @param specId 扩展字段的value
     * @return json字符串
     */
    public static String addFeature(String json, String key, String specId) {
        Map<String, Object> map;
        if (StringUtils.isBlank(json)) {
            // 查出来是空的，直接添加
            map = new HashMap<>(3);
        if (StringUtils.isNoneBlank(key) && StringUtils.isNoneBlank(specId)) {
            map.put(key, specId);
        }
            return JSON.toJSONString(map);
        }else if (isJsonString(json)) {
            // 查出来不是空的， 判断是否为Json字符串
            map = JSON.parseObject(json).getInnerMap();
        if (StringUtils.isNoneBlank(key) && StringUtils.isNoneBlank(specId)) {
            map.put(key, specId);
            return JSON.toJSONString(map);
        }
        }else {
            //覆盖掉之前的内容
            map = new HashMap<>(3);
        if (StringUtils.isNoneBlank(key) && StringUtils.isNoneBlank(specId)) {
            return JSON.toJSONString(map.put(key,specId));
        }
        }
            return null;
    }

    /**
     * 删除扩展字段
     *
     * @param json 扩展字段集合
     * @param key  key
     * @return boolean
     */
    public static String removeFeature(String json, String key) {
            //如果是json字段,去除掉
        if (StringUtils.isNoneBlank(json) && isJsonString(json)) {
            Map<String, Object> map = JSON.parseObject(json).getInnerMap();
            if (map.containsKey(key)) {
                map.remove(key);
                return JSON.toJSONString(map);
            }
        }
            //其他情况转成json格式
            return "{}";
    }

    /**
     * 暴力解析:Alibaba fastjson
     * 判断该字符串是否是Json字符串
     *
     * @param json
     * @return
     */
    public static boolean isJsonString(String json) {
        try {
            if (null != json && StringUtils.isNoneBlank(json)) {
                JSONObject.parseObject(json);
            }
        } catch (Exception e) {
            return false;
        }
            return true;
    }


    public static JSONObject tryGetJsonObject(String json) {
        try {
            if (null != json && StringUtils.isNoneBlank(json)) {
               return JSONObject.parseObject(json);
            }
        } catch (Exception e) {
        }
        return  null;
    }
}
