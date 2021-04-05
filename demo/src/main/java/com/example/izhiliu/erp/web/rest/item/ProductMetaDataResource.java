package com.izhiliu.erp.web.rest.item;

import com.codahale.metrics.annotation.Timed;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.item.param.ClaimParam;
import com.izhiliu.erp.web.rest.util.HeaderUtil;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;


/**
 * REST controller for managing ProductMetaData.
 */
@Validated
@RestController
@RequestMapping("/api")
public class ProductMetaDataResource {

    private final Logger log = LoggerFactory.getLogger(ProductMetaDataResource.class);

    private static final String ENTITY_NAME = "productMetaData";

    private final ProductMetaDataService productMetaDataService;

    public ProductMetaDataResource(ProductMetaDataService productMetaDataService) {
        this.productMetaDataService = productMetaDataService;
    }

    /**
     * POST  /product-meta-data : Create a new productMetaData.
     *
     * @param productMetaDataDTO the productMetaDataDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new productMetaDataDTO, or with status 400 (Bad Request) if the productMetaData has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/product-meta-data")
    @Timed
    public ResponseEntity<ProductMetaDataDTO> createProductMetaData(@RequestBody ProductMetaDataDTO productMetaDataDTO) throws URISyntaxException {
        log.debug("REST request to save ProductMetaData : {}", productMetaDataDTO);
        if (productMetaDataDTO.getId() != null) {
            throw new BadRequestAlertException("A new productMetaData cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create_product_meta_data.idexists");
        }
        ProductMetaDataDTO result = productMetaDataService.save(productMetaDataDTO);
        return ResponseEntity.created(new URI("/api/product-meta-data/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * PUT  /product-meta-data : Updates an existing productMetaData.
     *
     * @param productMetaDataDTO the productMetaDataDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated productMetaDataDTO,
     * or with status 400 (Bad Request) if the productMetaDataDTO is not valid,
     * or with status 500 (Internal Server Error) if the productMetaDataDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/product-meta-data")
    @Timed
    public ResponseEntity<ProductMetaDataDTO> updateProductMetaData(@RequestBody ProductMetaDataDTO productMetaDataDTO) throws URISyntaxException {
        log.debug("REST request to update ProductMetaData : {}", productMetaDataDTO);
        if (productMetaDataDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        ProductMetaDataDTO result = productMetaDataService.save(productMetaDataDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, productMetaDataDTO.getId()))
            .body(result);
    }

    /**
     * GET  /product-meta-data : get all the productMetaData.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of productMetaData in body
     */
    @GetMapping("/product-meta-data")
    @Timed
    public ResponseEntity<List<ProductMetaDataDTO>> getAllProductMetaData(Pageable pageable) {
        log.debug("REST request to get a page of ProductMetaData");
        Page<ProductMetaDataDTO> page = productMetaDataService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/product-meta-data");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /product-meta-data/:id : get the "id" productMetaData.
     *
     * @param id the id of the productMetaDataDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the productMetaDataDTO, or with status 404 (Not Found)
     */
    @GetMapping("/product-meta-data/{id}")
    @Timed
    public ResponseEntity<ProductMetaDataDTO> getProductMetaData(@PathVariable String id) {
        log.debug("REST request to get ProductMetaData : {}", id);
        Optional<ProductMetaDataDTO> productMetaDataDTO = productMetaDataService.findOne(id);
        return ResponseEntity.ok(productMetaDataDTO.get());
    }

    /**
     * DELETE  /product-meta-data/:id : delete the "id" productMetaData.
     *
     * @param ids the id of the productMetaDataDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/product-meta-data")
    @Timed
    public ResponseEntity<Void> deleteProductMetaData(@RequestBody @NotEmpty List<String> ids) {
        log.debug("REST request to delete ProductMetaData : {}", ids);
        productMetaDataService.delete(ids);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, ids.toString())).build();
    }

    @GetMapping("/product-meta-data/getAllByCurrentUser")
    @Timed
    public ResponseEntity<List<ProductListVM>> getAllByCurrentUser(@RequestParam(value = "keyword", required = false) String keyword, Pageable pageable) {
        log.debug("REST request to get a page of ProductMetaData");
        Page<ProductListVM> page = productMetaDataService.pageByCurrentUser(keyword, SecurityUtils.currentLogin(), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/product-meta-data/getAllByCurrentUser");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * 采集商品
     */
    @PostMapping("/product-meta-data/collect")
    @Timed
    public ResponseEntity<Void> collect(@RequestBody @NotEmpty List<String> urls) {
        productMetaDataService.collectToShopee(urls);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, urls.toString())).build();
    }

    /**
     * 认领商品
     */
    @PostMapping("/product-meta-data/claim")
    @Timed
    public ResponseEntity<Void> claim(@RequestBody @Valid List<ClaimParam> claims) {
        productMetaDataService.claim(claims);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, claims.toString())).build();
    }

    /**
     * 生成商品13位SkuCode编码
     */
    @GetMapping("/product-meta-data/sku-code")
    @Timed
    public Long getSkuCode(){
        return productMetaDataService.getSkuCode();
    }

}
