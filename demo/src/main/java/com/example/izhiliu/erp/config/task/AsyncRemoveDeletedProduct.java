package com.izhiliu.erp.config.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.repository.item.*;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.util.SnowflakeGenerate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * describe: 异步删除已经 软删除的  product   and  shu
 * <p>
 *
 * @author cheng
 * @date 2019/1/29 9:45
 */
@Component
public class AsyncRemoveDeletedProduct  implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AsyncRemoveDeletedProduct.class);

    @Resource
    ShopeeProductRepository shopeeProductRepository;
    @Resource
    ShopeeProductService shopeeProductService;

    @Resource
    ShopeeProductSkuRepository  shopeeProductSkuRepository;
    @Resource
    ShopeeSkuAttributeRepository shopeeSkuAttributeRepository;
    @Resource
    ShopeeProductAttributeValueRepository shopeeProductAttributeValueRepository;
    @Resource
    ShopeeProductMediaRepository shopeeProductMediaRepository;
    @Resource
    ShopeeProductDescRepository shopeeProductDescRepository;
    @Resource
    BoostItemRepository boostItemRepository;

    private String  key = "AsyncRemoveDeletedProduct";
    private String  countKey = "async_remove_deleted_rroduct_count";
    /**
     * //定义范围开始数字
     */
    public static final int START = 0;

    /**
     * //定义范围结束数字
     */
    public static final int MAX = 8;

    public static final int SIZE = 100;

    HashOperations<String, String, String> stringHashOperations ;

    StringRedisTemplate stringRedisTemplate;

    @Resource
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        stringHashOperations = stringRedisTemplate.opsForHash();
    }

    private String id;

    @Resource
    public void setId(SnowflakeGenerate snowflakeGenerate) {
        this.id = snowflakeGenerate.nextId().toString();
    }

    public void asyncRemoveDeletedProduct() {
        int number = START;
        Boolean aBoolean;
        do {
            aBoolean = stringRedisTemplate.opsForHash().putIfAbsent(key,  ""+number,id);
            if(!aBoolean){
                number = ++number;
                if(number > MAX){
                    stringRedisTemplate.delete(key);
                    number =  START;
                }
            }
        } while (!aBoolean);

        final List<Long> shopeeProductIds = shopeeProductRepository.selectByDeleted(new Page(number,SIZE));
        if (!shopeeProductIds.isEmpty()) {
            log.info(" shopeeProductIds {}",shopeeProductIds);
            shopeeProductIds.forEach(shopeeProductId -> {
                removeAll(shopeeProductId);
            });
            stringRedisTemplate.opsForValue().increment("async_remove_deeleted_product_count",shopeeProductIds.size());
        }
        stringRedisTemplate.opsForHash().delete(key,  ""+number);
    }

    public void removeAll(Long shopeeProductId) {
        final int shopeeSkuAttribute = shopeeSkuAttributeRepository.deleteByDeleted(shopeeProductId);
        log(shopeeSkuAttribute, "shopee-sku-attribute");
        final int shopeeProductSku = shopeeProductSkuRepository.deleteByDeleted(shopeeProductId);
        log(shopeeProductSku, "shopee-product-sku");
        final int shopeeProductAttributeValue = shopeeProductAttributeValueRepository.deleteByDeleted(shopeeProductId);
        log(shopeeProductAttributeValue, "shopee-product-attribute-value");
        final int shopeeProductMedia = shopeeProductMediaRepository.deleteByDeleted(shopeeProductId);
        log(shopeeProductMedia, "shopee-product-media");
        final int shopeeProductDesc = shopeeProductDescRepository.deleteByDeleted(shopeeProductId);
        log(shopeeProductDesc, "shopee-product-desc");
        final int boostItem = boostItemRepository.deleteByDeleted(shopeeProductId);
        log(boostItem, "boost-item");
        final int shopeeProduct = shopeeProductRepository.deleteByDeleted(shopeeProductId);
        log(shopeeProduct, "shopee-product");
    }

    private void log(int number, String content) {
        stringHashOperations.increment(countKey,content,number);
        stringHashOperations.increment( LocalDate.now().toString().concat(countKey),content,number);
        log.info("delete ".concat(content.concat(" size {}")), number);
    }


    public void asyncRemoveDeletedSiteProduct(){
        final String syncPriceRangeItem_index = stringRedisTemplate.opsForValue().get("async_remove_deleted_site_product");
        final List<Long> longs = shopeeProductRepository.selectByDeletedSite(Objects.isNull(syncPriceRangeItem_index)?0:Integer.parseInt(syncPriceRangeItem_index),SIZE);
        if(!CollectionUtils.isEmpty(longs)){
            stringRedisTemplate.opsForValue().increment("async_remove_deleted_site_product",longs.size());
            for (Long productId : longs) {
                removeDeletedShopeeItemData(productId);
            }
            final Integer productMunber = Integer.valueOf(Objects.requireNonNull(stringRedisTemplate.opsForValue().get("async_remove_deleted_site_product")));
            if(productMunber > 100){
                stringRedisTemplate.opsForValue().set("async_remove_deleted_site_product","0");
            }
            stringRedisTemplate.opsForValue().increment("async_remove_deleted_site_product_count",longs.size());
        }
    }

    public void removeDeletedShopeeItemData(Long productId) {
        shopeeProductService.delete(productId);
        removeAll(productId);
    }


    /**
     *  异步删除deleted  的 商品数据
     * <p>
     * 每天 凌晨 1:00 执行
     */
    @Scheduled(cron = "0/15 * 3,4,5 * * ?")
    public void syncCategory() {
        final String concat = LocalDate.now().toString().concat(countKey);
        //   每天的
        if(!stringRedisTemplate.hasKey(concat)){
            stringHashOperations.put(concat,"init","0");
            stringRedisTemplate.expire(concat,6, TimeUnit.DAYS);
        }
        asyncRemoveDeletedSiteProduct();
        asyncRemoveDeletedProduct();
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        final String concat = LocalDate.now().toString().concat(countKey);
        //   每天的
        if(!stringRedisTemplate.hasKey(concat)){
            stringHashOperations.put(concat,"init","0");
            stringRedisTemplate.expire(concat,6, TimeUnit.DAYS);
        }
        //  总和
        if(!stringRedisTemplate.hasKey(countKey)){
            stringHashOperations.put(countKey,"init","0");
        }
        asyncRemoveDeletedSiteProduct();
        asyncRemoveDeletedProduct();
    }
}
