package com.izhiliu.erp.config.task;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateApi;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.domain.item.ShopeeProductMedia;
import com.izhiliu.erp.service.item.BatchBoostItemService;
import com.izhiliu.erp.service.item.ShopeeProductMediaService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductMediaDTO;
import com.izhiliu.erp.service.item.impl.ShopeeProductMediaServiceImpl;
import com.izhiliu.erp.service.item.module.channel.ShopeeModelChannel;
import com.izhiliu.erp.web.rest.item.param.ShopProductParam;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class MultipleTask   {


    @Resource
    private CurrencyRateApi currencyRateApi;

    @Resource
    BatchBoostItemService batchBoostItemService;

    @Resource
    ShopeeProductMediaServiceImpl shopeeProductMediaService;

    @Resource
    ShopeeProductService  shopeeProductService;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private final static String BOOST_ITEM_TASK_KEY = "BOOST_ITEM_TASK_KEY";

    @Resource
    protected ShopeeModelChannel shopeeModelBridge;

    @Value("${isBoost}")
    private boolean isBoost = true;


    /**
     * 定时置顶
     * <p>
     * 每天 4分钟执行 一次
     */
    @Scheduled(cron = "0 0/4 * * * ?")
    public void syncBoostItem() {
        if(isBoost){
            RLock rLock = redissonClient.getLock(BOOST_ITEM_TASK_KEY);
            if(rLock.tryLock()){
                try {
                    batchBoostItemService.boostItemV2();
                }finally {
                    rLock.unlock();
                }
            }
        }
    }
    /**
     * 定时置顶
     * <p>
     * 每天 4分钟执行 一次
     */
//    @Scheduled(cron = "0 0/5 * * * ?")
    public void syncPriceRangeItem() throws InterruptedException {
//        boolean isStop = true;
//        do {
            final String syncPriceRangeItem_index = stringRedisTemplate.opsForValue().get("syncPriceRangeItem_index");
            final List<ShopeeProductMedia> longs = shopeeProductMediaService.selectByPriceRange(Objects.isNull(syncPriceRangeItem_index)?0:Integer.parseInt(syncPriceRangeItem_index));
            if (CollectionUtils.isEmpty(longs)) {
//                isStop = false;
//                continue;
            } else {
                stringRedisTemplate.opsForValue().increment("syncPriceRangeItem_index",longs.size());
                for (ShopeeProductMedia aLong : longs) {
                    final Optional<ShopeeProductDTO> shopeeProductDTO = shopeeProductService.find(aLong.getProductId());
                    if (shopeeProductDTO.isPresent()) {
                        final ShopeeProductDTO shopeeProductDTO1 = shopeeProductDTO.get();
                        if (Objects.equals(shopeeProductDTO1.getCollect(), "alibaba")) {
                            log.info(" 正在处理异步任务 shopeeProductDTO {} ", JSONObject.toJSONString(shopeeProductDTO1));
                            final Integer type = shopeeProductDTO1.getType();

                            if (Objects.equals("[]", aLong.getPriceRange())) {

                                shopeeProductMediaService.clearCacheBatch(aLong);
                                stringRedisTemplate.opsForValue().increment("syncCachePriceRangeItem", 1);

                            } else {

                                final ShopeeProductMedia shopeeProductMedia = new ShopeeProductMedia();
                                shopeeProductMedia.setId(aLong.getId());
                                shopeeProductMedia.setPriceRange("[]");
                                shopeeProductMediaService.getRepository().updateById(shopeeProductMedia);
                                if (type == 3 && Objects.nonNull(shopeeProductDTO1.getShopeeItemId())) {
                                    log.info("[推送到店铺] : {}", shopeeProductDTO1.getShopId());
                                    shopeeModelBridge.push(shopeeProductDTO1.getId(), shopeeProductDTO1.getShopId(), shopeeProductDTO1.getLoginId());
                                    stringRedisTemplate.opsForValue().increment("syncPriceRangeItem_online", 1);
                                }
                                stringRedisTemplate.opsForValue().increment("syncPriceRangeItem", 1);
                            }
                        }
                    }
                }
//                Thread.sleep(3000);
            }
//        } while (isStop);

    }

    @Scheduled(cron = "0 0 2 * * ? ")
    public void syncCurrencyRate(){
        new Thread(() -> {
            final CurrencyRateApi.Currency[] values = CurrencyRateApi.Currency.values();
            for (CurrencyRateApi.Currency form : values) {
                for (CurrencyRateApi.Currency to : values) {
                    try {
                        currencyRateApi.currencyConvert(form.code,to.code);
                    } catch (Exception e) {
                         log.error("error : "+e.getMessage(),e);
                    }
                }
            }
        }).run();
    }


    public void run(ApplicationArguments args) throws Exception {
        syncCurrencyRate();
    }
}
