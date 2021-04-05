package com.izhiliu.erp.service.item.impl;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.izhiliu.core.common.constant.PlatformEnum;
import com.izhiliu.core.common.constant.ShopeeConstant;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.ProductMetaData;
import com.izhiliu.erp.repository.item.ProductMetaDataRepository;
import com.izhiliu.erp.service.item.PlatformNodeService;
import com.izhiliu.erp.service.item.PlatformService;
import com.izhiliu.erp.service.item.ProductMetaDataService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.PlatformDTO;
import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.mapper.ProductMetaDataMapper;
import com.izhiliu.erp.service.item.mapper.list.ProductMetaDataListMapper;
import com.izhiliu.erp.service.item.module.convert.*;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.*;
import com.izhiliu.erp.util.SnowflakeGenerate;
import com.izhiliu.erp.web.rest.item.param.ClaimParam;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


/**
 * Service Implementation for managing ProductMetaData.
 */
@Service
public class ProductMetaDataServiceImpl implements ProductMetaDataService, ApplicationListener<ApplicationReadyEvent> {
    private final Logger log = LoggerFactory.getLogger(ProductMetaDataServiceImpl.class);

    @Resource
    private ProductMetaDataRepository productMetaDataRepository;

    @Resource
    private ProductMetaDataMapper productMetaDataMapper;

    @Resource
    private ProductMetaDataListMapper productMetaDataListMapper;

    @Resource
    private PlatformService platformService;

    @Resource
    private PlatformNodeService platformNodeService;

    @Resource
    private ShopeeProductService shopeeProductService;

    @Resource
    private ShopeeMetaDataConvert shopeeMetaDataConvert;

    @Resource
    private AlibabaMetaDataConvert alibabaMetaDataConvert;
    @Resource
    private PingduoduoMetaDataConvert pingduoduoMetaDataConvert;
    @Resource
    private E7MetaDataConvert e7MetaDataConvert;
    @Resource
    private TaobaoMetaDataConvert taobaoMetaDataConvert;
    @Resource
    private TianmaoMetaDataConvert tianmaoMetaDataConvert;

    @Resource
    private ExpressMetaDataConvert expressMetaDataConvert;

    @Resource
    private LazadaMetaDataConvert lazadaMetaDataConvert;

    @Resource
    private ShopeeMetaConvertShopee shopeeMetaConvertShopee;

    @Resource
    private AlibabaMetaConvertShopee alibabaMetaConvertShopee;

    @Resource
    private TaobaoMetaConvertShopee taobaoMetaConvertShopee;

    @Resource
    private TianmaoMetaConvertShopee tianmaoMetaConvertShopee;

    @Resource
    private ExpressMetaConvertShopee expressMetaConvertShopee;

    @Resource
    private LazadaMetaConvertShopee lazadaMetaConvertShopee;

    @Resource
    private SnowflakeGenerate snowflakeGenerate;

