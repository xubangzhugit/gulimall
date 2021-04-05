package com.example.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.gulimall.product.entity.BrandEntity;
import com.example.gulimall.product.service.BrandService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {
    @Autowired
    private BrandService brandService;
    private String v;

    /*@Resource
    OSSClient as;*/
    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setDescript("aaaa");
        brandEntity.setLogo("logo");
        brandEntity.setDescript("setDescript");
        brandEntity.setName("setName");
        brandEntity.setSort(4);
        brandEntity.setShowStatus(2);
        brandEntity.setFirstLetter("5");
        boolean save = this.brandService.save(brandEntity);
        log.info("testSave====="+save);
        BrandEntity brand = this.brandService.getById(1);
        log.info("testSave====="+brand);
        List<BrandEntity> brand_id = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1));
        log.info(brand_id.get(0).toString());

       /* Comparator<Integer> com = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Integer.compare(o1,o2);
            }
        };
        TreeSet<Integer> st = new TreeSet<Integer>(com);*/
    }
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Test
    void redisTest(){
        synchronized (this){
            ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();

            executeService(stringStringValueOperations);
            //否则 直接返回hello
        }
    }
    @Test
    void redisLockTest(){
        synchronized (this){
            ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
            v = UUID.randomUUID().toString();
            //分布式加锁
            Boolean lock = stringStringValueOperations.setIfAbsent("lock", v, 2000, TimeUnit.SECONDS);
            if(lock){
                //加锁成功，执行业务
                //stringRedisTemplate.expire("lock",2000,TimeUnit.SECONDS);设置过期时间
                executeService(stringStringValueOperations);
                //删除锁
//                String lock1 = stringRedisTemplate.opsForValue().get("lock");
////
////                if(lock1.equals(v)){
////                    stringRedisTemplate.delete("lock");//删除锁
////                }
                //lua脚本解锁,删除锁，分布式解锁
                String lua = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then return redis.call(\"del\",KEYS[1]) else return 0 end";
                stringRedisTemplate.execute(new DefaultRedisScript<Long>(lua,Long.class),Arrays.asList("lock"),v);
            }else {
                //加锁失败 重试
                try{
                    Thread.sleep(3000);
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    String timeId = IdWorker.getTimeId(); //mybatisPlus自带的id生成工具类
                }
                redisLockTest();
            }

        }
    }

    private void executeService(ValueOperations<String, String> stringStringValueOperations) {
        String hello = stringStringValueOperations.get("hello");
        if (StringUtils.isEmpty(hello)) {
            //查询数据库，给redis缓存中存储
            stringStringValueOperations.set("hello", "word");
        }
        //否则 直接返回hello
    }
    @Autowired
    Redisson redisson;
    @Test
    void redissonTest(){
        RLock redissonlock = redisson.getLock("redissonlock");
        redissonlock.lock(); //redisson 加锁 有看门狗机制，时间过了1/3，会自动重置过期时间。默认30秒
        try{

            //执行业务代码
            ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
            executeService(stringStringValueOperations);
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            redissonlock.unlock();
        }

    }

    /**
     * redisson 读写锁，读锁共享，写锁互斥 保证读到的都是最新的数据
     * 读锁 共享
     */
    @Test
    void redissonReadWriteLock(){
        RReadWriteLock rw_lock = 
                redisson.getReadWriteLock("RW_lock");
        RLock rLock = rw_lock.readLock();

        rLock.lock();
        //执行业务
        String id = stringRedisTemplate.opsForValue().get("id");
        System.out.println(id);
        rLock.unlock();
    }
    /**
     * redisson 读写锁，读锁共享，写锁互斥 保证读到的都是最新的数据
     * 写锁 互斥
     */
    @Test
    void redissonReadWriteLock2(){
        RReadWriteLock rw_lock =
                redisson.getReadWriteLock("RW_lock");
        RLock rLock = rw_lock.writeLock();

        rLock.lock();
        //执行业务
        String s = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set("id",s);
        System.out.println("写入成功");
        rLock.unlock();
    }

}
