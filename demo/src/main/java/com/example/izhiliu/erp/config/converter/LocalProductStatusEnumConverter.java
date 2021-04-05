package com.izhiliu.erp.config.converter;


import com.google.common.collect.Maps;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import org.springframework.core.convert.converter.Converter;

import java.util.Map;
import java.util.Objects;

/**
 *   { }
 */
public class LocalProductStatusEnumConverter implements Converter<String, LocalProductStatus> {
    private Map<Object, LocalProductStatus> enumMap = Maps.newHashMap();

    public LocalProductStatusEnumConverter() {
        for (LocalProductStatus genderEnum : LocalProductStatus.values()) {
            enumMap.put(genderEnum.status, genderEnum);
        }
        for (LocalProductStatus genderEnum : LocalProductStatus.values()) {
            enumMap.put(genderEnum.code+"", genderEnum);
        }
        enumMap.put("操作失败", LocalProductStatus.PUBLISH_FAILURE);
        enumMap.put("操作成功", LocalProductStatus.PUBLISH_SUCCESS);
    }

    @Override
    public LocalProductStatus convert(String source) {
        LocalProductStatus genderEnum = enumMap.get(source);
        if (Objects.isNull(genderEnum)) {
//            throw new IllegalArgumentException("无法匹配对应的枚举类型");
            return  null;
        }
        return genderEnum;
    }
}

