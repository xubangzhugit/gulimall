package com.izhiliu.erp.service.item.impl;

import com.izhiliu.core.common.constant.PlatformNodeEnum;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.repository.item.ProductSearchStrategyRepository;
import com.izhiliu.erp.service.item.ProductSearchStrategyService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ProductSearchStrategyDTO;
import com.izhiliu.erp.service.item.mapper.ProductSearchStrategyMapper;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.erp.web.rest.item.vm.SearchOptionsVM;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 11:15
 */
@Service
public class ProductSearchStrategyServiceImpl implements ProductSearchStrategyService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchStrategyServiceImpl.class);

    @Resource
    private UaaService uaaService;

    @Resource
    private ProductSearchStrategyMapper productSearchStrategyMapper;

    @Resource
    private ProductSearchStrategyRepository productSearchStrategyRepository;

    @Resource
    private ShopeeProductService shopeeProductService;

    @Autowired
    @Qualifier(value = "handleProductExceptionInfo")
    private HandleProductExceptionInfo handleProductExceptionInfo;


    @Override
    public Page<ProductSearchStrategyDTO> getAllByCurrentUser(String loginId, Integer type, Pageable pageable) {
        return productSearchStrategyRepository.findAllByLoginIdAndTypeOrderByIdDesc(loginId, type, pageable).map(productSearchStrategyMapper::toDto);
    }


    @Override
    public ProductSearchStrategyDTO save(ProductSearchStrategyDTO productSearchStrategy) {
        return productSearchStrategyMapper.toDto(productSearchStrategyRepository.save(productSearchStrategyMapper.toEntity(productSearchStrategy)));
    }


    @Override
    public void delete(String id) {
        productSearchStrategyRepository.deleteById(id);
    }

    @Override
    public SearchOptionsVM getSearchOption(String loginId, int type) {
        final SearchOptionsVM vo = new SearchOptionsVM();
        handleProductExceptionInfo.country(vo);
        try {
            Map<String, List<ShopeeShopDTO>> tmp = uaaService.getShopeeShopInfoV2(loginId,SecurityUtils.isSubAccount()).getBody().stream().map(shopeeShopDTO -> {
                ShopeeShopDTO dto = new ShopeeShopDTO();
                dto.setShopId(shopeeShopDTO.getShopId());
                dto.setKyyShopId(shopeeShopDTO.getKyyShopId());
                dto.setCountry(handleProductExceptionInfo.doMessage(Stream.of(PlatformNodeEnum.NODES).filter(platformNodeEnum ->
                    Objects.equals( platformNodeEnum.code,shopeeShopDTO.getCountry())
                ).findFirst().get().suffix));
                dto.setShopName(shopeeShopDTO.getShopName());
                return dto;
            }).collect(Collectors.groupingBy(ShopeeShopDTO::getCountry));

            Map<String, List<ShopeeShopDTO>> map = new HashMap<>();
            if (SecurityUtils.isSubAccount()) {
                ResponseEntity<List<String>> entity = uaaService.fetchAllSubAccountShopId(SecurityUtils.getCurrentLogin());
                List<String> shopIds = entity.getBody();

                if (null != tmp) {
                    tmp.forEach((k, v) -> {
                        Iterator<ShopeeShopDTO> iterator = v.iterator();
                        while (iterator.hasNext()) {
                            ShopeeShopDTO next = iterator.next();
                            if (null != next && null != next.getShopId()) {
                                Long shopId = next.getShopId();
                                if (!shopIds.contains(String.valueOf(shopId))) {
                                    iterator.remove();
                                }
                            }
                        }
                        if (!v.isEmpty()) {
                            map.put(k, v);
                        }
                    });
                }
                vo.setShops(map);
            } else {
                vo.setShops(tmp);
            }
        } catch (Exception e) {
            log.error("[调用UAA获取店铺列表失败] : {}", e);
        }

        if (type == ShopeeProduct.Type.SHOP.code) {
            vo.setLocalStatus(Arrays.asList(LocalProductStatus.STATUS));
            vo.setRemoteStatus(Arrays.asList(ShopeeItemStatus.STATUS));
        } else {
            vo.setSources(shopeeProductService.listBySource(loginId));
        }

        return vo;
    }
}
