package com.izhiliu.erp.service.item.impl;

import com.izhiliu.erp.domain.item.CurrencyRate;
import com.izhiliu.erp.repository.item.CurrencyRateRepository;
import com.izhiliu.erp.service.item.CurrencyRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/15 15:41
 */
@Service
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private final Logger log = LoggerFactory.getLogger(CurrencyRateServiceImpl.class);

    @Resource
    private CurrencyRateRepository currencyRateRepository;

    @Override
    public CurrencyRate save(CurrencyRate currencyRate) {
        return currencyRateRepository.save(currencyRate);
    }

    @Override
    public Optional<CurrencyRate> findOne(String id) {
        return currencyRateRepository.findById(id);
    }

    @Override
    public Page<CurrencyRate> findAll(Pageable pageable) {
        return currencyRateRepository.findAll(pageable);
    }
}
