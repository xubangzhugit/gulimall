package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.PlatformNode;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity PlatformNode and its DTO PlatformNodeDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface PlatformNodeMapper extends EntityMapper<PlatformNodeDTO, PlatformNode> {



    default PlatformNode fromId(Long id) {
        if (id == null) {
            return null;
        }
        PlatformNode platformNode = new PlatformNode();
        platformNode.setId(id);
        return platformNode;
    }
}
