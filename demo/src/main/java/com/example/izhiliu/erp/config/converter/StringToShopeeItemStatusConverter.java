package com.izhiliu.erp.config.converter;

import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/21 14:54
 */
@Component
public class StringToShopeeItemStatusConverter implements Converter<String, ShopeeItemStatus> {

    @Override
    public ShopeeItemStatus convert(String input) {
        return ShopeeItemStatus.CACHE_MAP.get(input);
    }
}
