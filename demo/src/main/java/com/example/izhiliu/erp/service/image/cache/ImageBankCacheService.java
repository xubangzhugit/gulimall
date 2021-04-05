package com.izhiliu.erp.service.image.cache;


import com.izhiliu.erp.service.image.dto.ImageBankCapacityCacheObejct;
import com.izhiliu.erp.service.item.cache.RedisService;
import com.izhiliu.model.SubscribeSuccessDTO;

public interface ImageBankCacheService  extends RedisService<ImageBankCapacityCacheObejct, String> {
    void minus(long newRemainingMemory, String currentLogin);

    boolean cleanCache(SubscribeSuccessDTO dto);
}
