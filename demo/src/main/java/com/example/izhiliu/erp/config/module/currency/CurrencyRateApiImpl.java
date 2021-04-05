package com.izhiliu.erp.config.module.currency;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.erp.config.ApplicationProperties;
import com.izhiliu.erp.config.module.currency.base.BaseCurrencyRateService;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateApi;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.domain.item.CurrencyRate;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.erp.web.rest.errors.InternalServiceException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Period;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/3 15:56
 */
@Component
public class CurrencyRateApiImpl implements CurrencyRateApi {

    private static final Logger log = LoggerFactory.getLogger(CurrencyRateApiImpl.class);

    public CurrencyRateApiImpl(@Qualifier(value = "sinaCurrencyRateService") SinaCurrencyRateService sinaCurrencyRateService, ApplicationProperties properties, StringRedisTemplate stringRedisTemplate, @Qualifier(value = "handleProductExceptionInfo") HandleProductExceptionInfo handleProductExceptionInfo) {
        baseCurrencyRateServices.add(sinaCurrencyRateService);
        this.properties = properties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.handleProductExceptionInfo = handleProductExceptionInfo;
    }

    private final ApplicationProperties properties;

    private Executor executor = Executors.newFixedThreadPool(1);

    List<BaseCurrencyRateService> baseCurrencyRateServices = new ArrayList<>(2);

//    @Autowired
//    private CurrencyRateService currencyRateService;

    final
    StringRedisTemplate stringRedisTemplate;


    private final HandleProductExceptionInfo handleProductExceptionInfo;

    @Override
    public CurrencyRateResult currencyConvert(String from, String to) {
        CurrencyRateResult currencyRateResult = null;
        final Iterator<BaseCurrencyRateService> iterator = baseCurrencyRateServices.iterator();
        while (Objects.isNull(currencyRateResult) && iterator.hasNext()) {
            final BaseCurrencyRateService next = iterator.next();
            try {
                currencyRateResult = doCurrencyConvert(from, to, next);
            } catch (Exception e) {
                log.error("汇率错误 from :".concat(from).concat(" to:").concat(to), e);
            }
        }
        return currencyRateResult;
    }

    private CurrencyRateResult doCurrencyConvert(String from, String to, BaseCurrencyRateService currencyRateService) {
        final CurrencyRateResult result1 = cache(from, to,currencyRateService);
        if (result1 != null) {
            return result1;
        }
        return getLatestCurrencyRate(from, to, currencyRateService);
    }

    private CurrencyRateResult getLatestCurrencyRate(String from, String to, BaseCurrencyRateService next) {
        CurrencyRateResult result;
        try {
            result = getCurrencyRate(from, to, next);
        } catch (Exception e) {
            result = getCurrencyRate(to, from, next);
            // 汇率倒置
            result.setRate(new BigDecimal(1).divide(result.getRate(), 8, RoundingMode.DOWN));
            // 互换
            String current = result.getFrom();
            result.setFrom(result.getTo());
            result.setTo(current);
        }
        save(from, to, result);
        return result;
    }


    private CurrencyRateResult getCurrencyRate(String from, String to, BaseCurrencyRateService baseCurrencyRateService) {
        final String api = baseCurrencyRateService.getApi(from, to);

        int count = 3;
        String response = "None";
        while (count > 1) {
            try {
                response = HttpRequest.get(api)
                        .execute().body();
                break;
            } catch (HttpException e) {
                log.error("[获取汇率错误] ", e);
                count = handlerException(from + ":" + to, count, e);
            }
        }

        log.debug("------------------------------------------------------------");
        log.info("[request-api] : {} \n [response-body] : {}", api, response);
        log.debug("------------------------------------------------------------");

        CurrencyRateResult currencyRateResult = baseCurrencyRateService.wrapperResult(response, from, to);

        if (!currencyRateResult.isSuccess()) {
            CurrencyRateResult currencyRateUSDResult = getCurrencyRateOnce(from, "usd", baseCurrencyRateService);
            CurrencyRateResult currencyRateUSDTOResult = getCurrencyRateOnce("usd", to, baseCurrencyRateService);
            if (currencyRateUSDResult.isSuccess() && currencyRateUSDTOResult.isSuccess()) {
                currencyRateResult.setSuccess(true);
                currencyRateResult.setRate(currencyRateUSDResult.getRate().multiply(currencyRateUSDTOResult.getRate()));
                currencyRateResult.setFrom(from);
                currencyRateResult.setTo(to);
            }
        }

        return currencyRateResult;
    }

