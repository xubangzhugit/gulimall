package com.izhiliu.erp.service.item.cache.impl;

import com.google.common.util.concurrent.Atomics;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.erp.service.item.cache.BoostItemCacheService;
import com.izhiliu.erp.service.item.dto.CacheObejct;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 用来多服务器 进行 同步下标
 */
@Service
@Slf4j
public class BoostItemCacheServiceImpl implements BoostItemCacheService {




    ThreadLocal<CacheObejct> cacheObejct = ThreadLocal.withInitial(() -> {
        try {
            throw new IllegalAccessException("请指定对应的参数");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    });


    public static String BOOST_CURSOR_KEY = "lux:boost:item:cursor:";
    public static  String BOOST_CURSOR_PUll_KEY = "lux:boost:item:pull:";
    int SIZE = -5;


    StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> stringStringValueOperations;
    private ZSetOperations<String, String> stringStringZSetOperations;
    RedisLockHelper redisLockHelper;

    public BoostItemCacheServiceImpl(StringRedisTemplate stringRedisTemplate,RedisLockHelper redisLockHelper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.stringStringValueOperations = stringRedisTemplate.opsForValue();
        this.stringStringZSetOperations = stringRedisTemplate.opsForZSet();
        this.redisLockHelper = redisLockHelper;
    }


    @Override
    public Integer doPull(Optional<CacheObejct> supplier) throws IllegalAccessException {
        final Set<String> strings = stringStringZSetOperations.reverseRange(getKey(), 0, 0);
        if (!strings.isEmpty()) {
            this.cacheObejct.get().setTimestamp(strings.iterator().next());
        }
        if (Objects.isNull(getTimestamp())) {
            return doPull(supplier, getIntegerSupplier(supplier));
        } else {
            return doPull(supplier, getIntegerSupplier());
        }

    }

    private String getKey() {
        return this.cacheObejct.get().getKey();
    }

    private Supplier<Integer> getIntegerSupplier() {
        return () -> {
            final Double rank = stringStringZSetOperations.score(getKey(), getTimestamp());
            return Objects.isNull(rank) ? -1 : rank.intValue();
        };
    }

    private Supplier<Integer> getIntegerSupplier(Optional<CacheObejct> supplier) {
        return () -> {
            final Set<ZSetOperations.TypedTuple<String>> typedTuples = stringStringZSetOperations.reverseRangeWithScores(getKey(), 0, 0);
            final Integer integer;
            if (CollectionUtils.isEmpty(typedTuples)) {
                integer = doPush(supplier);
            } else {
                final ZSetOperations.TypedTuple<String> next = typedTuples.iterator().next();
                integer = next.getScore().intValue();
                this.cacheObejct.get().setTimestamp(next.getValue());
            }
            return integer;
        };
    }

    public Integer doPull(Optional<CacheObejct> supplier, Supplier<Integer> setSupplier) throws IllegalAccessException {
        final int i;
        try {
            lock(this.cacheObejct.get().getLock());
            // 如果是 新启动的项目 就获取最新的
            final Integer integer = setSupplier.get();
            if (integer < 0) {
                stringRedisTemplate.expire(getKey(),1,TimeUnit.MINUTES);
                return -1;
            }
            final Integer o = supplier.get().getSize();
            if (log.isDebugEnabled()) {
                log.debug("  cache size {} ,datasource {}", integer, o);
            }
            // 进来的 总数  减去  存的   不小于  就 移动  游标
            i = o.intValue() - integer.intValue();
            if (i >= 0) {
                //  否则就下标往前移动
                stringStringZSetOperations.incrementScore(getKey(), getTimestamp(), SIZE);
                if(i== 0){
                    stringRedisTemplate.expire(getKey(),1,TimeUnit.MINUTES);
                    return -1;
                }
            } else {
                return -1;
            }
        } finally {
            unlock(this.cacheObejct.get().getLock());
        }
        return i;
    }

    private String getTimestamp() {
        return this.cacheObejct.get().getTimestamp();
    }




    @Override
    public Integer doPush(Optional<CacheObejct> supplier) {
        final String value = Instant.now().toEpochMilli() + "";
        this.cacheObejct.get().setTimestamp(value);
        final int size = supplier.get().getSize();
        stringStringZSetOperations.add(getKey(), value, size);
        stringRedisTemplate.expire(getKey(),6,TimeUnit.MINUTES);
        return size+SIZE;
    }

    @Override
    public void unlock(String key) {
        redisLockHelper.unlock(key);
    }

    @Override
    public void lock(String key) throws IllegalAccessException {
        try {
            final String value = UUID.randomUUID().toString();
            Boolean aBoolean;

            int i = 0;
            do {
                aBoolean =  redisLockHelper.lock(key,value,3,TimeUnit.MINUTES);
                if (!aBoolean) {
                    if (i == 15) {
                        throw new IllegalAccessException("redis 获取锁超时");
                    }
                    Thread.sleep(500);
                    ++i;
                }
            } while (!aBoolean);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /**
     * 重写
     *
     * @return
     */
    @Override
    public Optional<Integer> run(Optional<CacheObejct> supplier) throws IllegalAccessException {
        cacheObejct.set(supplier.get());
        return pull(supplier);
    }


    @Override
    public Optional<Integer> pull(Optional<CacheObejct> supplier) throws IllegalAccessException {
        return Optional.of(doPull(supplier));
    }
}