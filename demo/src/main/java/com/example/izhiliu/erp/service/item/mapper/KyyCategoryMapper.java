package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.KyyCategory;
import com.izhiliu.erp.service.item.dto.KyyCategoryDTO;
import org.mapstruct.Mapper;

/**
 * @author Twilight
 * @date 2021/1/18 16:36
 */
@Mapper(componentModel = "spring", uses = {})
public interface KyyCategoryMapper extends EntityMapper<KyyCategoryDTO, KyyCategory> {
}
