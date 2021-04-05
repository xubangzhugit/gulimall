package com.izhiliu.erp.config.aop.subscribe;

import com.izhiliu.core.config.internation.InternationUtils;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.config.subscribe.Enum.SubLimitAopEnums;
import com.izhiliu.core.config.subscribe.SubLimitAnnotation;
import com.izhiliu.erp.web.rest.errors.SubLimitException;
import com.izhiliu.feign.client.DariusService;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Author: louis
 * @Date: 2019/7/13 18:05
 */
@Component
public class SubLimitService  {

    private final Logger log = LoggerFactory.getLogger(SubLimitService.class);
    private static final String LUX_SUB_LIMIT_LOCK = "lux_usb_limit_lock:";
    private static final String LUX_SUB_LIMIT_LOCK_BACK = "lux_usb_limit_lock_back:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisLockHelper redisLock;
    @Resource
    private MessageSource messageSource;
    @Resource
    private DariusService dariusService;


    @Value("${isLimit}")
    private boolean isLimit;


    /**
     *
     * @param currentLogin  登录的信息
     * @param key  操作的 key
     * @param tag  附加信息
     */
    public void handleLimitSupport(String currentLogin,String key,String tag){
        final String lockKey = LUX_SUB_LIMIT_LOCK + ":" + currentLogin+ ":" + tag;
        handleLimit(currentLogin, key, 1,lockKey);
    }

    public void handleLimit(String currentLogin,String key,int size){
        final String lockKey = LUX_SUB_LIMIT_LOCK + ":" + currentLogin;
        handleLimit(currentLogin, key, size,lockKey);
    }
    /**
     *
     * @param currentLogin    当前登录的
     * @param key         subLimitAnnotation.limitProduct()   {@link SubLimitAnnotation}
     * @throws NoSuchMethodException
     */
    public void handleLimit(String currentLogin, String key, int size, String lockKey) {
        if (size < 0) {
            throw new SubLimitException(" limit size  > 0 ");
        }
        if(isLimit) {
            if (nonSpinLock(lockKey)) {
                try {
                    log.info("进入限次切面");
                    String redisKey = SubLimitAopEnums.DAY_LIMIT.getCode() + currentLogin + "_" +key;
                    String data = this.doPull(redisKey);
                    if (null == data) {
                        data = this.doPush(redisKey);
                    }
                    final int limitSize = Integer.parseInt(data);
                    if(limitSize == -1){
                        log.error("今日免费次数 是 -1  loginId {}  key  {}",currentLogin,key);
                        return;
                    }
                    if (StringUtils.isBlank(data)|| limitSize < size) {
                        log.error("今日免费次数已达到上限  loginId {}  key  {}",currentLogin,key);
                        throw new SubLimitException(messageSource.getMessage(SubLimitAopEnums.NO_FREE.getCode(), null, InternationUtils.getLocale()));
                    }
                    this.incr(redisKey,-1 * size);
                } finally {
                    this.unlock(lockKey);
                }
            }else{
                log.error("今日免费次数已达到上限  loginId {}  key  {}",currentLogin,key);
                throw new SubLimitException(messageSource.getMessage("subscribe.double_click", null, InternationUtils.getLocale()));
            }
        }
    }

    /**
     *
     * @param currentLogin    当前登录的
     * @param key         subLimitAnnotation.limitProduct()   {@link SubLimitAnnotation}
     * @throws NoSuchMethodException
     */
    public void handleLimit(String currentLogin,String key) {
        handleLimit(currentLogin, key,1);
    }


    /**
     *
     * @param currentLogin    当前登录的
     * @param key         subLimitAnnotation.limitProduct()  {@link SubLimitAnnotation}
     * @throws NoSuchMethodException
     */
    public void doAfterThrowing(String currentLogin,String key)   {
        if(isLimit) {
            final String luckKey = LUX_SUB_LIMIT_LOCK + ":" + currentLogin;
            if (StringUtils.isNotBlank(this.doPull(luckKey))) {
                this.unlock(luckKey);
                return;
            }
            //     因为已经知道是已经扣除过了  所以 直接使用
            String redisKey = SubLimitAopEnums.DAY_LIMIT.getCode() + currentLogin + "_" + key;
            this.incr(redisKey, 1);
        }
    }


    public boolean nonSpinLock( String luckKey) {
        return redisLock.lock(luckKey, luckKey, 3, TimeUnit.SECONDS);
    }

    public void unlock(String key) {
      redisLock.unlock(key);
    }


    public String doPull(@NonNull  String limitProduct){
        return stringRedisTemplate.opsForValue().get(limitProduct);
    }

    /**
     *    多地方调用 修改请注意哦
     * @param limitProduct
     * @param size
     */
    public void incr(@NonNull  String limitProduct,int size){
        String data = stringRedisTemplate.opsForValue().get(limitProduct);
        //    这个key 必须存在 而且  存活时间要大于 30 秒 才会 回补 防止 出现  存活时间没有的情况
        if (!(org.springframework.util.StringUtils.isEmpty(data))&&stringRedisTemplate.getExpire(limitProduct)>30L) {
            stringRedisTemplate.opsForValue().increment(limitProduct,size);
        }
    }


    /**
     *
     * @param limitProduct  限流次数
     * @return
     */
    public String doPush(@NonNull String limitProduct) {
        final String login = SecurityUtils.currentLogin();
        dariusService.subscribeLimit(login, new String[]{limitProduct});
        /**
         * @todo    很奇怪的操作  读写不在一起。。
         */
        return  doPull(limitProduct);
    }


}
