package com.izhiliu.erp.web.rest.item;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.config.security.SecurityInfo;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.config.BodyValidStatus;
import com.izhiliu.erp.domain.item.ShopeeShopCategory;
import com.izhiliu.erp.service.discount.impl.DiscountPriceServiceImpl;
import com.izhiliu.erp.service.image.ShopeeShopCategoryService;
import com.izhiliu.erp.service.image.dto.ShopeeShopCategoryDTO;
import com.izhiliu.erp.service.image.result.ShopCategorySelect;
import com.izhiliu.erp.web.rest.AbstractController;
import com.izhiliu.erp.web.rest.errors.BadRequestAlertException;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V21;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import com.izhiliu.uaa.feignclient.UaaService;
import io.swagger.annotations.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 
 *
 * @author pengzhen
 * @email pengzhen
 * @date 2019-11-20 10:51:56
 */
@RestController
@Slf4j
@Api(value ="相关接口",description  ="相关接口",tags = "相关接口")
@RequestMapping("/api/shopee/shop/category")
public class ShopeeShopCategoryResource extends AbstractController<ShopeeShopCategory, ShopeeShopCategoryDTO, ShopeeShopCategoryService> {

  @Autowired
  private ShopeeShopCategoryService shopeeShopCategoryService;
  @Autowired
  private UaaService uaaService;

  @Override
  public String info() {
    return getClass().getName();
  }


  @Override
  public ShopeeShopCategoryDTO checke(Long id){
    return  iBaseService.checke(id);
  }

  @PostMapping("/sync")
  public ResponseEntity<Boolean> synchronousShopCategory(@RequestBody()List<Long> shopIds){
    shopeeShopCategoryService.sync(shopIds);
    return  ResponseEntity.ok(Boolean.TRUE);
  }

  @PostMapping("/sync/item")
  public ResponseEntity<Boolean> synchronousCategory(@RequestBody()List<Long> itemIds){
    shopeeShopCategoryService.syncCategory(itemIds);
    return  ResponseEntity.ok(Boolean.TRUE);
  }

  @GetMapping("/list")
  public ResponseEntity<List<ShopeeShopCategoryDTO>> searchProductByCurrentUser(ShopCategorySelect productSearchStrategy){
    //判断是否为子帐号,并且没有传店铺
    if(SecurityUtils.isSubAccount() && CollectionUtils.isEmpty(productSearchStrategy.getShops())){
      productSearchStrategy.setShops(Objects.requireNonNull(uaaService.fetchAllSubAccountShopId(SecurityUtils.getCurrentLogin()).getBody()).stream().map(Long::valueOf).collect(Collectors.toList()));
    }
    final IPage<ShopeeShopCategoryDTO> imageBankAddressDtoIPage = iBaseService.searchProductByCurrentUser(productSearchStrategy);
    HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(imageBankAddressDtoIPage, "/api/shopee/shop/category/list");
    return new ResponseEntity(imageBankAddressDtoIPage.getRecords(), headers, HttpStatus.OK);
  }


  @Override
  public ResponseEntity<Object> save(@RequestBody ShopeeShopCategoryDTO aDto) {
    if (log.isDebugEnabled()) {
      log.debug("REST request to save {} : {}", info(), aDto);
    }
    if (aDto.getId() != null) {
      throw new BadRequestAlertException("A new objcte cannot already have an ID", info(), "bad.request.alert.exception.create.shopee.product.sku.idexists");
    }
    return iBaseService.insert(aDto);
  }

  @Override
  public ResponseEntity<ShopeeShopCategoryDTO> query(@PathVariable("id") @Validated  @NotNull Long id){
    final ShopeeShopCategoryDTO aDto =   iBaseService.selectById(id);
    return ResponseEntity.ok(aDto);
  }

  @Override
  public ResponseEntity update(@RequestBody ShopeeShopCategoryDTO aDto) {
    if (log.isDebugEnabled()) {
      log.debug("REST request to save {} : {}", info(), aDto);
    }
    if (aDto.getId() == null) {
      throw new BadRequestAlertException("A new objcte cannot already have an ID", info(), "bad.request.alert.exception.create.shopee.product.sku.idexists");
    }
    return iBaseService.put(aDto, null);
  }


  @PostMapping("/handle-item")
  public ResponseEntity handleItem(@RequestBody ShopeeShopCategoryDTO aDto) {
    if (log.isDebugEnabled()) {
      log.debug("REST request to save {} : {}", info(), aDto);
    }
    final String currentLogin = SecurityUtils.currentLogin();
    if (aDto.getId() == null) {
      throw new BadRequestAlertException("A new objcte cannot already have an ID", info(), "bad.request.alert.exception.create.shopee.product.sku.idexists");
    }
    aDto.setLoginId(currentLogin);
     DiscountPriceServiceImpl.executorService.execute(() -> {
      iBaseService.handleItem(aDto, false);
    });
    return ResponseEntity.ok(Boolean.TRUE);
  }

  @PostMapping("/batch/enabled/{status}")
  public  ResponseEntity batchEnabled(@PathVariable(value = "status") Integer status,@RequestBody  @NotEmpty ArrayList<Long> categoryIds){
    if(CollectionUtils.isEmpty(categoryIds)){
      return  BodyValidStatus.myPackage(400," categoryIds  is null");
    }
    iBaseService.batchEnabled(status,categoryIds);
    return ResponseEntity.ok(Boolean.TRUE);
  }


  @GetMapping("/all/find")
  public  ResponseEntity list(ShopCategorySelect select){
    final IPage<ProductListVM_V21> productListVM_v21IPage = iBaseService.queryProductByProductId(select.getCateagoryId(), select.getProductItemId(), new Page(select.getPage(), select.getSize()));
    HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(productListVM_v21IPage, "/api/shopee/shop/category/all/find");
    return new ResponseEntity(productListVM_v21IPage.getRecords(), headers, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Boolean> delete(@RequestBody  @NotEmpty ArrayList<Long> deleteId){
     iBaseService.remove(deleteId);
    return ResponseEntity.ok(Boolean.TRUE);
  }
}

