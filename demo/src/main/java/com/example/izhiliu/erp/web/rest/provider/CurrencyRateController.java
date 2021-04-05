package com.izhiliu.erp.web.rest.provider;


import com.izhiliu.erp.domain.item.CurrencyRate;
import com.izhiliu.erp.service.item.CurrencyRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class CurrencyRateController {

    @Autowired
    CurrencyRateService currencyRateService;

    @PostMapping("/service/currency-rate")
    Optional<CurrencyRate> save(CurrencyRate currencyRate){
     return    Optional.ofNullable(currencyRateService.save(currencyRate));
    }

    @GetMapping("/service/currency-rate/{login}")
    Optional<CurrencyRate> findOne(@PathVariable("login") String login){
        return    currencyRateService.findOne(login);
    }
}
