package com.izhiliu.erp.web.rest.item;

import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.service.item.ProductSearchStrategyService;
import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;
import com.izhiliu.erp.web.rest.item.vm.SearchOptionsVM;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 11:19
 */
@Validated
@RestController
@RequestMapping("/api")
public class ProductSearchStrategyResource {

    @Resource
    private ProductSearchStrategyService productSearchStrategyService;

    @GetMapping("/product-search-strategy/getSearchOption")
    public ResponseEntity<SearchOptionsVM> getSearchOption(@RequestParam(value = "type", defaultValue = "1") Integer type) {
        return ResponseEntity.ok(productSearchStrategyService.getSearchOption(SecurityUtils.getCurrentLogin(), type));
    }

    @GetMapping("/product-search-strategy/getAllByCurrentUser")
    public ResponseEntity<List<ProductSearchStrategyDTO>> getAllByCurrentUser(@RequestParam("type") Integer type, Pageable pageable) {
        final Page<ProductSearchStrategyDTO> page = productSearchStrategyService.getAllByCurrentUser(SecurityUtils.currentLogin(), type, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/product-search-strategy/getAllByCurrentUser");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @PostMapping("/product-search-strategy")
    public ResponseEntity<ProductSearchStrategyDTO> save(@Validated(ProductSearchStrategyDTO.Create.class) @RequestBody ProductSearchStrategyDTO productSearchStrategy) {
        productSearchStrategy.setLoginId(SecurityUtils.currentLogin());
        return ResponseEntity.ok(productSearchStrategyService.save(productSearchStrategy));
    }

    @DeleteMapping("/product-search-strategy/{id}")
    public ResponseEntity<Boolean> delete(@PathVariable("id") String id) {
        productSearchStrategyService.delete(id);
        return ResponseEntity.ok(true);
    }
}
