package com.izhiliu.erp.config.module.currency.base;

public interface BaseCurrencyRateService {

     String getApi(String form, String to);


     CurrencyRateResult wrapperResult(String response,String form, String to);
}
