package com.izhiliu.erp.web.rest.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codahale.metrics.annotation.Timed;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.service.RateLimit;
import com.izhiliu.erp.service.item.PlatformNodeService;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;


/**
 * REST controller for managing PlatformNode.
 */
@Validated
@RestController
@RequestMapping("/api")
public class PlatformNodeResource {

    private final Logger log = LoggerFactory.getLogger(PlatformNodeResource.class);

    private static final String ENTITY_NAME = "platformNode";

    private final PlatformNodeService platformNodeService;

    public PlatformNodeResource(PlatformNodeService platformNodeService) {
        this.platformNodeService = platformNodeService;
    }

    /**
     * POST  /platform-nodes : Create a new platformNode.
     *
     * @param platformNodeDTO the platformNodeDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new platformNodeDTO, or with status 400 (Bad Request) if the platformNode has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/platform-nodes")
    @Timed
    public ResponseEntity<PlatformNodeDTO> createPlatformNode(@RequestBody PlatformNodeDTO platformNodeDTO) throws URISyntaxException {
        log.debug("REST request to save PlatformNode : {}", platformNodeDTO);
        if (platformNodeDTO.getId() != null) {
            throw new BadRequestAlertException("A new platformNode cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create_platform_node.idexists");
        }
        PlatformNodeDTO result = platformNodeService.save(platformNodeDTO);
        return ResponseEntity.created(new URI("/api/platform-nodes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /platform-nodes : Updates an existing platformNode.
     *
     * @param platformNodeDTO the platformNodeDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated platformNodeDTO,
     * or with status 400 (Bad Request) if the platformNodeDTO is not valid,
     * or with status 500 (Internal Server Error) if the platformNodeDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/platform-nodes")
    @Timed
    public ResponseEntity<PlatformNodeDTO> updatePlatformNode(@RequestBody PlatformNodeDTO platformNodeDTO) throws URISyntaxException {
        log.debug("REST request to update PlatformNode : {}", platformNodeDTO);
        if (platformNodeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        platformNodeService.update(platformNodeDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, platformNodeDTO.getId().toString())).build();
    }

    /**
     * GET  /platform-nodes : get all the platformNodes.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of platformNodes in body
     */
    @GetMapping("/platform-nodes")
    @Timed
    public ResponseEntity<List<PlatformNodeDTO>> getAllPlatformNodes(Pageable pageable) {
        log.debug("REST request to get a page of PlatformNodes");
        IPage<PlatformNodeDTO> page = platformNodeService.page(PaginationUtil.toIPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/platform-nodes");
        return new ResponseEntity<>(((IPage) page).getRecords(), headers, HttpStatus.OK);
    }

    /**
     * GET  /platform-nodes/:id : get the "id" platformNode.
     *
     * @param id the id of the platformNodeDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the platformNodeDTO, or with status 404 (Not Found)
     */
    @GetMapping("/platform-nodes/{id}")
    @Timed
    public ResponseEntity<PlatformNodeDTO> getPlatformNode(@PathVariable Long id) {
        log.debug("REST request to get PlatformNode : {}", id);
        Optional<PlatformNodeDTO> platformNodeDTO = platformNodeService.find(id);
        return ResponseEntity.ok(platformNodeDTO.get());
    }

    /**
     * DELETE  /platform-nodes/:id : delete the "id" platformNode.
     *
     * @param id the id of the platformNodeDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/platform-nodes/{id}")
    @Timed
    public ResponseEntity<Void> deletePlatformNode(@PathVariable Long id) {
        log.debug("REST request to delete PlatformNode : {}", id);
        platformNodeService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    @GetMapping("/platform-nodes/getAllByPlatform")
    @Timed
    public ResponseEntity<List<PlatformNodeDTO>> getAllByPlatform(@RequestParam("platformId") @NotNull Long platformId) {
        List<PlatformNodeDTO> list = platformNodeService.pageByPlatform(platformId, SecurityUtils.currentLogin());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(new Page().setPages(0).setSize(Integer.MAX_VALUE).setCurrent(1).setTotal(Integer.toUnsignedLong(list.size())), "/api/platform-nodes");
        return new ResponseEntity<>(list, headers, HttpStatus.OK);
    }

    @GetMapping("/service/platform-nodes/{code}")
    public ResponseEntity<PlatformNodeDTO> getPlatfromByCode(@PathVariable String code) {
        PlatformNodeDTO platformNodeDTO = new PlatformNodeDTO();
        platformNodeService.findByCode(code).ifPresent(e->{
            platformNodeDTO.setName(e.getName());
        });
        return ResponseEntity.ok(platformNodeDTO);
    }
}
