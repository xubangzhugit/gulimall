package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.VocabularyProhibition;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDTO;
import org.mapstruct.Mapper;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:56
 */
@Mapper(componentModel = "spring", uses = {})
public interface VocabularyProhibitionMapper extends EntityMapper<VocabularyProhibitionDTO, VocabularyProhibition> {
}
