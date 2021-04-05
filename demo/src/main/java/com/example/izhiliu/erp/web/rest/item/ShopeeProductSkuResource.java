package com.izhiliu.erp.web.rest.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.codahale.metrics.annotation.Timed;
import com.izhiliu.core.config.security.SecurityInfo;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.domain.item.ShopeeProductSku;
import com.izhiliu.erp.service.item.ShopeeProductSkuService;
import com.izhiliu.erp.service.item.business.BusinessShopeeProductSkuService;
import com.izhiliu.erp.service.item.dto.ShopeeProductSkuDTO;
import com.izhiliu.erp.util.ValidatorUtils;
import com.izhiliu.erp.web.rest.item.vm.VariationVM;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.util.HeaderUtil;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.groups.Default;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing ShopeeProductSku.
 */
@Validated
@RestController
@RequestMapping("/api")
public class ShopeeProductSkuResource {

    private final Logger log = LoggerFactory.getLogger(ShopeeProductSkuResource.class);

    private static final String ENTITY_NAME = "shopeeProductSku";

    private final ShopeeProductSkuService shopeeProductSkuService;
    @Resource
    BusinessShopeeProductSkuService businessShopeeProductSkuService;


    public ShopeeProductSkuResource(ShopeeProductSkuService shopeeProductSkuService) {
        this.shopeeProductSkuService = shopeeProductSkuService;
    }

    /**
     * POST  /shopee-product-skus : Create a new shopeeProductSku.
     *
     * @param shopeeProductSkuDTO the shopeeProductSkuDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeProductSkuDTO, or with status 400 (Bad Request) if the shopeeProductSku has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/shopee-product-skus")
    @Timed
    public ResponseEntity<ShopeeProductSkuDTO> createShopeeProductSku(@RequestBody ShopeeProductSkuDTO shopeeProductSkuDTO) throws URISyntaxException {
        log.debug("REST request to save ShopeeProductSku : {}", shopeeProductSkuDTO);
        if (shopeeProductSkuDTO.getId() != null) {
            throw new BadRequestAlertException("A new shopeeProductSku cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create.shopee.product.sku.idexists");
        }
        ShopeeProductSkuDTO result = shopeeProductSkuService.save(shopeeProductSkuDTO);
        return ResponseEntity.created(new URI("/api/shopee-product-skus/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /shopee-product-skus : Updates an existing shopeeProductSku.
     *
     * @param shopeeProductSkuDTO the shopeeProductSkuDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated shopeeProductSkuDTO,
     * or with status 400 (Bad Request) if the shopeeProductSkuDTO is not valid,
     * or with status 500 (Internal Server Error) if the shopeeProductSkuDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/shopee-product-skus")
    @Timed
    public ResponseEntity<ShopeeProductSkuDTO> updateShopeeProductSku(@RequestBody ShopeeProductSkuDTO shopeeProductSkuDTO) throws URISyntaxException {
        log.debug("REST request to update ShopeeProductSku : {}", shopeeProductSkuDTO);
        if (shopeeProductSkuDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        shopeeProductSkuService.update(shopeeProductSkuDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, shopeeProductSkuDTO.getId().toString())).build();
    }

    /**
     * GET  /shopee-product-skus : get all the shopeeProductSkus.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of shopeeProductSkus in body
     */
    @GetMapping("/shopee-product-skus")
    @Timed
    public ResponseEntity<List<ShopeeProductSkuDTO>> getAllShopeeProductSkus(Pageable pageable) {
        log.debug("REST request to get a page of ShopeeProductSkus");
        IPage<ShopeeProductSkuDTO> page = shopeeProductSkuService.page(PaginationUtil.toIPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-product-skus");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    /**
     * GET  /shopee-product-skus/:id : get the "id" shopeeProductSku.
     *
     * @param id the id of the shopeeProductSkuDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the shopeeProductSkuDTO, or with status 404 (Not Found)
     */
    @GetMapping("/shopee-product-skus/{id}")
    @Timed
    public ResponseEntity<ShopeeProductSkuDTO> getShopeeProductSku(@PathVariable Long id) {
        log.debug("REST request to get ShopeeProductSku : {}", id);
        Optional<ShopeeProductSkuDTO> shopeeProductSkuDTO = shopeeProductSkuService.find(id);
        return ResponseEntity.ok(shopeeProductSkuDTO.get());
    }

    /**
     * DELETE  /shopee-product-skus/:id : delete the "id" shopeeProductSku.
     *
     * @param id the id of the shopeeProductSkuDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/shopee-product-skus/{id}")
    @Timed
    public ResponseEntity<Void> deleteShopeeProductSku(@PathVariable Long id) {
        log.debug("REST request to delete ShopeeProductSku : {}", id);
        shopeeProductSkuService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    @GetMapping(path = {"/shopee-product-skus/getAllByProduct/{productId}","/service/shopee-product-skus/all-by-product/{productId}"})
    @Timed
    @Deprecated
    public ResponseEntity<VariationVM> getAllByProduct(@PathVariable("productId") Long productId) {
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(shopeeProductSkuService.variationByProduct(productId)));
    }


    @GetMapping( "/v2/shopee-product-skus/getAllByProduct/{productId}")
    @Timed
    public ResponseEntity<VariationVM> getAllByProductV2(@PathVariable("productId") Long productId,@RequestParam(value = "isActualStock",required = false,defaultValue = "false")Boolean isActualStock) {
            return ResponseUtil.wrapOrNotFound(Optional.ofNullable(businessShopeeProductSkuService.variationByProduct(productId,isActualStock)));
    }


    @GetMapping("/service/shopee-product-skus/skus-by-loginId")
    @Timed
    public ResponseEntity<List<ShopeeProductSku>> findSkuListByloginId(@RequestParam("login")String login, @RequestParam("skuCode")String skuCode) {
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(shopeeProductSkuService.findSkuListByloginId(login,skuCode)));
    }

    @PostMapping("/shopee-product-skus/createOrUpdateByProduct")
    @Timed
    @Deprecated
    public ResponseEntity<?> createOrUpdateByProduct(@RequestBody @Valid VariationVM param) {
        try {
            ValidatorUtils.validateBySpring(param, Default.class);
            shopeeProductSkuService.coverTheProduct(param);
            return ResponseEntity.ok(true);
        } catch (BindException e) {
            e.printStackTrace();
            return  ResponseEntity.status(500).body(e.getMessage());
        }

    }

    @PostMapping("/v2/shopee-product-skus/createOrUpdateByProduct")
    @Timed
    public ResponseEntity<?> createOrUpdateByProductV2(@RequestBody @Valid VariationVM param) {
        try {
            final SecurityInfo securityInfo = SecurityUtils.genInfo();
            param.setVersion(true);
            ValidatorUtils.validateBySpring(param, Default.class);
             if (!CollectionUtils.isEmpty(param.getVariations())) {
                 for (VariationVM.Variation variation : param.getVariations()) {
                     variation.setCurrency(null);
                 }
             }
            businessShopeeProductSkuService.coverTheProduct(param,securityInfo.getCurrentLogin() );
            return ResponseEntity.ok(true);
        } catch (BindException e) {
            e.printStackTrace();
            return  ResponseEntity.status(500).body(e.getMessage());
        }

    }



}
