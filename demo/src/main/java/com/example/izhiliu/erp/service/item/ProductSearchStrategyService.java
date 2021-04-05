package com.izhiliu.erp.service.item;

import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;
import com.izhiliu.erp.web.rest.item.vm.SearchOptionsVM;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 11:12
 */
public interface ProductSearchStrategyService {

    String CACHE_PAGE = "product-search-strategy-value-page";

    Page<ProductSearchStrategyDTO> getAllByCurrentUser(String loginId, Integer type, Pageable pageable);

    ProductSearchStrategyDTO save(ProductSearchStrategyDTO productSearchStrategy);

    void delete(String id);

    SearchOptionsVM getSearchOption(String loginId, int type);
}
