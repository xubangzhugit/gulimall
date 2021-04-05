package com.izhiliu.erp.web.rest.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.codahale.metrics.annotation.Timed;
import com.izhiliu.erp.service.item.ShopeeCategoryService;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.util.HeaderUtil;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing ShopeeCategory.
 */
@Validated
@RestController
@RequestMapping("/api")
public class ShopeeCategoryResource {

    private final Logger log = LoggerFactory.getLogger(ShopeeCategoryResource.class);

    private static final String ENTITY_NAME = "shopeeCategory";

    @Resource
    private ShopeeCategoryService shopeeCategoryService;

    /**
     * POST  /shopee-categories : Create a new shopeeCategory.
     *
     * @param shopeeCategoryDTO the shopeeCategoryDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeCategoryDTO, or with status 400 (Bad Request) if the shopeeCategory has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/shopee-categories")
    @Timed
    public ResponseEntity<ShopeeCategoryDTO> createShopeeCategory(@RequestBody ShopeeCategoryDTO shopeeCategoryDTO) throws URISyntaxException {
        log.debug("REST request to save ShopeeCategory : {}", shopeeCategoryDTO);
        if (shopeeCategoryDTO.getId() != null) {
            throw new BadRequestAlertException("A new shopeeCategory cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create.shopee.category.idexists");
        }
        ShopeeCategoryDTO result = shopeeCategoryService.save(shopeeCategoryDTO);
        return ResponseEntity.created(new URI("/api/shopee-categories/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /shopee-categories : Updates an existing shopeeCategory.
     *
     * @param shopeeCategoryDTO the shopeeCategoryDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated shopeeCategoryDTO,
     * or with status 400 (Bad Request) if the shopeeCategoryDTO is not valid,
     * or with status 500 (Internal Server Error) if the shopeeCategoryDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/shopee-categories")
    @Timed
    public ResponseEntity<ShopeeCategoryDTO> updateShopeeCategory(@RequestBody ShopeeCategoryDTO shopeeCategoryDTO) throws URISyntaxException {
        log.debug("REST request to update ShopeeCategory : {}", shopeeCategoryDTO);
        if (shopeeCategoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        shopeeCategoryService.update(shopeeCategoryDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, shopeeCategoryDTO.getId().toString())).build();
    }

    /**
     * GET  /shopee-categories : get all the shopeeCategories.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of shopeeCategories in body
     */
    @GetMapping("/shopee-categories")
    @Timed
    public ResponseEntity<List<ShopeeCategoryDTO>> getAllShopeeCategories(Pageable pageable) {
        log.debug("REST request to get a page of ShopeeCategories");
        IPage<ShopeeCategoryDTO> page = shopeeCategoryService.page(PaginationUtil.toIPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-categories");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    /**
     * GET  /shopee-categories/:id : get the "id" shopeeCategory.
     *
     * @param id the id of the shopeeCategoryDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the shopeeCategoryDTO, or with status 404 (Not Found)
     */
    @GetMapping("/shopee-categories/{id}")
    @Timed
    public ResponseEntity<ShopeeCategoryDTO> getShopeeCategory(@PathVariable Long id) {
        log.debug("REST request to get ShopeeCategory : {}", id);
        Optional<ShopeeCategoryDTO> shopeeCategoryDTO = shopeeCategoryService.find(id);
        return ResponseEntity.ok(shopeeCategoryDTO.get());
    }

    /**
     * DELETE  /shopee-categories/:id : delete the "id" shopeeCategory.
     *
     * @param id the id of the shopeeCategoryDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/shopee-categories/{id}")
    @Timed
    public ResponseEntity<Void> deleteShopeeCategory(@PathVariable Long id) {
        log.debug("REST request to delete ShopeeCategory : {}", id);
        shopeeCategoryService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    @GetMapping("/shopee-categories/getAllByTier")
    @Timed
    public ResponseEntity<List<ShopeeCategoryDTO>> getAllByTier(@RequestParam("tier") @NotNull Integer tier, @RequestParam("platformNodeId") @NotNull Long platformNodeId, Pageable pageable) {
        final IPage<ShopeeCategoryDTO> page = shopeeCategoryService.pageByTierAndPlatformNode(tier, platformNodeId, PaginationUtil.toPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-categories/pageByTierAndPlatformNode");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    @GetMapping("/shopee-categories/getAllChild")
    @Timed
    public ResponseEntity<List<ShopeeCategoryDTO>> getAllChild(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam("id") @NotNull Long id, @RequestParam("platformNodeId") @NotNull Long platformNodeId, Pageable pageable) {
        final IPage<ShopeeCategoryDTO> page = shopeeCategoryService.pageByChild(keyword, id, platformNodeId, PaginationUtil.toPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/shopee-categories/pageByChild");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    @GetMapping("/shopee-categories/getForebears/{id}")
    @Timed
    public ResponseEntity<List<ShopeeCategoryDTO>> getForebears(@PathVariable("id") Long id) {
        return new ResponseEntity<>(shopeeCategoryService.listByForebears(id), HttpStatus.OK);
    }
}
