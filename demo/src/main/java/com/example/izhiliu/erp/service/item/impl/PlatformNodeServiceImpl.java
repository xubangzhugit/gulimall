package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.domain.item.PlatformNode;
import com.izhiliu.erp.repository.item.PlatformNodeRepository;
import com.izhiliu.erp.service.item.PlatformNodeService;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import com.izhiliu.erp.service.item.mapper.PlatformNodeMapper;
import com.izhiliu.erp.service.item.module.handle.HandleProductExceptionInfo;
import com.izhiliu.uaa.feignclient.UaaService;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Service Implementation for managing PlatformNode.
 */
@Service
public class PlatformNodeServiceImpl extends IBaseServiceImpl<PlatformNode, PlatformNodeDTO, PlatformNodeRepository, PlatformNodeMapper> implements PlatformNodeService {

    private final Logger log = LoggerFactory.getLogger(PlatformNodeServiceImpl.class);

    @Resource
    private UaaService uaaService;

    @Autowired
    @Qualifier(value = "handleProductExceptionInfo")
    private HandleProductExceptionInfo handleProductExceptionInfo;

    @Override
    public IPage<PlatformNodeDTO> page(IPage<PlatformNode> page) {
        final IPage<PlatformNode> page2 = repository.selectPage(page, null);
        page2.getRecords().forEach(platformNode -> {
            handleProductExceptionInfo.country(platformNode);
        });
        return toDTO(page2);
    }

    @Override
    public Optional<PlatformNodeDTO> findByCurrency(String currency) {
        return Optional.ofNullable(mapper.toDto(repository.findByCurrency(currency)));
    }

    @Override
    public Optional<PlatformNodeDTO> findByCode(String code) {
        return Optional.ofNullable(mapper.toDto(repository.findByCode(code)));
    }

    @Override
    public List<PlatformNodeDTO> pageByPlatform(Long platformId, String loginId) {
        ResponseEntity<List<ShopeeShopDTO>> shopeeShops = uaaService.getShopeeShopInfoV2(SecurityUtils.getCurrentLogin(), SecurityUtils.isSubAccount());
        return mapper.toDto(repository.pageByPlatformId(new Page(0, Integer.MAX_VALUE), platformId).getRecords().stream()
            .filter(e -> {
                if (shopeeShops.getBody() == null) {
                    return true;
                } else {
                    for (ShopeeShopDTO shopeeShop : shopeeShops.getBody()) {
                        /*
                         * 返回有店铺的站点
                         */
                        if (e.getId().equals(ShopeeUtil.nodeId(shopeeShop.getCountry()))) {
                            return true;
                        }
                    }
                    return false;
                }
            }).peek(platformNode -> handleProductExceptionInfo.country(platformNode))
            .collect(Collectors.toList()));
    }
}
