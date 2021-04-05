package com.izhiliu.erp.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/29 14:33
 */
public class JsonUtil {

    public static String toJSONString(Object obj) {
        if (Objects.isNull(obj)) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    public static <T> T toObject(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }

        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> List<T> toArray(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }

        try {
            return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
