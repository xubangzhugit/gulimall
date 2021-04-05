package com.izhiliu.erp.service.image.cache.impl;

import com.izhiliu.erp.repository.image.ImageBankAddressRepository;
import com.izhiliu.erp.repository.item.UserImageRepository;
import com.izhiliu.erp.service.image.cache.ImageBankCacheService;
import com.izhiliu.erp.service.image.dto.ImageBankCapacityCacheObejct;
import com.izhiliu.feign.client.DariusService;
import com.izhiliu.model.SubscribeSuccessDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用来多服务器 进行 同步下标
 */
@Service
@Slf4j
public class ImageBankCacheServiceImpl implements ImageBankCacheService {





    public final static String IMAGE_BANK_KEY = "lux:image:bank:";
    //1024L * 1024 * 1000
    public final static long DIAMOND_SIZE = 1048576000;
    public final static long INIT_USED_SIZE = 0L;


    @Resource
    DariusService  dariusService;
    StringRedisTemplate stringRedisTemplate;
    ImageBankAddressRepository imageBankAddressService;
    UserImageRepository userImageRepository;

    public ImageBankCacheServiceImpl(StringRedisTemplate stringRedisTemplate, ImageBankAddressRepository imageBankAddressService,UserImageRepository userImageRepository,DariusService  dariusService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.imageBankAddressService = imageBankAddressService;
        this.userImageRepository=userImageRepository;
        this.dariusService=dariusService;
    }


    @Override
    public ImageBankCapacityCacheObejct doPull(Optional<String> supplier) {
        final Map<Object, Object> entries = stringRedisTemplate.boundHashOps(getKey(supplier.get())).entries();
        if (!entries.isEmpty()) {
             Long total = MapUtils.getLongValue(entries, "total");
             if(Objects.isNull(total) || total.compareTo(0L) == 0){
                 total = getTotal(supplier.get());
             }
            final long used = MapUtils.getLongValue(entries, "used");
            return new ImageBankCapacityCacheObejct().setLoginId(supplier.get()).setTotal(total).setUsed(used);
        }
        return doPush(supplier);
    }

    private String getKey(String oldKey) {
        return IMAGE_BANK_KEY+oldKey;
    }



    @Override
    public ImageBankCapacityCacheObejct doPush(Optional<String> supplier) {
        final BoundHashOperations<String, String, String> stringObjectObjectBoundHashOperations = stringRedisTemplate.boundHashOps(getKey(supplier.get()));
        //  pictureSpace  来自 darius
         Long pictureSpace = getTotal(supplier.get());
        if(Objects.isNull(pictureSpace)){
            pictureSpace = DIAMOND_SIZE;
        }
        final Long size = imageBankAddressService.selectImageSizeByUserId(supplier.get());
        //  旧的数据
//        final Long oldSize = userImageRepository.selectImageSizeByUserId(supplier.get());
        long initSize = Objects.isNull(size) ? INIT_USED_SIZE : size;
//          initSize = initSize + (Objects.isNull(oldSize) ? 0 : oldSize);
        stringObjectObjectBoundHashOperations.put("used",String.valueOf(initSize));
        stringObjectObjectBoundHashOperations.expire(30,TimeUnit.DAYS);
        return  new ImageBankCapacityCacheObejct().setLoginId(supplier.get()).setTotal(pictureSpace).setUsed(initSize);
    }

    protected Long getTotal(String login) {
        try {
            final ResponseEntity<Map<String, Long>> mapResponseEntity = dariusService.queryLimitFeatureSpecial(login, "pictureSpace");
            final Long pictureSpace = mapResponseEntity.getBody().getOrDefault("pictureSpace", DIAMOND_SIZE);
            stringRedisTemplate.boundHashOps(getKey(login)).putIfAbsent("total",pictureSpace+"");
            return pictureSpace;
        } catch (Exception e) {
          log.error(e.getMessage(),e);
        }
        return null;
    }


    /**
     * 重写
     *
     * @return
     */
    @Override
    public Optional<ImageBankCapacityCacheObejct> run(Optional<String> supplier)   {
        return pull(supplier);
    }


    @Override
    public Optional<ImageBankCapacityCacheObejct> pull(Optional<String> supplier) {
        return Optional.ofNullable(doPull(supplier));
    }

    @Override
    public void minus(long newRemainingMemory, String currentLogin) {
        final BoundHashOperations<String, String, String> stringObjectObjectBoundHashOperations = stringRedisTemplate.boundHashOps(getKey(currentLogin));
        stringObjectObjectBoundHashOperations.increment("used",newRemainingMemory);
    }

    @Override
    public boolean cleanCache(SubscribeSuccessDTO dto) {
        return stringRedisTemplate.delete(getKey(dto.getLogin()));
    }
}