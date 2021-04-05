package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.Platform;
import com.izhiliu.erp.service.item.dto.PlatformDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Platform and its DTO PlatformDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface PlatformMapper extends EntityMapper<PlatformDTO, Platform> {



    default Platform fromId(Long id) {
        if (id == null) {
            return null;
        }
        Platform platform = new Platform();
        platform.setId(id);
        return platform;
    }
}
