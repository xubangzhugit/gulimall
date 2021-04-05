package com.izhiliu.erp.service.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.core.domain.common.BaseService;
import com.izhiliu.erp.domain.item.VocabularyProhibition;
import com.izhiliu.erp.service.item.dto.VocabularyDTO;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDTO;
import com.izhiliu.erp.web.rest.discount.qo.VocabularyQO;
import com.izhiliu.erp.web.rest.item.vm.VocabularyVO;

import java.util.List;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:36
 */
public interface VocabularyProhibitionService extends BaseService<VocabularyProhibition, VocabularyProhibitionDTO> {
    /**
     * 获取词汇禁用列表
     * @param qo
     * @return
     */
    IPage<VocabularyProhibitionDTO> getVocabularyAll(VocabularyQO qo);

    boolean updateVocabularyProhibition(VocabularyDTO dto);

    boolean deleteVocabularyType(VocabularyDTO dto);

    List<VocabularyVO> conventTOVO(List<VocabularyProhibitionDTO> records);
}
