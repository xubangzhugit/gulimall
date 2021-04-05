package com.izhiliu.erp.web.rest.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.codahale.metrics.annotation.Timed;
import com.izhiliu.erp.service.item.ShopeeSkuAttributeService;
import com.izhiliu.erp.service.item.dto.ShopeeSkuAttributeDTO;
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
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing ShopeeSkuAttribute.
 */
@RestController
@RequestMapping("/api")
public class ShopeeSkuAttributeResource {

    private final Logger log = LoggerFactory.getLogger(ShopeeSkuAttributeResource.class);

    private static final String ENTITY_NAME = "shopeeSkuAttribute";

    private final ShopeeSkuAttributeService shopeeSkuAttributeService;

    public ShopeeSkuAttributeResource(ShopeeSkuAttributeService shopeeSkuAttributeService) {
        this.shopeeSkuAttributeService = shopeeSkuAttributeService;
    }

    /**
     * POST  /shopee-sku-attributes : Create a new shopeeSkuAttribute.
     *
     * @param shopeeSkuAttributeDTO the shopeeSkuAttributeDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeSkuAttributeDTO, or with status 400 (Bad Request) if the shopeeSkuAttribute has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/shopee-sku-attributes")
    @Timed
    public ResponseEntity<ShopeeSkuAttributeDTO> createShopeeSkuAttribute(@RequestBody ShopeeSkuAttributeDTO shopeeSkuAttributeDTO) throws URISyntaxException {
        log.debug("REST request to save ShopeeSkuAttribute : {}", shopeeSkuAttributeDTO);
        if (shopeeSkuAttributeDTO.getId() != null) {
            throw new BadRequestAlertException("A new shopeeSkuAttribute cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create.shopee.product.sku.attribute.idexists");
        }
        ShopeeSkuAttributeDTO result = shopeeSkuAttributeService.save(shopeeSkuAttributeDTO);
        return ResponseEntity.created(new URI("/api/shopee-sku-attributes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /shopee-sku-attributes : Updates an existing shopeeSkuAttribute.
     *
     * @param shopeeSkuAttributeDTO the shopeeSkuAttributeDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated shopeeSkuAttributeDTO,
     * or with status 400 (Bad Request) if the shopeeSkuAttributeDTO is not valid,
     * or with status 500 (Internal Server Error) if the shopeeSkuAttributeDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/shopee-sku-attributes")
    @Timed
    public ResponseEntity<ShopeeSkuAttributeDTO> updateShopeeSkuAttribute(@RequestBody ShopeeSkuAttributeDTO shopeeSkuAttributeDTO) throws URISyntaxException {
        log.debug("REST request to update ShopeeSkuAttribute : {}", shopeeSkuAttributeDTO);
        if (shopeeSkuAttributeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        shopeeSkuAttributeService.update(shopeeSkuAttributeDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, shopeeSkuAttributeDTO.getId().toString())).build();
    }

    /**
     * GET  /shopee-sku-attributes : get all the shopeeSkuAttributes.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of shopeeSkuAttributes in body
     */
    @GetMapping("/shopee-sku-attributes")
    @Timed
    public ResponseEntity<List<ShopeeSkuAttributeDTO>> getAllShopeeSkuAttributes(Pageable pageable) {
        log.debug("REST request to get a page of ShopeeSkuAttributes");
        IPage<ShopeeSkuAttributeDTO> page = shopeeSkuAttributeService.page(PaginationUtil.toIPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-sku-attributes");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    /**
     * GET  /shopee-sku-attributes/:id : get the "id" shopeeSkuAttribute.
     *
     * @param id the id of the shopeeSkuAttributeDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the shopeeSkuAttributeDTO, or with status 404 (Not Found)
     */
    @GetMapping("/shopee-sku-attributes/{id}")
    @Timed
    public ResponseEntity<ShopeeSkuAttributeDTO> getShopeeSkuAttribute(@PathVariable Long id) {
        log.debug("REST request to get ShopeeSkuAttribute : {}", id);
        Optional<ShopeeSkuAttributeDTO> shopeeSkuAttributeDTO = shopeeSkuAttributeService.find(id);
        return ResponseEntity.ok(shopeeSkuAttributeDTO.get());
    }

    /**
     * DELETE  /shopee-sku-attributes/:id : delete the "id" shopeeSkuAttribute.
     *
     * @param id the id of the shopeeSkuAttributeDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/shopee-sku-attributes/{id}")
    @Timed
    public ResponseEntity<Void> deleteShopeeSkuAttribute(@PathVariable Long id) {
        log.debug("REST request to delete ShopeeSkuAttribute : {}", id);
        shopeeSkuAttributeService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
