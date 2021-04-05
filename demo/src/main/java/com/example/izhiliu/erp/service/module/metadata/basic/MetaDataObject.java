package com.izhiliu.erp.service.module.metadata.basic;

import com.alibaba.fastjson.JSONObject;
import com.izhiliu.erp.config.module.currency.base.CurrencyRateResult;
import com.izhiliu.erp.service.discount.dto.PricingStrategiesDto;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class MetaDataObject {

    public   static  final  CollectController  COLLECT_CONTROLLER = new  CollectController(true,true,5);


    private  String html;

    private  String content;

    private JSONObject discount;

    private  String collectUrl;


    private CollectController collectController  = COLLECT_CONTROLLER;

    public void setBeforeUseConfiguration() {

         if(Objects.isNull(collectController)){
             collectController = COLLECT_CONTROLLER;
         }
    }

    @Getter
    @Setter
    public static  class  CollectController{

        private  float weight;

        private  boolean isPricingStrategies;

        PricingStrategiesDto pricingStrategiesDto;

        CurrencyRateResult currencyRateResult;

        /**
         *   定价模板id
         */
        private  Long pricing;

        /**
         *   是否采集的是折扣价格
         */
        private  boolean collectDiscount;

        /**
         *  是否采集真实的价格
         */
        private  boolean collectStock;

        /**
         *  设置的默认价格
         */
        private  int    stock;

        public CollectController() {
        }

        public CollectController(boolean collectDiscount, boolean collectStock, int stock) {
            this.collectDiscount = collectDiscount;
            this.collectStock = collectStock;
            this.stock = stock;
        }
    }
}
