//package com.izhiliu.erp.web.rest.item;
//
//import com.baomidou.mybatisplus.core.metadata.IPage;
//import com.codahale.metrics.annotation.Timed;
//import com.izhiliu.erp.service.item.ShopeeAttributeService;
//import com.izhiliu.erp.service.item.ShopeeBasicDataService;
//import com.izhiliu.erp.service.item.dto.ShopeeAttributeDTO;
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
//import javax.annotation.Resource;
//import javax.validation.constraints.NotNull;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.List;
//import java.util.Optional;
//
///**
// * REST controller for managing ShopeeAttribute.
// */
//@Validated
//@RestController
//@RequestMapping("/api")
//public class ShopeeAttributeResource {
//
//    private final Logger log = LoggerFactory.getLogger(ShopeeAttributeResource.class);
//
//    private static final String ENTITY_NAME = "shopeeAttribute";
//
//    @Resource
//    private ShopeeAttributeService shopeeAttributeService;
//
//    /**
//     * POST  /shopee-attributes : Create a new shopeeAttribute.
//     *
//     * @param shopeeAttributeDTO the shopeeAttributeDTO to create
//     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeAttributeDTO, or with status 400 (Bad Request) if the shopeeAttribute has already an ID
//     * @throws URISyntaxException if the Location URI syntax is incorrect
//     */
//    @PostMapping("/shopee-attributes")
//    @Timed
//    public ResponseEntity<ShopeeAttributeDTO> createShopeeAttribute(@RequestBody ShopeeAttributeDTO shopeeAttributeDTO) throws URISyntaxException {
//        log.debug("REST request to save ShopeeAttribute : {}", shopeeAttributeDTO);
//        if (shopeeAttributeDTO.getId() != null) {
//            throw new BadRequestAlertException("A new shopeeAttribute cannot already have an ID", ENTITY_NAME, "idexists");
//        }
//        ShopeeAttributeDTO result = shopeeAttributeService.save(shopeeAttributeDTO);
//        return ResponseEntity.created(new URI("/api/shopee-attributes/" + result.getId()))
//            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
//            .body(result);
//    }
//
//    /**
//     * PUT  /shopee-attributes : Updates an existing shopeeAttribute.
//     *
//     * @param shopeeAttributeDTO the shopeeAttributeDTO to update
//     * @return the ResponseEntity with status 200 (OK) and with body the updated shopeeAttributeDTO,
//     * or with status 400 (Bad Request) if the shopeeAttributeDTO is not valid,
//     * or with status 500 (Internal Server Error) if the shopeeAttributeDTO couldn't be updated
//     * @throws URISyntaxException if the Location URI syntax is incorrect
//     */
//    @PutMapping("/shopee-attributes")
//    @Timed
//    public ResponseEntity<ShopeeAttributeDTO> updateShopeeAttribute(@RequestBody ShopeeAttributeDTO shopeeAttributeDTO) throws URISyntaxException {
//        log.debug("REST request to update ShopeeAttribute : {}", shopeeAttributeDTO);
//        if (shopeeAttributeDTO.getId() == null) {
//            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
//        }
//
//        shopeeAttributeService.update(shopeeAttributeDTO);
//        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, shopeeAttributeDTO.getId().toString())).build();
//    }
//
//    /**
//     * GET  /shopee-attributes : get all the shopeeAttributes.
//     *
//     * @param pageable the pagination information
//     * @return the ResponseEntity with status 200 (OK) and the list of shopeeAttributes in body
//     */
//    @GetMapping("/shopee-attributes")
//    @Timed
//    public ResponseEntity<List<ShopeeAttributeDTO>> getAllShopeeAttributes(Pageable pageable) {
//        log.debug("REST request to get a page of ShopeeAttributes");
//        IPage<ShopeeAttributeDTO> page = shopeeAttributeService.page(PaginationUtil.toIPage(pageable));
//        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-attributes");
//        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
//    }
//
//    /**
//     * GET  /shopee-attributes/:id : get the "id" shopeeAttribute.
//     *
//     * @param id the id of the shopeeAttributeDTO to retrieve
//     * @return the ResponseEntity with status 200 (OK) and with body the shopeeAttributeDTO, or with status 404 (Not Found)
//     */
//    @GetMapping("/shopee-attributes/{id}")
//    @Timed
//    public ResponseEntity<ShopeeAttributeDTO> getShopeeAttribute(@PathVariable Long id) {
//        log.debug("REST request to get ShopeeAttribute : {}", id);
//        Optional<ShopeeAttributeDTO> shopeeAttributeDTO = shopeeAttributeService.find(id);
//        return ResponseEntity.ok(shopeeAttributeDTO.get());
//    }
//
//    /**
//     * DELETE  /shopee-attributes/:id : delete the "id" shopeeAttribute.
//     *
//     * @param id the id of the shopeeAttributeDTO to delete
//     * @return the ResponseEntity with status 200 (OK)
//     */
//    @DeleteMapping("/shopee-attributes/{id}")
//    @Timed
//    public ResponseEntity<Void> deleteShopeeAttribute(@PathVariable Long id) {
//        log.debug("REST request to delete ShopeeAttribute : {}", id);
//        shopeeAttributeService.delete(id);
//        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
//    }
//
//    @GetMapping("/shopee-attributes/getAllByCategory")
//    @Timed
//    public ResponseEntity<List<ShopeeAttributeDTO>> getAllByCategory(@RequestParam("categoryId") @NotNull Long categoryId, Pageable pageable) {
//        final IPage<ShopeeAttributeDTO> page = shopeeAttributeService.pageByCategory(categoryId, PaginationUtil.toPage(pageable));
//        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-attributes/findAllByCategory");
//        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
//    }
//
//    @GetMapping("/shopee-attributes/clearRepeat")
//    public void clearRepeat() {
//        shopeeAttributeService.clearRepeat();
//    }
//}
