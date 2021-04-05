//package com.izhiliu.erp.web.rest.item;
//
//import com.baomidou.mybatisplus.core.metadata.IPage;
//import com.codahale.metrics.annotation.Timed;
//import com.izhiliu.erp.service.item.ShopeeAttributeValueOptionService;
//import com.izhiliu.erp.service.item.dto.ShopeeAttributeValueOptionDTO;
//import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
//import com.izhiliu.erp.web.rest.util.HeaderUtil;
//import com.izhiliu.erp.web.rest.util.PaginationUtil;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import javax.validation.constraints.NotNull;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.List;
//import java.util.Optional;
//
///**
// * REST controller for managing ShopeeAttributeValueOption.
// */
//@Validated
//@RestController
//@RequestMapping("/api")
//public class ShopeeAttributeValueOptionResource {
//
//    private final Logger log = LoggerFactory.getLogger(ShopeeAttributeValueOptionResource.class);
//
//    private static final String ENTITY_NAME = "shopeeAttributeValueOption";
//
//    private final ShopeeAttributeValueOptionService shopeeAttributeValueOptionService;
//
//    public ShopeeAttributeValueOptionResource(ShopeeAttributeValueOptionService shopeeAttributeValueOptionService) {
//        this.shopeeAttributeValueOptionService = shopeeAttributeValueOptionService;
//    }
//
//    /**
//     * POST  /shopee-attribute-value-options : Create a new shopeeAttributeValueOption.
//     *
//     * @param shopeeAttributeValueOptionDTO the shopeeAttributeValueOptionDTO to create
//     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeAttributeValueOptionDTO, or with status 400 (Bad Request) if the shopeeAttributeValueOption has already an ID
//     * @throws URISyntaxException if the Location URI syntax is incorrect
//     */
//    @PostMapping("/shopee-attribute-value-options")
//    @Timed
//    public ResponseEntity<ShopeeAttributeValueOptionDTO> createShopeeAttributeValueOption(@RequestBody ShopeeAttributeValueOptionDTO shopeeAttributeValueOptionDTO) throws URISyntaxException {
//        log.debug("REST request to save ShopeeAttributeValueOption : {}", shopeeAttributeValueOptionDTO);
//        if (shopeeAttributeValueOptionDTO.getId() != null) {
//            throw new BadRequestAlertException("A new shopeeAttributeValueOption cannot already have an ID", ENTITY_NAME, "idexists");
//        }
//        ShopeeAttributeValueOptionDTO result = shopeeAttributeValueOptionService.save(shopeeAttributeValueOptionDTO);
//        return ResponseEntity.created(new URI("/api/shopee-attribute-value-options/" + result.getId()))
//            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
//            .body(result);
//    }
//
//    /**
//     * PUT  /shopee-attribute-value-options : Updates an existing shopeeAttributeValueOption.
//     *
//     * @param shopeeAttributeValueOptionDTO the shopeeAttributeValueOptionDTO to update
//     * @return the ResponseEntity with status 200 (OK) and with body the updated shopeeAttributeValueOptionDTO,
//     * or with status 400 (Bad Request) if the shopeeAttributeValueOptionDTO is not valid,
//     * or with status 500 (Internal Server Error) if the shopeeAttributeValueOptionDTO couldn't be updated
//     * @throws URISyntaxException if the Location URI syntax is incorrect
//     */
//    @PutMapping("/shopee-attribute-value-options")
//    @Timed
//    public ResponseEntity<ShopeeAttributeValueOptionDTO> updateShopeeAttributeValueOption(@RequestBody ShopeeAttributeValueOptionDTO shopeeAttributeValueOptionDTO) throws URISyntaxException {
//        log.debug("REST request to update ShopeeAttributeValueOption : {}", shopeeAttributeValueOptionDTO);
//        if (shopeeAttributeValueOptionDTO.getId() == null) {
//            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
//        }
//        shopeeAttributeValueOptionService.update(shopeeAttributeValueOptionDTO);
//        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, shopeeAttributeValueOptionDTO.getId().toString())).build();
//    }
//
//    /**
//     * GET  /shopee-attribute-value-options : get all the shopeeAttributeValueOptions.
//     *
//     * @param pageable the pagination information
//     * @return the ResponseEntity with status 200 (OK) and the list of shopeeAttributeValueOptions in body
//     */
//    @GetMapping("/shopee-attribute-value-options")
//    @Timed
//    public ResponseEntity<List<ShopeeAttributeValueOptionDTO>> getAllShopeeAttributeValueOptions(Pageable pageable) {
//        log.debug("REST request to get a page of ShopeeAttributeValueOptions");
//        IPage<ShopeeAttributeValueOptionDTO> page = shopeeAttributeValueOptionService.page(PaginationUtil.toIPage(pageable));
//        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-attribute-value-options");
//        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
//    }
//
//    /**
//     * GET  /shopee-attribute-value-options/:id : get the "id" shopeeAttributeValueOption.
//     *
//     * @param id the id of the shopeeAttributeValueOptionDTO to retrieve
//     * @return the ResponseEntity with status 200 (OK) and with body the shopeeAttributeValueOptionDTO, or with status 404 (Not Found)
//     */
//    @GetMapping("/shopee-attribute-value-options/{id}")
//    @Timed
//    public ResponseEntity<ShopeeAttributeValueOptionDTO> getShopeeAttributeValueOption(@PathVariable Long id) {
//        log.debug("REST request to get ShopeeAttributeValueOption : {}", id);
//        Optional<ShopeeAttributeValueOptionDTO> shopeeAttributeValueOptionDTO = shopeeAttributeValueOptionService.find(id);
//        return ResponseEntity.ok(shopeeAttributeValueOptionDTO.get());
//    }
//
//    /**
//     * DELETE  /shopee-attribute-value-options/:id : delete the "id" shopeeAttributeValueOption.
//     *
//     * @param id the id of the shopeeAttributeValueOptionDTO to delete
//     * @return the ResponseEntity with status 200 (OK)
//     */
//    @DeleteMapping("/shopee-attribute-value-options/{id}")
//    @Timed
//    public ResponseEntity<Void> deleteShopeeAttributeValueOption(@PathVariable Long id) {
//        log.debug("REST request to delete ShopeeAttributeValueOption : {}", id);
//        shopeeAttributeValueOptionService.delete(id);
//        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
//    }
//
//    @GetMapping("/shopee-attribute-value-options/getAllByAttribute")
//    @Timed
//    public ResponseEntity<List<ShopeeAttributeValueOptionDTO>> getAllByAttribute(@RequestParam("attributeId") @NotNull Long attributeId, Pageable pageable) {
//        final IPage<ShopeeAttributeValueOptionDTO> page = shopeeAttributeValueOptionService.pageByAttribute(attributeId, PaginationUtil.toPage(pageable));
//        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-attribute-value-options/getAllByAttribute");
//        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
//    }
//
//    @GetMapping("/shopee-attribute-value-options/clearRepeat")
//    @Timed
//    public void clearRepeat() {
//        shopeeAttributeValueOptionService.clearRepeat();
//    }
//
//    @GetMapping("/clearRepeat")
//    public void deleteRepeat() {
//        shopeeAttributeValueOptionService.deleteRepeat();
//    }
//}