    private CurrencyRateResult getCurrencyRateOnce(String from, String to, BaseCurrencyRateService baseCurrencyRateService) {
        final String api = baseCurrencyRateService.getApi(from, to);
        String response = HttpRequest.get(api)
                .execute().body();
        return baseCurrencyRateService.wrapperResult(response, from, to);
    }


    private int handlerException(String key, int count, HttpException e) {
        if (count == 0) {
            /*
             * 存在则续命3天,不存在只能报错了
             */
            CurrencyRate currencyRate = get(key);
            if (Objects.isNull(currencyRate)) {
                throw new InternalServiceException(handleProductExceptionInfo.doMessage("internal.service.exception.get.exchange.rate.failed") + e.getMessage());
            }
            currencyRate.setLastSyncTime(Instant.now());
            save(currencyRate);
        }

        return --count;
    }

    private CurrencyRateResult cache(String from, String to, BaseCurrencyRateService baseCurrencyRateService) {
        if (from.equals(to)) {
            return getCurrencyRateResult(from, to);
        }

        final CurrencyRate currencyRate = get(from + ":" + to);
        if (Optional.ofNullable(currencyRate).isPresent()) {
            final Instant lastUpdateTime = currencyRate.getLastSyncTime().plus(Period.ofDays(properties.getCurrencyRateRefreshDays()));
            /*
             * 现在已经距离最后一次更新有3天了
             *
             * 22 + 3       22  最后更新时间比当前时间大
             * 22 + 3       23  最后更新时间比当前时间大
             * 22 + 3       26  最后更新时间比当前时间小 该更新了
             */
            if (null == currencyRate.getLastSyncTime() || lastUpdateTime.isBefore(Instant.now())) {
                executor.execute(() -> getLatestCurrencyRate(from, to, baseCurrencyRateService));
            }
            return resultConvertEntity(currencyRate);
        } else {
            return null;
        }
    }

    static CurrencyRateResult getCurrencyRateResult(String from, String to) {
        final CurrencyRateResult result = new CurrencyRateResult();
        result.setFrom(from);
        result.setTo(to);
        result.setSuccess(true);
        result.setRate(new BigDecimal("1.0"));
        return result;
    }

    static CurrencyRateResult resultConvertEntity(CurrencyRate currencyRate) {
        final CurrencyRateResult result = new CurrencyRateResult();
        result.setSuccess(true);
        result.setRate(currencyRate.getRate());
        result.setFrom(currencyRate.getFrom());
        result.setTo(currencyRate.getTo());
        return result;
    }

    private void save(String from, String to, CurrencyRateResult result) {
        if(result.isSuccess() && null != result.getRate()){
            final CurrencyRate rate = new CurrencyRate();
            rate.setId(from + ":" + to);
            rate.setFrom(result.getFrom());
            rate.setTo(result.getTo());
            rate.setRate(result.getRate());
            rate.setLastSyncTime(Instant.now());
            save(rate);
        }

    }


    public CurrencyRate get(String key) {
        final String stringCurrencyRate = stringRedisTemplate.opsForValue().get(key);
        if (Objects.nonNull(stringCurrencyRate)) {
            return JSONObject.parseObject(stringCurrencyRate, CurrencyRate.class);
        }
        return null;
    }

    public Boolean save(CurrencyRate currencyRate) {
        stringRedisTemplate.opsForValue().set(currencyRate.getId(), JSONObject.toJSONString(currencyRate));
        return Boolean.TRUE;
    }

}
