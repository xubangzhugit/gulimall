package com.izhiliu.erp.web.rest.item;

import com.izhiliu.core.common.ValidList;
import com.izhiliu.erp.service.item.BatchEditProductService;
import com.izhiliu.erp.service.item.dto.BatchEditProductDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;

/**
 * describe:
 *
 * document: https://www.yuque.com/izhiliu/hg4a8o/ur2962
 * <p>
 *
 * @author cheng
 * @date 2019/4/8 19:59
 */
@Validated
@RequestMapping("/api")
@RestController
public class BatchEditProductResource {

    @Resource
    private BatchEditProductService batchEditProductService;

    @GetMapping("/batch-edit-product/getProducts")
    public ResponseEntity<List<BatchEditProductDTO>> getProducts(@RequestParam("productIds") @NotEmpty List<Long> productIds) {
        return ResponseEntity.ok(batchEditProductService.getProducts(productIds));
    }

    @GetMapping("/v2/batch-edit-product/getProducts")
    public ResponseEntity<List<BatchEditProductDTO>> getProductsV2(@RequestParam("productIds") @NotEmpty List<Long> productIds) {
            return ResponseEntity.ok(batchEditProductService.getProductsV2(productIds));
    }

    /**
     * 类目和属性
     */
    @PostMapping("/batch-edit-product/categoryAndAttribute")
    public ResponseEntity<Boolean> categoryAndAttribute(@RequestBody @Validated List<BatchEditProductDTO> products) {
        batchEditProductService.categoryAndAttribute(products);
        return ResponseEntity.ok(true);
    }

    /**
     * 基本信息
     */
    @PostMapping("/batch-edit-product/basic")
    public ResponseEntity<Boolean> basic(@RequestBody @Validated ValidList<BatchEditProductDTO> products) {
        batchEditProductService.basicInfo(products);
        return ResponseEntity.ok(true);
    }

    /**
     * 价格和库存
     */
    @PostMapping("/batch-edit-product/priceAndStock")
    @Deprecated
    public ResponseEntity<Boolean> priceAndStock( @RequestBody @Validated List<BatchEditProductDTO> products) {
        batchEditProductService.priceAndStock(products);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/v2/batch-edit-product/priceAndStock")
    public ResponseEntity<Boolean> priceAndStockV2( @RequestBody @Validated List<BatchEditProductDTO> products) {
        products.stream().forEach(e -> e.getVariationWrapper().setVersion(true));
        batchEditProductService.priceAndStockForBatch(products);
        return ResponseEntity.ok(true);
    }

    /**
     * 物流相关
     */
    @PostMapping("/batch-edit-product/logisticInfo")
    public ResponseEntity<Boolean> logisticInfo(@RequestBody @Validated List<BatchEditProductDTO> products) {
        batchEditProductService.logisticInfo(products);
        return ResponseEntity.ok(true);
    }
}
