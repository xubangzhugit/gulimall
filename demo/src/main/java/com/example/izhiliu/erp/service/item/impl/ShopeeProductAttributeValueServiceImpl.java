package com.izhiliu.erp.service.item.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.izhiliu.core.config.security.SecurityInfo;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.item.ShopeeProductAttributeValue;
import com.izhiliu.erp.log.LogConstant;
import com.izhiliu.erp.log.LoggerOp;
import com.izhiliu.erp.repository.item.ShopeeProductAttributeValueRepository;
import com.izhiliu.erp.service.item.ShopeeCategoryService;
import com.izhiliu.erp.service.item.ShopeeProductAttributeValueService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ShopeeAttributeDTO;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductAttributeValueDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.mapper.ShopeeProductAttributeValueMapper;
import com.izhiliu.erp.web.rest.errors.IllegalOperationException;
import com.izhiliu.erp.web.rest.item.param.ProductAttributeValueParam;
import com.izhiliu.open.shopee.open.sdk.api.item.result.GetItemDetailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.izhiliu.core.util.Constants.ANONYMOUS_USER;

/**
 * Service Implementation for managing ShopeeProductAttributeValue.
 */
@Service
public class ShopeeProductAttributeValueServiceImpl extends IBaseServiceImpl<ShopeeProductAttributeValue, ShopeeProductAttributeValueDTO, ShopeeProductAttributeValueRepository, ShopeeProductAttributeValueMapper> implements ShopeeProductAttributeValueService {

    private final Logger log = LoggerFactory.getLogger(ShopeeProductAttributeValueServiceImpl.class);

    @Resource
    private ShopeeCategoryService shopeeCategoryService;

    @Resource
    private ShopeeProductService shopeeProductService;

    @Resource
    private MessageSource messageSource;

    @Resource
    private ShopeeBasicDataServiceImpl shopeeBasicDataService;

    LoggerOp getLoggerOpObejct() {
        return new LoggerOp().start().setKind(LogConstant.SHOPEE_PRODUCT).setType(LogConstant.PUT).setCode(LogConstant.SPU);
    }


    @Override
    public void coverByProduct(ProductAttributeValueParam param) {
        final LoggerOp loggerOpObejct = getLoggerOpObejct();
        final SecurityInfo securityInfo = SecurityUtils.genInfo();
        loggerOpObejct.setLoginId(securityInfo.getCurrentLogin());
        try {
            log.info(loggerOpObejct.setMessage("ProductAttributeValueParam").toString());
            final Long productId = param.getProductId();
            final Optional<ShopeeProductDTO> shopeeProductDTO = shopeeProductService.find(productId);

            checking(param, shopeeProductDTO);

            deleteByProduct(productId);

            if (param.getAttributeValues() == null || param.getAttributeValues().size() == 0) {
                return;
            }

            final List<ShopeeProductAttributeValueDTO> list = param.getAttributeValues().stream()
                .map(e -> handlerAttributeId(e, productId))
                .filter(e -> e.getShopeeAttributeId() != null)
                .collect(Collectors.toList());

            batchSaveOrUpdate(list);
        } catch (Throwable e) {
            log.error(loggerOpObejct.error().setMessage(" put ProductAttributeValueParam error  param "+ JSONObject.toJSONString(param)).toString(),e );
            throw  e;
        }
        log.info(loggerOpObejct.ok().setMessage("ProductAttributeValueParam").toString());
    }


