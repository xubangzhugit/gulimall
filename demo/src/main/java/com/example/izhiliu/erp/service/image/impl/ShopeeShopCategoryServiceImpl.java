package com.izhiliu.erp.service.image.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.core.config.security.SecurityInfo;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.core.util.Constants;
import com.izhiliu.erp.config.BodyValidStatus;
import com.izhiliu.erp.config.DingDingInfoMessageSendr;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.domain.item.ShopeeShopCategory;
import com.izhiliu.erp.domain.item.ShopeeShopCategoryItem;
import com.izhiliu.erp.repository.image.ShopeeShopCategoryRepository;
import com.izhiliu.erp.service.discount.impl.DiscountPriceServiceImpl;
import com.izhiliu.erp.service.image.ShopeeShopCategoryService;
import com.izhiliu.erp.service.image.dto.ShopeeShopCategoryDTO;
import com.izhiliu.erp.service.image.mapper.ShopeeShopCategoryMapper;
import com.izhiliu.erp.service.image.result.ShopCategorySelect;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.mapper.ProductListMapper_V2;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.errors.RepeatSubmitException;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V21;
import com.izhiliu.open.shopee.open.sdk.api.category.ShopCategoryApi;
import com.izhiliu.open.shopee.open.sdk.api.category.entity.ShopCategory;
import com.izhiliu.open.shopee.open.sdk.api.category.result.*;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ShopeeShopCategoryServiceImpl extends IBaseServiceImpl<ShopeeShopCategory, ShopeeShopCategoryDTO, ShopeeShopCategoryRepository, ShopeeShopCategoryMapper> implements ShopeeShopCategoryService {

    public static final String AYNC_SHOP_CATEGORY_TO_LOCAL = "sync-shop-category-to-local:";
    @Resource
    RedisLockHelper redisLockHelper;

    @Resource
    DingDingInfoMessageSendr dingDingInfoMessageSendr;

    @Resource
    UaaService uaaService;
    @Resource
    ShopCategoryApi shopCategoryApi;

    @Resource
    ShopeeProductService shopeeProductService;

    @Resource
    protected ProductListMapper_V2 productListMapper_v2;

    @Resource
    SnowflakeGenerate snowflakeGenerate;


    @Override
    public ShopeeShopCategoryDTO checke(Long id) {
        final Optional<ShopeeShopCategoryDTO> shopCategoryDTO = this.find(id);
        final ShopeeShopCategoryDTO boostItemDTO2 = shopCategoryDTO.orElseThrow(() -> new IllegalOperationException("illegal.operation.exception", true));
        final String currentUserLogin = SecurityUtils.currentLogin();
        if (!boostItemDTO2.getLoginId().equals(currentUserLogin) && !currentUserLogin.equals(Constants.ANONYMOUS_USER)) {
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
        return  boostItemDTO2;
    }

    @Override
    public List<ShopeeShopCategoryDTO> checke(List<Long> ids) {
        if(CollectionUtils.isEmpty(ids)){
            return  null;
        }
        final Collection<ShopeeShopCategoryDTO> list = this.list(ids);
        final String currentUserLogin = SecurityUtils.currentLogin();
        for (ShopeeShopCategoryDTO shopeeShopCategoryDTO : list) {
            if (!shopeeShopCategoryDTO.getLoginId().equals(currentUserLogin) && !currentUserLogin.equals(Constants.ANONYMOUS_USER)) {
                throw new IllegalOperationException("illegal.operation.exception",true);
            }
        }
        return  new ArrayList<>(list);
    }

    @Override
    public ShopeeShopCategoryDTO selectById(Long id) {
        final ShopeeShopCategoryDTO checke = checke(id);
        List<Long> productIds = getRepository().selectProductIdsByCategoryId(new Page(1,10), checke.getShopCategoryId());
        final List<Long> collect = productIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(collect)){
            return  checke;
        }
        final List<ShopeeProduct> batchEditProductVMS = shopeeProductService.selectBatchIds(collect);
        final List<ProductListVM_V21> productListVM_v21s = productListMapper_v2.toProductListV3(batchEditProductVMS);
        final IPage<ProductListVM_V21> objectPage = new Page<ProductListVM_V21>().setRecords(productListVM_v21s);
        shopeeProductService.settingMediaInfo(objectPage,collect);
        checke.setBatchGetProduct(objectPage.getRecords());
        return  checke;
    }


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void batchEnabled(Integer status, ArrayList<Long> categoryIds) {
        final List<ShopeeShopCategoryDTO> checke = checke(categoryIds);
        for (ShopeeShopCategoryDTO shopeeShopCategoryDTO : checke) {
            shopeeShopCategoryDTO.setStatus(status);
            final String parentLoginId = SecurityUtils.currentLogin();
            DiscountPriceServiceImpl.executorService.execute(() -> {
                  put(shopeeShopCategoryDTO,parentLoginId);
            });
        }
    }

    @Override
    public IPage<ProductListVM_V21> queryProductByProductId(Long cateagoryId, Long productItemId,Page page) {
        final String currentUserLogin = SecurityUtils.currentLogin();
        IPage<Long> productIds = getRepository().queryProductIdByshopCategoryId(page,currentUserLogin,cateagoryId,productItemId);
        if(CollectionUtils.isEmpty(productIds.getRecords())){
            return new Page<>().setRecords(Collections.EMPTY_LIST);
        }
        final List<Long> records = productIds.getRecords();
        final List<ShopeeProduct> batchEditProductVMS = shopeeProductService.selectBatchIds(records);
        if(CollectionUtils.isEmpty(batchEditProductVMS)){
            return new Page<>().setRecords(Collections.EMPTY_LIST);
        }

        final IPage<ProductListVM_V21> productListVM_v21IPage = new Page<ProductListVM_V21>().setTotal(productIds.getTotal()).setCurrent(productIds.getCurrent()).setPages(productIds.getPages()).setSize(productIds.getSize()).setRecords(productListMapper_v2.toProductListV3(batchEditProductVMS));
        shopeeProductService.settingMediaInfo(productListVM_v21IPage,records);
        return productListVM_v21IPage;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IPage<ShopeeShopCategoryDTO> searchProductByCurrentUser(ShopCategorySelect productSearchStrategy) {
        final SecurityInfo securityInfo = SecurityUtils.genInfo();

        if(!CollectionUtils.isEmpty(productSearchStrategy.getShops())){
            productSearchStrategy.setShops(getLegitimateShops(productSearchStrategy.getShops(), securityInfo));
        }
        if(Objects.nonNull(productSearchStrategy.getField())){
            productSearchStrategy.setSqlField(productSearchStrategy.getSqlFieldString()[productSearchStrategy.getField()]);
        }

        final IPage<ShopeeShopCategory> shopeeShopCategoryPage = getRepository().searchProductByCurrentUser(new Page<>(productSearchStrategy.getPage(), productSearchStrategy.getSize()), securityInfo.getParentLogin(), productSearchStrategy);
        final IPage<ShopeeShopCategoryDTO> shopeeShopCategoryPageDTO = toDTO(shopeeShopCategoryPage);
        shopeeShopCategoryPageDTO.getRecords().forEach(shopeeShopCategoryDTO -> {
            shopeeShopCategoryDTO.setProductCount(getRepository().queryProductCountByshopCategoryId(shopeeShopCategoryDTO.getShopCategoryId()));
        });
        settingShopName(securityInfo.getParentLogin(),shopeeShopCategoryPageDTO);
        return shopeeShopCategoryPageDTO;
    }


    private void settingShopName(String loginId, IPage<ShopeeShopCategoryDTO> shopCategoryDTOIPage) {
        final List<Long> collect = shopCategoryDTOIPage.getRecords().stream().map(ShopeeShopCategoryDTO::getShopId).distinct().collect(Collectors.toList());
        if(!collect.isEmpty()){
            final List<ShopeeShopDTO> body = uaaService.getShopName(collect, loginId).getBody();
            if(!body.isEmpty()){
                shopCategoryDTOIPage.getRecords().forEach(productListVM_v21 -> {
                    boolean isShop = true;
                    final Long shopId = productListVM_v21.getShopId();
                    for (ShopeeShopDTO shopeeShopDTO : body) {
                        if (Objects.equals(shopId, shopeeShopDTO.getShopId())) {
                            productListVM_v21.setShopName(shopeeShopDTO.getShopName());
                            isShop = false;
                        }
                    }
                    if (isShop) {
                        productListVM_v21.setShopName("Undefined");
                    }
                });
            }
        }
    }

    /**
     * 增加
     *
     * @param aDto
     * @return
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResponseEntity doInsert(ShopeeShopCategoryDTO aDto) {
        try {
            final ShopCategory shopCategory = new ShopCategory();
            shopCategory.setName(aDto.getName());
            shopCategory.setStatus(Status.getStatus(aDto.getStatus()).getStatus());
            shopCategory.setSortWeight(aDto.getSort());
            final ShopeeResult<AddResult> addResultShopeeResult = shopCategoryApi.addShopCategory(aDto.getShopId(), shopCategory);
            if (addResultShopeeResult.isResult()) {
                final AddResult data = addResultShopeeResult.getData();
                aDto.setId(snowflakeGenerate.nextId());
                aDto.setShopCategoryId(data.getShopCategoryId());
                final ShopeeShopCategoryDTO save = save(aDto);
                if(Objects.nonNull(save)){
                    aDto.setIsSave(Boolean.TRUE);
                    if(!CollectionUtils.isEmpty(aDto.getProductIds())){
                       DiscountPriceServiceImpl.executorService.execute(() -> {
                             handleItem(aDto, false);
                       });
                    }
                }
                return ResponseEntity.ok(save);
            } else {
                return ResponseEntity.status(500).body(BodyValidStatus.myPackage(addResultShopeeResult.getError().getMsg()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dingDingInfoMessageSendr.send(new StringBuilder("新增店铺类目出现意外了"), e);
            return ResponseEntity.status(500).body(BodyValidStatus.myPackage(e.getMessage()));
        }
    }
    /**
     * 增加
     *
     * @param aDto
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResponseEntity insert(ShopeeShopCategoryDTO aDto) {
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        final List<Long> legitimateShops = getLegitimateShops(aDto.getShopIds(), securityInfo);
        if(CollectionUtils.isEmpty(legitimateShops)){
            return  ResponseEntity.ok(Boolean.TRUE);
        }
        final String parentLoginId = SecurityUtils.currentLogin();
        aDto.setShopIds(null);
        if(legitimateShops.size() == 1){
             aDto.setShopId(legitimateShops.get(0));
             aDto.setLoginId(parentLoginId);
            doInsert(aDto);
        }else{
            boolean isProduct = false;
             Map<Long, List<Long>> collect  = null;
            if(!CollectionUtils.isEmpty(aDto.getShopIds())){
                aDto.setProductIds(null);
                final List<ShopeeProduct> list = shopeeProductService.findList(aDto.getShopIds(), SecurityUtils.currentLogin(), null, Boolean.TRUE);
               if(!CollectionUtils.isEmpty(list)){
                   isProduct = true;
                   collect= list.stream().filter(Objects::nonNull).collect(Collectors.groupingBy(ShopeeProduct::getShopId, Collectors.mapping(ShopeeProduct::getId, Collectors.toList())));
               }
            }

            for (Long legitimateShop : legitimateShops) {
                aDto.setShopId(legitimateShop);
                final ShopeeShopCategoryDTO target = new ShopeeShopCategoryDTO();
                BeanUtils.copyProperties(aDto, target);
                if(isProduct) {
                    final List<Long> longs = collect.get(legitimateShop);
                    target.setProductIds(longs);
                }
                target.setLoginId(parentLoginId);
                DiscountPriceServiceImpl.executorService.execute(() -> {doInsert(target); });
            }
        }
        return  ResponseEntity.ok(Boolean.TRUE);
    }

    /**
     * 修改
     *
     * @param aDto
     * @param parentLoginId
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResponseEntity put(ShopeeShopCategoryDTO aDto, String parentLoginId) {
        try {
            final String loginId = StringUtils.isNotBlank(parentLoginId)?parentLoginId:SecurityUtils.currentLogin();
            final Optional<ShopeeShopCategoryDTO> shopCategoryDTO = find(aDto.getId());
            final ShopeeShopCategoryDTO shopeeShopCategoryDTO = shopCategoryDTO.get();
            aDto.setId(shopeeShopCategoryDTO.getId());
            aDto.setGmtCreate(null);
            aDto.setGmtModified(Instant.now());
            final ShopCategory shopCategory = new ShopCategory();
            shopCategory.setName(aDto.getName());
            shopCategory.setStatus(Status.getStatus(aDto.getStatus()).getStatus());
            shopCategory.setSortWeight(aDto.getSort());
            final ShopeeResult<AddResult> addResultShopeeResult = shopCategoryApi.addShopCategory(aDto.getShopId(), shopCategory);
            if (addResultShopeeResult.isResult()) {
                final AddResult data = addResultShopeeResult.getData();
                aDto.setLoginId(loginId);
                aDto.setShopCategoryId(data.getShopCategoryId());
                final boolean update = update(aDto);
//                if(update){
//                    aDto.setIsSave(Boolean.TRUE);
//                    return  handleItem(aDto,false );
//                }
                return  ResponseEntity.ok(update);
            }
            return ResponseEntity.status(500).body(BodyValidStatus.myPackage(addResultShopeeResult.getError().getMsg()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dingDingInfoMessageSendr.send(new StringBuilder("修改店铺类目出现意外了"), e);
            return ResponseEntity.status(500).body(BodyValidStatus.myPackage(e.getMessage()));
        }
    }


    /**
     * 处理是否 增加 还是 删除 product
     *
     * @param aDto
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResponseEntity handleItem(ShopeeShopCategoryDTO aDto, boolean isLoginId) {
        try {
            if (CollectionUtils.isEmpty(aDto.getProductIds())) {
                return ResponseEntity.status(500).body(BodyValidStatus.myPackage("prodctIds is null"));
            }
            final String loginId = isLoginId ? SecurityUtils.currentLogin() : aDto.getLoginId();
            final List<ShopeeProduct> list =  shopeeProductService.findList(aDto.getProductIds(), loginId, null, Boolean.TRUE);
            if (CollectionUtils.isEmpty(list)) {
                return ResponseEntity.status(500).body(BodyValidStatus.myPackage("find after prodctIds is null"));
            }
            final Optional<ShopeeShopCategoryDTO> shopCategoryDTO = find(aDto.getId());
            final ShopeeShopCategoryDTO shopeeShopCategoryDTO = shopCategoryDTO.get();
            if (aDto.getIsSave()) {
                 save(loginId, list, shopeeShopCategoryDTO);
            } else {
                 delete(loginId,list, shopeeShopCategoryDTO);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dingDingInfoMessageSendr.send(new StringBuilder(" 处理  加入 或者 删除 商品 出现了意外 ").append(e.getMessage()), e);
            return ResponseEntity.status(500).body(BodyValidStatus.myPackage(e.getMessage()));
        }
        return ResponseEntity.ok(Boolean.TRUE);
    }

    void delete(String loginId,List<ShopeeProduct> list, ShopeeShopCategoryDTO shopeeShopCategoryDTO) {
        final ShopeeResult<AddItemsResult> addItemsResultShopeeResult = shopCategoryApi.deleteItems(shopeeShopCategoryDTO.getShopId(), shopeeShopCategoryDTO.getShopCategoryId(), list.stream().map(ShopeeProduct::getShopeeItemId).collect(Collectors.toList()));
        if (addItemsResultShopeeResult.isResult()) {
            final List<Long> remoteItemId = addItemsResultShopeeResult.getData().getInvalidItemId();
            final List<Long> invalidItemId;
            if(CollectionUtils.isEmpty(remoteItemId)){
                invalidItemId  = Collections.emptyList();
            }else{
                invalidItemId = remoteItemId;
            }
            final List<Long> collect = list.stream().filter(shopeeProduct -> !invalidItemId.contains(shopeeProduct.getShopeeItemId())).map(ShopeeProduct::getId).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(collect)) {
                getRepository().deleteItemByShopCategoryIdAndItems(shopeeShopCategoryDTO.getShopCategoryId(),collect,loginId);
            }
        }
    }


    void save(String loginId, List<ShopeeProduct> list, ShopeeShopCategoryDTO shopeeShopCategoryDTO) {
        final ShopeeResult<AddItemsResult> addItemsResultShopeeResult = shopCategoryApi.addItems(shopeeShopCategoryDTO.getShopId(), shopeeShopCategoryDTO.getShopCategoryId(), list.stream().map(ShopeeProduct::getShopeeItemId).collect(Collectors.toList()));
        if (addItemsResultShopeeResult.isResult()) {
            final List<Long> remoteItemId = addItemsResultShopeeResult.getData().getInvalidItemId();
            final List<Long> invalidItemId;
            if(CollectionUtils.isEmpty(remoteItemId)){
                invalidItemId  = Collections.emptyList();
            }else{
                invalidItemId = remoteItemId;
            }
            final List<ShopeeProduct> collect1 = list.stream().filter(shopeeProduct -> !invalidItemId.contains(shopeeProduct.getShopeeItemId())).collect(Collectors.toList());
            final List<ShopeeShopCategoryItem> collect = collect1.stream().map(productId -> {
                final ShopeeShopCategoryItem shopeeShopCategoryItem = new ShopeeShopCategoryItem().setShopeeCategoryId(shopeeShopCategoryDTO.getShopCategoryId()).setShopeeProductId(productId.getId()).setShopeeProductItemId(productId.getShopeeItemId()).setStatus(0);
                shopeeShopCategoryItem.setCategoryId(shopeeShopCategoryDTO.getId());
                shopeeShopCategoryItem.setGmtCreate(Instant.now());
                shopeeShopCategoryItem.setLoginId(loginId);
                return shopeeShopCategoryItem;
            }).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(collect)) {
                getRepository().deleteItemByShopCategoryIdAndItems(null,collect.stream().map(ShopeeShopCategoryItem::getShopeeProductId).collect(Collectors.toList()),loginId);
                getRepository().replace(collect, collect.iterator().next());
            }
        }
    }


    /**
     * 同步对应的 商品
     *
     * @param shopIds
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sync(List<Long> shopIds) {
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        final String parentLoginId = securityInfo.getParentLogin();
        shopIds = getLegitimateShops(shopIds, securityInfo);

        log.info("[拉取覆盖全部到本地] : {}", shopIds);

        Integer count = null;
        for (Long shopId : shopIds) {
            final String key = AYNC_SHOP_CATEGORY_TO_LOCAL + shopId;
            try {
                final boolean success = redisLockHelper.tryCounter(key, 1, TimeUnit.MINUTES);
                if (!success) {
                    if (count == null) {
                        count = 0;
                    }
                    final String value = redisLockHelper.get(key);
                    count += Integer.parseInt(value);
                    continue;
                }
                getRepository().removeShopCategoryByShopId(shopId);
                DiscountPriceServiceImpl.executorService.execute(() ->
                        {
                            doSyncCategory(parentLoginId, shopId, key);
                            redisLockHelper.unlock(key);
                        }
                );
            } catch (Exception e) {
                log.error("[店铺类目同步异常]", e);
                dingDingInfoMessageSendr.send(new StringBuilder("店铺类目同步异常"), e);
                redisLockHelper.unlock(key);
            }
        }
        if (count != null) {
            throw new RepeatSubmitException("repeat.submit.exception.loading", new String[]{(count == 0 ? "请不要频繁操作" : count + "")});
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public  void syncCategory(List<Long> categoreyIds) {
        for (Long categoreyId : categoreyIds) {
            final String key = AYNC_SHOP_CATEGORY_TO_LOCAL + categoreyId;
            final Optional<ShopeeShopCategoryDTO> shopCategoryDTO = find(categoreyId);
            if(shopCategoryDTO.isPresent()){
                try {
                    final boolean success = redisLockHelper.tryCounter(key, 1, TimeUnit.MINUTES);
                    if(success){
                        final ShopeeShopCategoryDTO shopeeShopCategoryDTO = shopCategoryDTO.get();
                        doSyncCategory(SecurityUtils.currentLogin(),shopeeShopCategoryDTO.getShopId(),key);
                    }
                } catch (Exception e) {
                    log.error("[店铺类目同步异常]", e);
                    dingDingInfoMessageSendr.send(new StringBuilder("店铺类目同步异常"), e);
                    redisLockHelper.unlock(key);
                }
            }
        }
    }


    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public  void doSyncCategory(String parentLoginId, Long shopId, String key) {
        int page = 0;
        do {
            page = syncPull(key, shopId, parentLoginId, page, 50);
        }
        while (page > -1);
    }

    List<Long> getLegitimateShops(List<Long> shopIds, SecurityInfo securityInfo) {
        final List<Long> hisShopIds = uaaService.getShopeeShopInfoV2(securityInfo.getCurrentLogin(), securityInfo.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
        shopIds = shopIds.stream().filter(hisShopIds::contains).collect(Collectors.toList());
        return shopIds;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long checkeV2(Long shopId) {
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        final List<Long> hisShopIds = uaaService.getShopeeShopInfoV2(securityInfo.getCurrentLogin(), securityInfo.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
        return hisShopIds.stream().filter(aLong -> Objects.equals(aLong, shopId)).findFirst().orElseThrow(RuntimeException::new);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Long> checkeV2(List<Long> shopIds) {
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        final List<Long> hisShopIds = uaaService.getShopeeShopInfoV2(securityInfo.getCurrentLogin(), securityInfo.isSubAccount()).getBody().stream().map(ShopeeShopDTO::getShopId).collect(Collectors.toList());
        final List<Long> collect = shopIds.stream().filter(shopId -> hisShopIds.contains(shopId)).collect(Collectors.toList());
        return collect;
    }

    private int syncPull(String key, Long shopId, String parentLoginId, int page, int size) {
        try {
            return doSyncPull(shopId, parentLoginId, page, size);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dingDingInfoMessageSendr.send(new StringBuilder("店铺类目同步异常"), e);
            redisLockHelper.unlock(key);
        }
        return -1;
    }

    private int doSyncPull(Long shopId, String parentLoginId, int page, int size) {
        final ShopeeResult<GetShopCategoryResult> shopCategory = shopCategoryApi.getShopCategory(shopId, page, size);
        if (shopCategory.isResult()) {
            final List<GetShopCategoryResult.ShopCategorysBean> shopCategorys = shopCategory.getData().getShopCategorys();
            final List<ShopeeShopCategoryDTO> collect = shopCategorys.stream().map(shopCategorysBean -> {
                final ShopeeShopCategoryDTO shopeeShopCategoryDTO = new ShopeeShopCategoryDTO()
                        .setDeleted(0)
                        .setShopCategoryId(shopCategorysBean.getShopCategoryId())
                        .setShopId(shopId)
                        .setLastSyncIndex(Instant.now().toEpochMilli())
                        .setLoginId(parentLoginId)
                        .setGmtCreate(Instant.now())
                        .setGmtModified(Instant.now())
                        .setName(shopCategorysBean.getName())
                        .setStatus(Status.getStatus(shopCategorysBean.getStatus()).getId())
                        .setSort(shopCategorysBean.getSortWeight());
                shopeeShopCategoryDTO.setId(snowflakeGenerate.nextId());
                return shopeeShopCategoryDTO;
            }).collect(Collectors.toList());
            batchSave(collect);

            DiscountPriceServiceImpl.executorService.execute(() -> {
                for (ShopeeShopCategoryDTO shopeeShopCategoryDTO : collect) {
                    try {
                        syncPullItems(parentLoginId,shopId,shopeeShopCategoryDTO.getId(), shopeeShopCategoryDTO.getShopCategoryId());
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        dingDingInfoMessageSendr.send(new StringBuilder("同步店铺类目异常"), e);
                    }
                }
            });
            if (shopCategory.getData().isMore()) {
                return ++page;
            }
        }
        return -1;
    }


    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void remove(List<Long> ids) {
        try {
            final String currentLogin = SecurityUtils.currentLogin();
            final Collection<ShopeeShopCategoryDTO> list = list(ids);
            for (ShopeeShopCategoryDTO shopeeShopCategoryDTO : list) {
                if (currentLogin.equals(shopeeShopCategoryDTO.getLoginId())) {
                    DiscountPriceServiceImpl.executorService.execute(() -> {
                                try {
                                    final ShopeeResult<DeleteResult> deleteResultShopeeResult = shopCategoryApi.deleteShopCategory(shopeeShopCategoryDTO.getShopId(), shopeeShopCategoryDTO.getShopCategoryId());
                                    if (deleteResultShopeeResult.isResult()) {
                                        delete(shopeeShopCategoryDTO.getId());
                                        getRepository().removeProductByshopCategoryId(shopeeShopCategoryDTO.getShopCategoryId());
                                    }
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                    dingDingInfoMessageSendr.send(new StringBuilder("删除店铺类目异常"), e);
                                }
                            }
                    );
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dingDingInfoMessageSendr.send(new StringBuilder("删除店铺类目异常"), e);
        }
    }


    private void syncPullItems(String parentLoginId, Long shopId, Long categoryId, Long shopCategoryId) {
        final ShopeeResult<GetItemsResult> items = shopCategoryApi.getItems(shopId, shopCategoryId);
        if (items.isResult()) {
            getRepository().removeProductByshopCategoryId(shopCategoryId);
            final List<Long> items1 = items.getData().getItems();
            List<ShopeeProduct> shopeeProductList = shopeeProductService.findShopeeProductList(items1);
            final List<ShopeeShopCategoryItem> collect = items1.stream().map(productItemId -> {
                final ShopeeShopCategoryItem shopeeShopCategoryItem = new ShopeeShopCategoryItem()
                        .setShopeeCategoryId(shopCategoryId)
                        .setCategoryId(categoryId)
                        .setLoginId(parentLoginId)
                        .setShopeeProductItemId(productItemId).setStatus(0);
                shopeeShopCategoryItem.setGmtCreate(Instant.now());
                shopeeProductList.stream().filter(f -> Objects.equals(productItemId, f.getShopeeItemId()))
                        .findFirst()
                        .ifPresent(f -> shopeeShopCategoryItem.setShopeeProductId(f.getId()));
                return shopeeShopCategoryItem;
            }).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(collect)) {
                getRepository().replace(collect, collect.iterator().next());
            }
        }
    }
}
