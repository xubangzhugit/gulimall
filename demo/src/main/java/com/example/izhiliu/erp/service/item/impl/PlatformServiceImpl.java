package com.izhiliu.erp.service.item.impl;

import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.domain.item.Platform;
import com.izhiliu.erp.repository.item.PlatformRepository;
import com.izhiliu.erp.service.item.PlatformService;
import com.izhiliu.erp.service.item.dto.PlatformDTO;
import com.izhiliu.erp.service.item.mapper.PlatformMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Service Implementation for managing Platform.
 */
@Service
public class PlatformServiceImpl extends IBaseServiceImpl<Platform, PlatformDTO, PlatformRepository, PlatformMapper> implements PlatformService {

    private final Logger log = LoggerFactory.getLogger(PlatformServiceImpl.class);

    @Override
    public List<PlatformDTO> listByCanClaim(String loginId) {
        return list().stream()
            .filter(e -> {
                log.info("[platform]: {}", e);
                return e.getCanClaim() != null && e.getCanClaim().equals(1);
            })
            .collect(Collectors.toList());
    }
}
