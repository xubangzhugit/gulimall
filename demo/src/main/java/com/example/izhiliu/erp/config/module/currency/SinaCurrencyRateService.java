package com.izhiliu.erp.config.module.currency;

import java.math.BigDecimal;

import com.izhiliu.erp.config.module.currency.base.BaseCurrencyRateService;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


/**
 * 新浪汇率接口
 *
 * @author Administrator
 */
@Service
@Scope
@Slf4j
public class SinaCurrencyRateService implements BaseCurrencyRateService {

    final static String httpUrl = "http://hq.sinajs.cn/";



//    public static void main(String[] args) {
//        final SinaCurrencyRateService sinaCurrencyRateService = new SinaCurrencyRateService();
//
////        long currentTime=System.currentTimeMillis();
////        String httpArg = "rn="+currentTime+"list=fx_s"+"";
////        System.out.println(currentTime);
////        String send = getHuilvData(httpUrl, httpArg);
//        String result = HttpRequest.get(sinaCurrencyRateService.getApi("idr","cny"))
//                .execute().body();
//        log.info("send："+ result);
//        String [] arr = result.split(",");
////        redisService.save("USDCNY", arr[8]);
//        log.info("最新USDCNY汇率：" +new BigDecimal(arr[8]));
//    }


    @Override
    public String getApi(String form, String to) {
        long currentTime = System.currentTimeMillis();
        String httpArg = "rn=" + currentTime + "list=fx_s" + form.toLowerCase() + to.toLowerCase();
        return httpUrl.concat(httpArg);
    }


    @Override
    public CurrencyRateResult wrapperResult(String response, String form, String to) {
        String[] arr = response.split(",");

        final CurrencyRateResult result = new CurrencyRateResult();
        //如果换算出错，通过 usd 中转
        if (arr.length < 9) {
            result.setSuccess(false);
            return result;
        }

        final BigDecimal bigDecimal = new BigDecimal(arr[8]);
        log.info(" {} 转 {}  最新USDCNY汇率： {}", form, to, bigDecimal);

        result.setSuccess(true);
        result.setFrom(form);
        result.setTo(to);
        result.setRate(bigDecimal);
        return result;
    }


}