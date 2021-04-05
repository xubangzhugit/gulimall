package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.KyyCategoryRelation;
import com.izhiliu.erp.service.item.dto.KyyCategoryRelationDTO;
import org.mapstruct.Mapper;

/**
 * @author Twilight
 * @date 2021/1/18 17:54
 */
@Mapper(componentModel = "spring", uses = {})
public interface KyyCategoryRelationMapper extends EntityMapper<KyyCategoryRelationDTO, KyyCategoryRelation> {
}
