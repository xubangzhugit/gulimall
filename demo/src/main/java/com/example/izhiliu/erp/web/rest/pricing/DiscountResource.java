package com.izhiliu.erp.web.rest.pricing;

import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.config.BodyValidStatus;
import com.izhiliu.erp.service.discount.DiscountPriceService;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountParamDto;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.open.shopee.open.sdk.api.discount.param.GetDiscountsListParam;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;


@Slf4j
@RequestMapping("/api/discount")
@RestController()
public class DiscountResource {


    @Resource
    DiscountPriceService discountPriceService;


    /**
     *    获取 当前 用户当前站点 的 集合
     * @return
     */
    @GetMapping("/list/{shopId}/{type}")
    public ResponseEntity listByplatform(@PathVariable("shopId") Long shopId,@PathVariable(value = "type",required = false) Integer  type){
        checke(shopId);
        if (Objects.isNull(type)){
            type = 3;
        }
        if(GetDiscountsListParam.Status.values().length > type && type > -1){
            return  ResponseEntity.ok(discountPriceService.getDiscountsList(shopId,type));
        }else{
            return ResponseEntity.status(400).body(
                    BodyValidStatus.builder()
                            .code("500")
                            .title("parameter_check_exception")
                            .field("parameter_check_exception")
                            .type("Validated").build());
        }

    }


    public String info() {
           return getClass().getName();
    }


    public ShopeeShopDTO checke(Long id) {
       return discountPriceService.checke(id);
    }


    @PostMapping("")
    public ResponseEntity save(@RequestBody @Validated(ShopeeDiscountParamDto.AddDiscount.class) ShopeeDiscountParamDto aDto){
        if (log.isDebugEnabled()) {
            log.debug("REST request to save {} : {}", info(), aDto);
        }
        final String login = SecurityUtils.currentLogin();
        return   this.discountPriceService.save(aDto,login);
    }

    @DeleteMapping("")
    public ResponseEntity delete(@Validated(ShopeeDiscountParamDto.DeleteDiscount.class) ShopeeDiscountParamDto aDto){
        if (log.isDebugEnabled()) {
            log.debug("REST request to save {} : {}", info(), aDto);
        }
        final String login = SecurityUtils.currentLogin();
        return   this.discountPriceService.delete(aDto,login);
    }


    @PostMapping("/product-or-sku")
    public ResponseEntity saveProductOrSku(@RequestBody @Validated(ShopeeDiscountParamDto.AddDiscountItem.class) ShopeeDiscountParamDto aDto){
        if (log.isDebugEnabled()) {
            log.debug("REST request to save {} : {}", info(), aDto);
        }
        final String login = SecurityUtils.currentLogin();
        return   this.discountPriceService.saveProductOrSku(aDto,login);
    }
    @DeleteMapping("/flush-cache")
    public ResponseEntity flushCache(@RequestParam("shopIds")  List<Long> shopIds){
        if (log.isDebugEnabled()) {
            log.debug("REST request to save {} : {}", info(), shopIds);
        }
        return   this.discountPriceService.flushCache(shopIds);
    }

}
