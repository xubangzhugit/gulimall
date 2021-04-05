package com.izhiliu.erp.web.rest.item;

import com.codahale.metrics.annotation.Timed;
import com.izhiliu.erp.service.item.ShopeeBasicDataService;
import com.izhiliu.erp.service.item.dto.ShopeeAttributeDTO;
import com.izhiliu.erp.web.rest.item.param.LogisticsQueryQO;
import com.izhiliu.erp.web.rest.item.result.LogisticsVO;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import com.izhiliu.open.shopee.open.sdk.entity.logistics.LogisticsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/3/7 20:15
 */
@Validated
@RestController
@RequestMapping("/api")
public class ShopeeBasicDataResource {

    private final Logger log = LoggerFactory.getLogger(ShopeeBasicDataResource.class);

    @Resource
    private ShopeeBasicDataService shopeeBasicDataService;

    @GetMapping("/v2/shopee-attributes/getAllByCategory")
    @Timed
    public ResponseEntity<List<ShopeeAttributeDTO>> getAllByCategoryV2(@RequestParam("shopeeCategoryId") @NotNull Long categoryId, @RequestParam("platformNodeId") @NotNull Long platformNodeId) {
        final List<ShopeeAttributeDTO> list = shopeeBasicDataService.getAllAttributeByCategoryId(categoryId, platformNodeId);
        HttpHeaders headers = PaginationUtil.fillTotalCount(list.size());
        return new ResponseEntity<>(list, headers, HttpStatus.OK);
    }

    @GetMapping("/shopee-attributes/getAllByCategory/refresh")
    @Timed
    public ResponseEntity<?> refreshAllAttributeByCategoryIdReCache(@RequestParam("shopeeCategoryId") @NotNull Long categoryId, @RequestParam("platformNodeId") @NotNull Long platformNodeId) {
        return new ResponseEntity<>(shopeeBasicDataService.refreshAllAttributeByCategoryIdReCache(categoryId, platformNodeId), HttpStatus.OK);
    }

    @GetMapping("/getLogistics")
    @Timed
    public ResponseEntity<List<LogisticsResult.LogisticsBean>> getLogistics(@RequestParam("shopId") @NotEmpty List<Long> shopId) {
        return ResponseEntity.ok(shopeeBasicDataService.getLogistics(shopId));
    }

    @GetMapping("/v2/shopee/logistics")
    @Timed
    public ResponseEntity<List<LogisticsResult.LogisticsBean>> getLogisticsV2(@RequestParam("shopId") @NotEmpty List<Long> shopId) {
        return ResponseEntity.ok(shopeeBasicDataService.getLogistics(shopId));
    }

    @GetMapping("/v3/shopee/logistics")
    public ResponseEntity<List<LogisticsVO>> getLogisticsV3(@Validated LogisticsQueryQO qo) {
        return ResponseEntity.ok(shopeeBasicDataService.getLogisticsV3(qo));
    }

    @GetMapping("/v1/shopee/refresh/logistics")
    @Timed
    public ResponseEntity<Boolean> refreshLogistics(@RequestParam("shopId") @NotEmpty List<Long> shopId) {
        return ResponseEntity.ok(shopeeBasicDataService.refreshLogistics(shopId));
    }
}
