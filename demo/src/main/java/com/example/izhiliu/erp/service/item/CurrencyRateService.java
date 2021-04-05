package com.izhiliu.erp.service.item;

import com.izhiliu.erp.domain.item.CurrencyRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/15 15:39
 */
public interface CurrencyRateService {

    String CACHE_ONE = "currency-rate-data";
    String CACHE_LIST = "currency-rate-data-list";
    String CACHE_PAGE = "currency-rate-data-page";
    String CACHE_COUNT = "currency-rate-data-count";

    CurrencyRate save(CurrencyRate currencyRate);

    Optional<CurrencyRate> findOne(String id);

    Page<CurrencyRate> findAll(Pageable pageable);
}
