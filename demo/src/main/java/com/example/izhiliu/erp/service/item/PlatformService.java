package com.izhiliu.erp.service.item;

import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.item.Platform;
import com.izhiliu.erp.service.item.dto.PlatformDTO;

import java.util.List;

/**
 * Service Interface for managing Platform.
 */
public interface PlatformService extends IBaseService<Platform, PlatformDTO> {

    String CACHE_ONE = "platform-one";
    String CACHE_LIST = "platform-list";
    String CACHE_PAGE = "platform-page";
    String CACHE_PAGE$ = "platform-page$";

    List<PlatformDTO> listByCanClaim(String loginId);
}
