package com.izhiliu.erp.repository.item;

import com.izhiliu.erp.domain.item.CurrencyRate;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/15 15:34
 */
public interface CurrencyRateRepository extends MongoRepository<CurrencyRate, String> {
}
