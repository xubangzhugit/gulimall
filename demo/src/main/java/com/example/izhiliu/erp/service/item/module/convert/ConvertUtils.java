
package com.izhiliu.erp.service.item.module.convert;

import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 转换用的工具包
 *
 * @author Seriel
 * @create 2019-08-01 17:13
 **/
@Slf4j
public final class ConvertUtils {

    /**
     * @param descImages 选择的对象
     * @param product    主要填充的 对象
     */
    static void fillImage(@NonNull List<String> descImages, @NonNull ShopeeProductDTO product) {
        List<String> images = product.getImages();
        if (!images.isEmpty()) {
            final int size = images.size();
            if (size < 9) {
                if (!descImages.isEmpty()) {
                    int i = 9 - size;
                    final Iterator<String> iterator = descImages.iterator();
                    while (iterator.hasNext() && i != 0) {
                        images.add(iterator.next());
                        --i;
                    }
                }
            } else {
                images = images.stream().limit(9).collect(Collectors.toList());
            }
            product.setImages(images);
        }

        if (log.isDebugEnabled()) {
            log.debug(" ConvertUtils  fillImage size  [{}]", images.size());
        }
    }

    static String generate(String... skuNames) {
        final String cdkey = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6).toUpperCase() + "-" ;
        return Arrays.stream(skuNames).filter(skuName -> Objects.nonNull(skuNames)).collect(Collectors.joining("-", cdkey, ""));
    }

}
