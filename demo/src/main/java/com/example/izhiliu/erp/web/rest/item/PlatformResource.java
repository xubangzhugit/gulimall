package com.izhiliu.erp.web.rest.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codahale.metrics.annotation.Timed;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.service.item.PlatformService;
import com.izhiliu.erp.service.item.dto.PlatformDTO;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.util.HeaderUtil;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;


/**
 * REST controller for managing Platform.
 */
@RestController
@RequestMapping("/api")
public class PlatformResource {

    private final Logger log = LoggerFactory.getLogger(PlatformResource.class);

    private static final String ENTITY_NAME = "platform";

    private final PlatformService platformService;

    public PlatformResource(PlatformService platformService) {
        this.platformService = platformService;
    }

    /**
     * POST  /platforms : Create a new platform.
     *
     * @param platformDTO the platformDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new platformDTO, or with status 400 (Bad Request) if the platform has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/platforms")
    @Timed
    public ResponseEntity<PlatformDTO> createPlatform(@RequestBody PlatformDTO platformDTO) throws URISyntaxException {
        log.debug("REST request to save Platform : {}", platformDTO);
        if (platformDTO.getId() != null) {
            throw new BadRequestAlertException("A new platform cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create_platform.idexists");
        }
        PlatformDTO result = platformService.save(platformDTO);
        return ResponseEntity.created(new URI("/api/platforms/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /platforms : Updates an existing platform.
     *
     * @param platformDTO the platformDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated platformDTO,
     * or with status 400 (Bad Request) if the platformDTO is not valid,
     * or with status 500 (Internal Server Error) if the platformDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/platforms")
    @Timed
    public ResponseEntity<PlatformDTO> updatePlatform(@RequestBody PlatformDTO platformDTO) throws URISyntaxException {
        log.debug("REST request to update Platform : {}", platformDTO);
        if (platformDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }

        platformService.update(platformDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, platformDTO.getId().toString())).build();
    }

    /**
     * GET  /platforms : get all the platforms.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of platforms in body
     */
    @GetMapping("/platforms")
    @Timed
    public ResponseEntity<List<PlatformDTO>> getAllPlatforms() {
        log.debug("REST request to get a page of Platforms");
        final List<PlatformDTO> list = platformService.listByCanClaim(SecurityUtils.currentLogin());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(new Page<PlatformNodeDTO>().setPages(0).setSize(Integer.MAX_VALUE).setCurrent(1).setTotal(Integer.toUnsignedLong(list.size())), "/api/platform-nodes");
        return new ResponseEntity<>(list, headers, HttpStatus.OK);
    }

    /**
     * GET  /platforms/:id : get the "id" platform.
     *
     * @param id the id of the platformDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the platformDTO, or with status 404 (Not Found)
     */
    @GetMapping("/platforms/{id}")
    @Timed
    public ResponseEntity<PlatformDTO> getPlatform(@PathVariable Long id) {
        log.debug("REST request to get Platform : {}", id);
        Optional<PlatformDTO> platformDTO = platformService.find(id);
        return ResponseEntity.ok(platformDTO.get());
    }

    /**
     * DELETE  /platforms/:id : delete the "id" platform.
     *
     * @param id the id of the platformDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/platforms/{id}")
    @Timed
    public ResponseEntity<Void> deletePlatform(@PathVariable Long id) {
        log.debug("REST request to delete Platform : {}", id);
        platformService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
