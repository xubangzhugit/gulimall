package com.izhiliu.erp.service.item.mapper;

import com.izhiliu.core.domain.common.EntityMapper;
import com.izhiliu.erp.domain.item.VocabularyProhibitionDetail;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDetailDTO;
import org.mapstruct.Mapper;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:57
 */
@Mapper(componentModel = "spring", uses = {})
public interface VocabularyProhibitionDetailMapper extends EntityMapper<VocabularyProhibitionDetailDTO, VocabularyProhibitionDetail> {
}
