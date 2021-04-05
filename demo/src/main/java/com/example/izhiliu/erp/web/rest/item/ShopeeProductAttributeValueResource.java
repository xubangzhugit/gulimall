package com.izhiliu.erp.web.rest.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codahale.metrics.annotation.Timed;
import com.izhiliu.erp.service.item.ShopeeProductAttributeValueService;
import com.izhiliu.erp.service.item.dto.ShopeeProductAttributeValueDTO;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.item.param.ProductAttributeValueParam;
import com.izhiliu.erp.web.rest.util.HeaderUtil;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing ShopeeProductAttributeValue.
 */
@RestController
@RequestMapping("/api")
public class ShopeeProductAttributeValueResource {

    private final Logger log = LoggerFactory.getLogger(ShopeeProductAttributeValueResource.class);

    private static final String ENTITY_NAME = "shopeeProductAttributeValue";

    private final ShopeeProductAttributeValueService shopeeProductAttributeValueService;

    public ShopeeProductAttributeValueResource(ShopeeProductAttributeValueService shopeeProductAttributeValueService) {
        this.shopeeProductAttributeValueService = shopeeProductAttributeValueService;
    }

    /**
     * POST  /shopee-product-attribute-options : Create a new shopeeProductAttributeValue.
     *
     * @param shopeeProductAttributeValueDTO the shopeeProductAttributeValueDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeProductAttributeValueDTO, or with status 400 (Bad Request) if the shopeeProductAttributeValue has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/shopee-product-attribute-values")
    @Timed
    public ResponseEntity<ShopeeProductAttributeValueDTO> createShopeeProductAttributeValue(@RequestBody ShopeeProductAttributeValueDTO shopeeProductAttributeValueDTO) throws URISyntaxException {
        log.debug("REST request to save ShopeeProductAttributeValue : {}", shopeeProductAttributeValueDTO);
        if (shopeeProductAttributeValueDTO.getId() != null) {
            throw new BadRequestAlertException("A new shopeeProductAttributeValue cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create.shopee.product.attribute.value.idexists");
        }
        ShopeeProductAttributeValueDTO result = shopeeProductAttributeValueService.save(shopeeProductAttributeValueDTO);
        return ResponseEntity.created(new URI("/api/shopee-product-attribute-options/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /shopee-product-attribute-options : Updates an existing shopeeProductAttributeValue.
     *
     * @param shopeeProductAttributeValueDTO the shopeeProductAttributeValueDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated shopeeProductAttributeValueDTO,
     * or with status 400 (Bad Request) if the shopeeProductAttributeValueDTO is not valid,
     * or with status 500 (Internal Server Error) if the shopeeProductAttributeValueDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/shopee-product-attribute-values")
    @Timed
    public ResponseEntity<ShopeeProductAttributeValueDTO> updateShopeeProductAttributeValue(@RequestBody ShopeeProductAttributeValueDTO shopeeProductAttributeValueDTO) throws URISyntaxException {
        log.debug("REST request to update ShopeeProductAttributeValue : {}", shopeeProductAttributeValueDTO);
        if (shopeeProductAttributeValueDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        shopeeProductAttributeValueService.update(shopeeProductAttributeValueDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, shopeeProductAttributeValueDTO.getId().toString())).build();
    }

    /**
     * GET  /shopee-product-attribute-options : get all the shopeeProductAttributeValues.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of shopeeProductAttributeValues in body
     */
    @GetMapping("/shopee-product-attribute-values")
    @Timed
    public ResponseEntity<List<ShopeeProductAttributeValueDTO>> getAllShopeeProductAttributeValues(Pageable pageable) {
        log.debug("REST request to get a page of ShopeeProductAttributeValues");
        IPage<ShopeeProductAttributeValueDTO> page = shopeeProductAttributeValueService.page(PaginationUtil.toIPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-product-attribute-options");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    /**
     * GET  /shopee-product-attribute-options/:id : get the "id" shopeeProductAttributeValue.
     *
     * @param id the id of the shopeeProductAttributeValueDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the shopeeProductAttributeValueDTO, or with status 404 (Not Found)
     */
    @GetMapping("/shopee-product-attribute-values/{id}")
    @Timed
    public ResponseEntity<ShopeeProductAttributeValueDTO> getShopeeProductAttributeValue(@PathVariable Long id) {
        log.debug("REST request to get ShopeeProductAttributeValue : {}", id);
        Optional<ShopeeProductAttributeValueDTO> shopeeProductAttributeValueDTO = shopeeProductAttributeValueService.find(id);
        return ResponseEntity.ok(shopeeProductAttributeValueDTO.get());
    }

    /**
     * DELETE  /shopee-product-attribute-options/:id : delete the "id" shopeeProductAttributeValue.
     *
     * @param id the id of the shopeeProductAttributeValueDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/shopee-product-attribute-values/{id}")
    @Timed
    public ResponseEntity<Void> deleteShopeeProductAttributeValue(@PathVariable Long id) {
        log.debug("REST request to delete ShopeeProductAttributeValue : {}", id);
        shopeeProductAttributeValueService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    @GetMapping("/shopee-product-attribute-values/getAllByProduct/{productId}")
    @Timed
    public ResponseEntity<List<ShopeeProductAttributeValueDTO>> getAllByProduct(@PathVariable("productId") Long productId) {
        return new ResponseEntity<>(shopeeProductAttributeValueService.selectByProduct(productId), HttpStatus.OK);
    }

    @PostMapping("/shopee-product-attribute-values/batchCreateOrUpdate")
    @Timed
    public ResponseEntity<Boolean> batchCreateOrUpdate(@RequestBody @Valid ProductAttributeValueParam param) {
        shopeeProductAttributeValueService.coverByProduct(param);
        return ResponseEntity.ok(true);
    }
}
