package com.izhiliu.erp.service.discount.impl;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.service.discount.DiscountPriceService;
import com.izhiliu.erp.service.discount.dto.DiscountProductDto;
import com.izhiliu.erp.service.discount.dto.DiscountSkuDto;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountParamDto;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.open.shopee.open.sdk.api.discount.ShopeeDiscountApiImpl;
import com.izhiliu.open.shopee.open.sdk.api.discount.param.*;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.AddDiscountResult;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.DeleteDiscountResult;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.GetDiscountsListResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DiscountPriceServiceImpl implements DiscountPriceService {

    public static final String SYNC_DISCOUNT_TO_LOCAL = "lux:sync-discount-to-local:";
    private static final int SIZE = 20;

    @Resource
    SnowflakeGenerate snowflakeGenerate;

    @Resource
    RedisLockHelper redisLockHelper;

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    UaaService  uaaService;

    @Resource
    ShopeeDiscountApiImpl shopeeDiscountApi;

    public static ExecutorService executorService = new ThreadPoolExecutor(
            2,
            20,
            1,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(1000), Executors.defaultThreadFactory(),new ThreadPoolExecutor.DiscardPolicy());


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ShopeeShopDTO checke(Long checheShopId) {
        final String login = SecurityUtils.currentLogin();
        List<ShopeeShopDTO> body;
        if (SecurityUtils.isSubAccount()) {
            body = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), true).getBody();
        } else {
            body = uaaService.getShopeeShopInfoV2(login, false).getBody();
        }

        if (body != null) {
            ShopeeShopDTO aLong  = body.stream().filter(shopId -> Objects.equals(checheShopId,shopId.getShopId())).findFirst().orElseThrow(() -> new  RuntimeException("shopId  error "));
             return  aLong;
        }
        return  null;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<ShopeeShopDTO> checke(List<Long> ids) {
        final String login = SecurityUtils.currentLogin();
        List<ShopeeShopDTO> body;
        if (SecurityUtils.isSubAccount()) {
            body = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), true).getBody();
        } else {
            body = uaaService.getShopeeShopInfoV2(login, false).getBody();
        }

        if (body != null) {
            List<ShopeeShopDTO> aLong  = body.stream().filter(shopId -> ids.contains(shopId.getShopId())).collect(Collectors.toList());
            return  aLong;
        }
        return  null;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long getId() {
        return snowflakeGenerate.nextId();
    }


    /**
     *     新增 折扣   并且支持 新增折扣的时候 直接加加入商品
     * @param aDto
     * @param login
     * @return
     */
    @Override
    public ResponseEntity save(ShopeeDiscountParamDto aDto, String login) {
        Objects.requireNonNull(checke(aDto.getShopId()));
        final ShopeeResult<AddDiscountResult> discountsList = shopeeDiscountApi.addDiscount(translation(aDto));
        //  成功 之后 清除  缓存
        if(discountsList.isResult()){
             String lockKey = getKey(String.valueOf(aDto.getShopId())).concat(GetDiscountsListParam.Status.ALL.getCode());
            stringRedisTemplate.delete(lockKey);
            lockKey = getKey(String.valueOf(aDto.getShopId())).concat(GetDiscountsListParam.Status.UPCOMING.getCode());
            stringRedisTemplate.delete(lockKey);
        }
        return  ResponseEntity.ok(discountsList.getData());
    }


    /**
     *      将已有的 折扣 里面 加入 新的商品
     * @param aDto
     * @param login
     * @return
     */
    @Override
    public ResponseEntity saveProductOrSku(ShopeeDiscountParamDto aDto, String login) {
        Objects.requireNonNull(checke(aDto.getShopId()));
        final ShopeeResult<AddDiscountResult> discountsList = shopeeDiscountApi.addDiscountItem(translation(aDto));
        return  ResponseEntity.ok(discountsList.getData());
    }

    @Override
    public ResponseEntity delete(ShopeeDiscountParamDto aDto, String login) {
        Objects.requireNonNull(checke(aDto.getShopId()));
        final ShopeeResult<DeleteDiscountResult> deleteDiscountResultShopeeResult = shopeeDiscountApi.deleteDiscount(deleteTranslation(aDto));
        deleteDiscountResultShopeeResult.setJson(null);
        return ResponseEntity.ok(deleteDiscountResultShopeeResult);
    }

    public ResponseEntity flushCache(List<Long> shopId) {
        final List<ShopeeShopDTO> checke = checke(shopId);
        if (!CollectionUtils.isEmpty(checke)) {
            for (ShopeeShopDTO shopeeShopDTO : checke) {
                final String key = getKey(shopeeShopDTO.getShopId().toString());
                for (int type = 0; type < 3; type++) {
                    stringRedisTemplate.delete(key.concat(String.valueOf(type)));
                }
            }
        }
        return ResponseEntity.ok(true);
    }



    private DeleteDiscountParam deleteTranslation(ShopeeDiscountParamDto aDto) {
        return DeleteDiscountParam.builder()
                .shopId(aDto.getShopId())
                .discountId(Objects.isNull(aDto.getDiscountId())?0:aDto.getDiscountId())
                .build();
    }

    private AddDiscountParam translation(ShopeeDiscountParamDto aDto) {
        return AddDiscountParam.builder()
                .shopId(aDto.getShopId())
                .discountId(Objects.isNull(aDto.getDiscountId())?0:aDto.getDiscountId())
                .discountName(aDto.getDiscountName())
                .startTime(aDto.getStartTime())
                .endTime(aDto.getEndTime())
                .items(CollectionUtils.isEmpty(aDto.getItems())?null:aDto.getItems().stream().map(this::translation).collect(Collectors.toList()))
                .build();
    }
    private DiscountItem translation(DiscountProductDto aDto) {
        final DiscountItem item = new DiscountItem();
        item.setItemId(aDto.getItemId());
        item.setPurchaseLimit(aDto.getPurchaseLimit());
        item.setItemPromotionPrice(aDto.getItemPromotionPrice());
        item.setVariations(CollectionUtils.isEmpty(aDto.getVariations())?null:aDto.getVariations().stream().map(this::translation).collect(Collectors.toList()));
        return item;
    }
    private DiscountVariation translation(DiscountSkuDto aDto) {
        final DiscountVariation variation = new DiscountVariation();
        variation.setVariationId(aDto.getVariationId());
        variation.setVariationPromotionPrice(aDto.getVariationPromotionPrice());
        return  variation;
    }


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<GetDiscountsListResult.Discount> getDiscountsList(Long shopId, int type) {
        final String login = SecurityUtils.currentLogin();
        try {
            return pullRedisCount(shopId,login,type);
        } catch (Exception e) {
           log.error(e.getMessage(),e);
        }
        return  Collections.emptyList();
    }


    private List<GetDiscountsListResult.Discount> pullRedisCount(Long shopId, String login, int type) {
        final String lockKey =  getKey(shopId.toString()).concat(String.valueOf(type));
        if(log.isDebugEnabled()){
            log.debug(" pullRedisCount  lockKey  : {}",lockKey);
        }
         //  如果缓存有的话就直接去拿
        try {
            if(stringRedisTemplate.hasKey(lockKey)){
                List<GetDiscountsListResult.Discount> discount = getDiscountsCache(lockKey);
                return  discount;
            }
            final String concat = lockKey.concat(":bak");
            if(stringRedisTemplate.hasKey(concat)){
                List<GetDiscountsListResult.Discount> discount = getDiscountsCache(concat);
                executorService.execute(() -> {
                    asynPullDiscountToRedis(shopId, login, type, lockKey);
                });
                return  discount;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return asynPullDiscountToRedis(shopId, login, type, lockKey);
    }

    private String getKey(String shopId) {
        return SYNC_DISCOUNT_TO_LOCAL.concat(shopId);
    }

    /**
     *    异步获取对应的数据
     * @param shopId
     * @param login
     * @param type
     * @param lockKey
     * @return
     */
    private List<GetDiscountsListResult.Discount> asynPullDiscountToRedis(Long shopId, String login, int type, String lockKey) {
        int page = 0;
        try {
            final ShopeeResult<GetDiscountsListResult> discountsList = shopeeDiscountApi.getDiscountsList(GetDiscountsListParam.builder().discountStatus(GetDiscountsListParam.Status.values()[type].getCode()).shopId(shopId).paginationOffset(page++).paginationRntriesPerPage(SIZE).build());
            if (!discountsList.isResult()) {
                    return Collections.emptyList();
            }
            final List<GetDiscountsListResult.Discount> discount = discountsList.getData().getDiscount();
            if(Objects.nonNull(discount)){
                final String[] context = !CollectionUtils.isEmpty(discount)? discount.stream().map(discount1 -> JSONObject.toJSONString(discount1)).toArray(value -> new String[value]):new String[]{""};
                // 一级缓存
                stringRedisTemplate.opsForSet().add(lockKey, context);
                stringRedisTemplate.expire(lockKey,3, TimeUnit.MINUTES);
                // 二级缓存
                final String concat = lockKey.concat(":bak");
                stringRedisTemplate.delete(concat);
                stringRedisTemplate.opsForSet().add(concat, context);
                stringRedisTemplate.expire(concat,3, TimeUnit.DAYS);
            }
            if (discountsList.getData().isMore()){
                executorService.execute(() -> {
                    pullDiscount(shopId,login,lockKey,type);
                });
            }
            log.info(" 异步获取营销活动 店铺商品  infoKey {} shopId:{}, login:{} ,item size:{} ", lockKey, shopId, login, discount.size());
                return  discountsList.getData().getDiscount();
        } catch (Exception e) {
            log.error(" infoKey {}  shopId:{}, login:{}, page:{} exception {}", lockKey,shopId, login, page, e);
        }
        return Collections.emptyList();
    }

    private List<GetDiscountsListResult.Discount> getDiscountsCache(String lockKey) throws IOException {
        List<GetDiscountsListResult.Discount> discount = new ArrayList<>(stringRedisTemplate.opsForSet().size(lockKey).intValue());
        Cursor<String> cursor ;
        final ScanOptions build = ScanOptions.scanOptions().count(3000).match("*").build();
        do {
            cursor  = stringRedisTemplate.opsForSet().scan(lockKey, build);
            while (cursor.hasNext()) {
                final String next = cursor.next();
                 if(StringUtils.isNotBlank(next)){
                     discount.add(JSONObject.parseObject(next, GetDiscountsListResult.Discount.class));
                 }
            }
            if(0 ==cursor.getCursorId()){
                cursor.close();
            }

        }while (!cursor.isClosed());

        return discount;
    }


    private void pullDiscount(Long shopId, String login, String lockKey, int type){
        int page = 1;

        while (true) {
            try {
                final ShopeeResult<GetDiscountsListResult> discountsList = shopeeDiscountApi.getDiscountsList(GetDiscountsListParam.builder().discountStatus(GetDiscountsListParam.Status.values()[type].getCode()).shopId(shopId).paginationOffset(page++).paginationRntriesPerPage(SIZE).build());
                if (!discountsList.isResult()) {
                    break;
                }
                final List<GetDiscountsListResult.Discount> discount = discountsList.getData().getDiscount();
                if (CommonUtils.isBlank(discount)){
                    break;
                }
                stringRedisTemplate.opsForSet().add(lockKey, discount.stream().map(discount1 -> JSONObject.toJSONString(discount1)).toArray(value -> new String[value]));
                //如果没有后续的数据直接跳出
                if (!discountsList.getData().isMore()) {
                    stringRedisTemplate.expire(lockKey,5,TimeUnit.MINUTES);
                    break;
                };
            } catch (Exception e) {
                log.error(" 异步获取营销活动 店铺商品 key:{} shopId:{}, login:{}, page:{} exception {}", lockKey, shopId, login, page, e);
                break;
            }
        }

    }


}
