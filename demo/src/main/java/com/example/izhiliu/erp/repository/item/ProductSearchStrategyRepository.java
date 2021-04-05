package com.izhiliu.erp.repository.item;

import com.izhiliu.erp.domain.item.ProductSearchStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 11:07
 */
public interface ProductSearchStrategyRepository extends MongoRepository<ProductSearchStrategy, String> {

    Page<ProductSearchStrategy> findAllByLoginIdAndTypeOrderByIdDesc(String loginId, int type, Pageable pageable);
}
