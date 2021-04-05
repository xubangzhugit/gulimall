package com.izhiliu.erp.service.discount;


import com.izhiliu.erp.service.discount.dto.ShopeeDiscountParamDto;
import com.izhiliu.open.shopee.open.sdk.api.discount.result.GetDiscountsListResult;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface DiscountPriceService   {

     List<GetDiscountsListResult.Discount> getDiscountsList(Long shopId, int type);

    ShopeeShopDTO checke(Long id);

    List<ShopeeShopDTO> checke(List<Long> ids);

    default  Long getId(){
        return  null;
    };

    ResponseEntity save(ShopeeDiscountParamDto aDto, String login);

    ResponseEntity saveProductOrSku(ShopeeDiscountParamDto aDto, String login);

    ResponseEntity delete(ShopeeDiscountParamDto aDto, String login);

    public ResponseEntity flushCache(List<Long> shopId);
}
