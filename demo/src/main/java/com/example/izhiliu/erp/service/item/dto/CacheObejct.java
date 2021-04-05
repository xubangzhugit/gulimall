package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.service.item.cache.impl.BoostItemCacheServiceImpl;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CacheObejct {
    public CacheObejct(String timestamp, String lock, String key, int size) {
        this.timestamp = timestamp;
        this.lock = BoostItemCacheServiceImpl.BOOST_CURSOR_PUll_KEY + lock;
        this.key = BoostItemCacheServiceImpl.BOOST_CURSOR_KEY + key;
        this.size = size;
    }

    /**
     * redis  时间戳
     */
    private String timestamp;
    /**
     * lock
     */
    private String lock;

    /**
     * key
     */
    private String key;
    /**
     * size
     */
    private int size;

}