    void checking(ProductAttributeValueParam param, Optional<ShopeeProductDTO> shopeeProductDTO) {
        if (! shopeeProductDTO.isPresent()) {
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
        final ShopeeProductDTO shopeeProductDTO1 = shopeeProductDTO.get();

        String loginId = Objects.equals(shopeeProductDTO1.getType(),3)? SecurityUtils.currentLogin():SecurityUtils.getCurrentLogin();

        if(!shopeeProductDTO1.getLoginId().equals(loginId) && !param.getLoginId().equals(ANONYMOUS_USER)){
            throw new IllegalOperationException("illegal.operation.exception",true);
        }
    }

    /**
     * 处理ID
     *
     * @param e
     * @return
     */
    private ShopeeProductAttributeValueDTO handlerAttributeId(ShopeeProductAttributeValueDTO e, Long productId) {
        final ShopeeProductAttributeValueDTO clone = ObjectUtil.clone(e);
        clone.setProductId(productId);
        clone.setShopeeAttributeId(clone.getAttributeId());
        clone.setAttributeId(null);
        return clone;
    }


    @Transactional(readOnly = true)
    @Override
    public List<ShopeeProductAttributeValueDTO> selectByProduct(long productId) {
        return mapper.toDto(repository.selectByProductId(productId));
    }


    @Override
    public void deleteByAttribute(long attributeId) {
        repository.deleteByAttributeId(attributeId);
    }

    @Override
    public void deleteByAttributeAndValue(long attributeId, String value) {
        repository.deleteByAttributeIdAndValue(attributeId, value);
    }


    @Override
    public void copyShopeeProductAttributeValue(long productId, long copyProductId) {
        deleteByProduct(copyProductId);

        /*
         * 取出源商品的所有属性值, 更换商品ID后保存
         */
        final List<ShopeeProductAttributeValueDTO> attributeValues = selectByProduct(productId).stream()
            .peek(e -> e.setProductId(copyProductId))
            .collect(Collectors.toList());
        batchSave(attributeValues);
    }


    @Override
    public int deleteByProduct(long productId) {
        return repository.deleteByProductId(productId);
    }

    @Override
    public void productResetShopeeCategory(long productId, long copyProductId, long categoryId, long platformNodeId) {
        /*
         * 1. 查出商品的原类目(拿到的是最底层的, 需要拿族谱从顶层开始匹配)
         * 2. 匹配类目成功后继续匹配属性
         */
        if (categoryId != 0L) {
            final List<ShopeeCategoryDTO> forebears = shopeeCategoryService.listByForebears(categoryId);
            if (forebears.size() != 0) {
                long shopeeCategoryParentId = 0;
                for (ShopeeCategoryDTO forebear : forebears) {
                    final Optional<ShopeeCategoryDTO> exist = shopeeCategoryService.findByPlatformNodeAndParentIdAndShopeeCategoryId(platformNodeId, shopeeCategoryParentId, forebear.getShopeeCategoryId());
                    log.info("exist: {}", exist.isPresent());
                    if (exist.isPresent()) {
                        shopeeCategoryParentId = exist.get().getId();
                    } else {
                        shopeeCategoryParentId = -1;
                        break;
                    }
                }

                /*
                 * 映射失败
                 */
                if (shopeeCategoryParentId == -1) {
                    final ShopeeProductDTO product = new ShopeeProductDTO();
                    product.setId(copyProductId);
                    product.setCategoryId(0L);
                    shopeeProductService.update(product);

                    deleteByProduct(copyProductId);
                }
            }
        }
    }

    @Override
    public void checkRequired(long productId, long categoryId, long nodeId) {
        final List<Long> essentials = shopeeBasicDataService.getAllAttributeByCategoryId(categoryId, nodeId).stream().filter(e -> e.getEssential() == 1).map(ShopeeAttributeDTO::getId).collect(Collectors.toList());
        final List<Long> existIds = selectByProduct(productId).stream().map(ShopeeProductAttributeValueDTO::getShopeeAttributeId).collect(Collectors.toList());

        if (existIds.size() < essentials.size()) {
            throw new IllegalOperationException("shopee.must.attribute");
        }

        essentials.removeAll(existIds);

        if (essentials.size() != 0) {
            throw new IllegalOperationException("shopee.must.attribute");

        }
    }

    @Override
    public void saveAttribute(GetItemDetailResult.ItemBean item, long shopeeCategoryId, Long categoryId, Long productId) {
        /*
         * 清空本地
         */

        deleteByProduct(productId);
        if (CommonUtils.isNotBlank(item.getAttributes())) {
            List<ShopeeProductAttributeValueDTO> collect = item.getAttributes().stream().map(attribute -> {
                final ShopeeProductAttributeValueDTO productAttributeValue = new ShopeeProductAttributeValueDTO();
                productAttributeValue.setProductId(productId);
                productAttributeValue.setValue(attribute.getAttributeValue());
                productAttributeValue.setShopeeAttributeId(Integer.toUnsignedLong(attribute.getAttributeId()));
                return productAttributeValue;
            }).collect(Collectors.toList());
            batchSave(collect);
        }
    }
}