    private Executor executor;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        executor = Executors.newFixedThreadPool(20);
    }

    /**
     * Save a productMetaData.
     *
     * @param productMetaDataDTO the entity to save
     * @return the persisted entity
     */

    @Override
    public ProductMetaDataDTO save(ProductMetaDataDTO productMetaDataDTO) {
        log.debug("Request to save ProductMetaData : {}", productMetaDataDTO);
        ProductMetaData productMetaData = productMetaDataMapper.toEntity(productMetaDataDTO);
        productMetaData = productMetaDataRepository.save(productMetaData);
        return productMetaDataMapper.toDto(productMetaData);
    }

    /**
     * Get all the productMetaData.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */

    @Override
    @Transactional(readOnly = true)
    public Page<ProductMetaDataDTO> findAll(Pageable pageable) {
        log.debug("Request to get all ProductMetaData");
        return productMetaDataRepository.findAll(pageable)
            .map(productMetaDataMapper::toDto);
    }

    /**
     * Get one productMetaData by id.
     *
     * @param id the id of the entity
     * @return the entity
     */

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Cacheable(cacheNames = "ProductMetaDataService",unless = "#result == null")
    public Optional<ProductMetaDataDTO> findOne(String id) {
        log.debug("Request to get ProductMetaData : {}", id);
        return productMetaDataRepository.findById(id)
            .map(productMetaDataMapper::toDto);
    }

    /**
     * Delete the productMetaData by id.
     *
     * @param ids the id of the entity
     */

    @Override
    public void delete(List<String> ids) {
        log.debug("Request to delete ProductMetaData : {}", ids);
        for (String id : ids) {
            productMetaDataRepository.deleteById(id);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ProductListVM> pageByCurrentUser(String name, String loginId, Pageable pageable) {
        final PageRequest page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), new Sort(Sort.Direction.DESC, "collectTime"));
        if (StrUtil.isBlank(name)) {
            return productMetaDataRepository.findAllByLoginId(loginId, page)
                .map(productMetaDataListMapper::toDto);
        } else {
            return productMetaDataRepository.findAllByLoginIdAndNameLike(loginId, name, page)
                .map(productMetaDataListMapper::toDto);
        }
    }


    @Override
    public void toShopee(ShopeeMetaDataConvert.ShopeeMetaData collected, String loginId, MetaDataObject.CollectController collectController) {
        final ProductMetaDataDTO metaData = save(shopeeMetaDataConvert.collect(collected));
        shopeeMetaConvertShopee.map(metaData, loginId,collectController);
    }

    @Override
    public void alibabaToShopee(AlibabaMetaDataConvert.AlibabaMetaData collected, String loginId,MetaDataObject.CollectController collectController) {
        final ProductMetaDataDTO metaData = save(alibabaMetaDataConvert.collect(collected));
        alibabaMetaConvertShopee.map(metaData, loginId,collectController);
    }

    @Override
    public void pingduoduoToShopee(PingduoduoMetaDataConvert.PingduoduoMeteData collected, String loginId,MetaDataObject.CollectController collectController) {
        final ProductMetaDataDTO metaData = save(pingduoduoMetaDataConvert.collect(collected));
        taobaoMetaConvertShopee.map(metaData, loginId,collectController);
    }

    @Override
    public void alibabaToShopee(TaobaoMetaDataConvert.TaoBaoMetaData collected, String loginId,MetaDataObject.CollectController collectController) {
        final ProductMetaDataDTO metaData = save(taobaoMetaDataConvert.collect(collected));
        taobaoMetaConvertShopee.map(metaData, loginId,collectController);
    }
    @Override
    public void alibabaToShopee(TianmaoMetaDataConvert.TianmaoMetaData collected, String loginId, MetaDataObject.CollectController collectController) {
        final ProductMetaDataDTO metaData = save(tianmaoMetaDataConvert.collect(collected));
        tianmaoMetaConvertShopee.map(metaData, loginId,collectController);
    }

    @Override
    public void expressToShopee(ExpressMetaDataConvert.ExpressMetaData collected, String loginId,MetaDataObject.CollectController collectController) {
        ProductMetaDataDTO metaData = save(expressMetaDataConvert.collect(collected));
        expressMetaConvertShopee.map(metaData, loginId,collectController);
    }

    @Override
    public void lazadaToShopee(LazadaMetaDataConvert.LazadaMetaData collected, String loginId, MetaDataObject.CollectController collectController) {
        final ProductMetaDataDTO metaData = save(lazadaMetaDataConvert.collect(collected));
        lazadaMetaConvertShopee.map(metaData, loginId,collectController);
    }

    @Override
    public void e7ToShopee(E7MetaDataConvert.E7MetaData collected, String loginId, MetaDataObject.CollectController collectController) {
        final ProductMetaDataDTO metaData = save(e7MetaDataConvert.collect(collected));
        taobaoMetaConvertShopee.map(metaData, loginId, collectController);
    }

    /**
     * bug 采集后列表不显示
     * <p>
     * 重现:
     * 1. 采集商品 (异步执行 立即返回 返回时清空缓存)
     * 2. 立即刷新列表 (此时之前的缓存被清空了 然而此时还没完成采集)
     * <p>
     * 问题出在异步上
     */

    @Override
    public void collectToShopee(List<String> urls) {
        for (String url : urls.stream()
            .distinct()
            .filter(e -> ReUtil.isMatch(ShopeeConstant.RE_COLLECT_ITEM_URL, e) && platformNodeService.find(ShopeeUtil.nodeId(ShopeeMetaDataConvert.ExtractUtil.extract(e).getSuffix()).longValue()).isPresent())
            .collect(Collectors.toList())) {
            log.info("[采集商品] : {}", url);

            final String loginId = SecurityUtils.currentLogin();
            executor.execute(() -> {
                try {
                    final ProductMetaDataDTO metaData = save(shopeeMetaDataConvert.collect(url, loginId));
                    shopeeMetaConvertShopee.map(metaData, loginId,null);
                } catch (Exception e) {
                    log.error("[采集商品异常]", e);
                }
            });
        }
    }


    @Override
    public void claim(List<ClaimParam> claims) {
        for (ClaimParam claim : claims) {
            /*
             * 商品或平台不存在
             */
            final Optional<ProductMetaData> metaDataExist = productMetaDataRepository.findById(claim.getMetaDataId());
            if (!metaDataExist.isPresent()) {
                log.error("[认领商品] - 源数据不存在: {}", claim.getMetaDataId());
                continue;
            }
            final ProductMetaData metaData = metaDataExist.get();
            final Optional<PlatformDTO> platformExist = platformService.find(metaData.getPlatformId());
            if (!platformExist.isPresent()) {
                log.error("[认领商品] - 平台不存在: {}", metaData.getPlatformId());
                continue;
            }

            /*
             * TODO
             * 不能重复认领同一平台
             *
             * 1. 取出所有子商品
             * 2. 校验子商品是否有属于当前平台的
             */
            boolean exist = false;
            for (ShopeeProductDTO child : shopeeProductService.listByMetaData(metaData.getId())) {
                if (child.getPlatformId().equals(claim.getPlatformId())) {
                    exist = true;
                }
            }
            if (exist) {
                continue;
            }

            /*
             * 来源为 Shopee
             */
            final Long metaPlatformId = metaData.getPlatformId();

            log.info("[认领商品] - {} | from {} to {}", claim.getMetaDataId(), metaPlatformId, claim.getPlatformId());

            final String loginId = SecurityUtils.currentLogin();
            claimSwitch(claim, metaData, metaPlatformId, loginId);
        }
    }

    /**
     * 认领处理
     */
    private void claimSwitch(ClaimParam claim, ProductMetaData metaData, Long metaPlatformId, String loginId) {
        if (metaPlatformId.equals(PlatformEnum.SHOPEE.getCode().longValue())) {
            final long toShopeeId = PlatformEnum.SHOPEE.getCode().longValue();
            if (claim.getPlatformId().equals(toShopeeId)) {
                executor.execute(() -> shopeeMetaConvertShopee.map(productMetaDataMapper.toDto(metaData), loginId,null));
            }
        } else if (metaPlatformId.equals(PlatformEnum.ALIBABA.getCode().longValue())) {
            final long toShopeeId = PlatformEnum.SHOPEE.getCode().longValue();
            if (claim.getPlatformId().equals(toShopeeId)) {
                executor.execute(() -> alibabaMetaConvertShopee.map(productMetaDataMapper.toDto(metaData), loginId,null));
            }
        }
    }

    /**
     * 生成skuCode
     */
    @Override
    public Long getSkuCode(){
        return snowflakeGenerate.skuCode();
    }
}
