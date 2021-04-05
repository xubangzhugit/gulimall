package com.izhiliu.erp.config.module.currency.base;

import java.util.Objects;

/**
 * describe: 货币汇率查询接口
 * <p>
 *
 * @author cheng
 * @date 2019/1/3 15:32
 */
public interface CurrencyRateApi {

   CurrencyRateResult currencyConvert(String form, String to);

    enum Currency {
        /**
         * 主打市场货币码
         */
        CNY("CNY", "人民币"),
        TWD("TWD", "台湾币"),
        INR("INR", "印度卢比"),
        IDR("IDR", "印尼盾"),
        MYR("MYR", "马来西亚吉特"),
        PHP("PHP", "菲律宾比索"),
        SGD("SGD", "新加坡元"),
        THB("THB", "泰铢"),
        VND("VND", "越南盾"),
        AED("AED", "阿联酋 迪拉姆"),
        OMR("OMR", "阿曼里亚尔"),
        SAR("SAR", "沙特里亚尔"),
        BRL("BRL", "巴西雷亚尔"),
        USD("USD", "美元"),
        MXN("MXN", "墨西哥");

        public String code;
        public String info;

        Currency(String code, String info) {
            this.code = code;
            this.info = info;
        }


        public static  String  selectCurrency(String currency){
            if(Objects.isNull(currency)){
                return  null;
            }
            for (Currency value : Currency.values()) {
                if(value.code.equalsIgnoreCase(currency)){
                    return  value.code;
                }
            }
            throw  new  RuntimeException("请输入合法的 货币 :".concat(currency));
        }
    }
}
