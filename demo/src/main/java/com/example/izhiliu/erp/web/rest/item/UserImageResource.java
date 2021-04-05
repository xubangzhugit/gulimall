package com.izhiliu.erp.web.rest.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.codahale.metrics.annotation.Timed;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.service.item.UserImageService;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.item.param.BatchDownloadImageQO;
import com.izhiliu.erp.web.rest.util.HeaderUtil;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import com.izhiliu.erp.service.item.dto.UserImageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * REST controller for managing ShopeeProductImage.
 */
@RestController
@RequestMapping("/api")
public class UserImageResource {

    private final Logger log = LoggerFactory.getLogger(UserImageResource.class);

    private static final String ENTITY_NAME = "shopeeProductImage";

    private final UserImageService shopeeProductImageService;

    public UserImageResource(UserImageService shopeeProductImageService) {
        this.shopeeProductImageService = shopeeProductImageService;
    }

    /**
     * POST  /user-images : Create a new shopeeProductImage.
     *
     * @param shopeeProductImageDTO the shopeeProductImageDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new shopeeProductImageDTO, or with status 400 (Bad Request) if the shopeeProductImage has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/user-image")
    @Timed
    public ResponseEntity<UserImageDTO> createShopeeProductImage(@RequestBody UserImageDTO shopeeProductImageDTO) throws URISyntaxException {
        log.debug("REST request to save ShopeeProductImage : {}", shopeeProductImageDTO);
        if (shopeeProductImageDTO.getId() != null) {
            throw new BadRequestAlertException("A new shopeeProductImage cannot already have an ID", ENTITY_NAME, "bad.request.alert.exception.create.shopee.product.sku.image.idexists");
        }
        shopeeProductImageDTO.setLoginId(SecurityUtils.currentLogin());
        UserImageDTO result = shopeeProductImageService.save(shopeeProductImageDTO);
        return ResponseEntity.created(new URI("/api/user-images/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /user-images : Updates an existing shopeeProductImage.
     *
     * @param shopeeProductImageDTO the shopeeProductImageDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated shopeeProductImageDTO,
     * or with status 400 (Bad Request) if the shopeeProductImageDTO is not valid,
     * or with status 500 (Internal Server Error) if the shopeeProductImageDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/user-image")
    @Timed
    public ResponseEntity<UserImageDTO> updateShopeeProductImage(@RequestBody UserImageDTO shopeeProductImageDTO) throws URISyntaxException {
        log.debug("REST request to update ShopeeProductImage : {}", shopeeProductImageDTO);
        if (shopeeProductImageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "bad.request.alert.exception.idnull");
        }
        shopeeProductImageDTO.setLoginId(SecurityUtils.currentLogin());
        shopeeProductImageService.update(shopeeProductImageDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, shopeeProductImageDTO.getId().toString())).build();
    }

    /**
     * GET  /user-images : get all the shopeeProductImages.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of shopeeProductImages in body
     */
    @GetMapping("/user-image")
    @Timed
    public ResponseEntity<List<UserImageDTO>> getAllShopeeProductImages(Pageable pageable) {
        log.debug("REST request to get a page of ShopeeProductImages");
        IPage<UserImageDTO> page = shopeeProductImageService.page(PaginationUtil.toIPage(pageable));
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/user-images");
        return new ResponseEntity<>(page.getRecords(), headers, HttpStatus.OK);
    }

    /**
     * GET  /user-images/:id : get the "id" shopeeProductImage.
     *
     * @param id the id of the shopeeProductImageDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the shopeeProductImageDTO, or with status 404 (Not Found)
     */
    @GetMapping("/user-image/{id}")
    @Timed
    public ResponseEntity<UserImageDTO> getShopeeProductImage(@PathVariable Long id) {
        log.debug("REST request to get ShopeeProductImage : {}", id);
        Optional<UserImageDTO> shopeeProductImageDTO = shopeeProductImageService.find(id);
        return ResponseEntity.ok(shopeeProductImageDTO.get());
    }

    /**
     * DELETE  /user-images/:id : delete the "id" shopeeProductImage.
     *
     * @param id the id of the shopeeProductImageDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/user-image/{id}")
    @Timed
    public ResponseEntity<Void> deleteShopeeProductImage(@RequestBody @NotEmpty List<Long> id) {
        log.debug("REST request to delete ShopeeProductImage : {}", id);
        shopeeProductImageService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * 更新图片
     *
     * @param productId
     * @param files
     * @return
     */
    @PostMapping("/user-image/upload")
    @Timed
    public ResponseEntity<List<UserImageDTO>> upload(Long productId, @RequestParam("file") MultipartFile[] files) {
        return null;
    }

    /**
     * @apiGroup 商品
     * @apiVersion 2.0.0
     * @api {POST} /batch/download/image 批量下载图片
     * @apiDescription 同步店铺产品评论
     * @apiParam {string[]} url 图片链接*
     * @apiParamExample {json} 示例：
     * {
     *   "url":["https://cdn1.keyouyun.com/3069/img/2021/32ec00c9edb35a113daa3cc733b630ae0","https://cdn1.keyouyun.com/3069/img/2021/1fd3f8f39f76dfe84e61a469988259866"]
     * }
     * @apiSuccessExample response
     * HTTP/1.1 200
     *  {
     *      cGFyZW50QGl6aGlsaXUuY29tQUQ4MDJB
     *  }
     * @apiErrorExample ErrorExample
     * HTTP/1.1 500
     */
    @PostMapping("/batch/download/image")
    public ResponseEntity<String> download(@RequestBody @Valid BatchDownloadImageQO qo){
        final String login = SecurityUtils.currentLogin();
        final String taskId = CommonUtils.getTaskId(login);
        qo.setTaskId(taskId);
        qo.setLogin(login);
        shopeeProductImageService.batchDownloadImage(qo);
        return ResponseEntity.ok(taskId);
    }
}
