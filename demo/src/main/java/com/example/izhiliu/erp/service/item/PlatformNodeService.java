package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.PlatformNode;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing PlatformNode.
 */
public interface PlatformNodeService extends IBaseService<PlatformNode, PlatformNodeDTO> {

    String CACHE_ONE = "platform-node-one";
    String CACHE_LIST = "platform-node-list";
    String CACHE_PAGE = "platform-node-page";
    String CACHE_PAGE$ = "platform-node-page$";

    List<PlatformNodeDTO> pageByPlatform(Long platformId, String loginId);

    Optional<PlatformNodeDTO> findByCurrency(String currency);

    Optional<PlatformNodeDTO> findByCode(String code);
}
