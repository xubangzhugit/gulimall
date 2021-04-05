package com.izhiliu.erp.service.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.core.domain.common.BaseService;
import com.izhiliu.erp.domain.item.VocabularyProhibitionDetail;
import com.izhiliu.erp.service.item.dto.VocabularyDTO;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDetailDTO;
import com.izhiliu.erp.web.rest.discount.qo.VocabularyQO;
import com.izhiliu.erp.web.rest.item.vm.VocabularyVO;

import java.util.List;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:40
 */
public interface VocabularyProhibitionDetailService extends BaseService<VocabularyProhibitionDetail, VocabularyProhibitionDetailDTO> {

    /**
     * 根据获取所有的
     * @param dto
     * @return
     */
    List<VocabularyProhibitionDetailDTO> vocabularyDetailAll(VocabularyDTO dto);

    /**
     * 获取禁用词汇
     * @param qo
     * @return
     */
    IPage<VocabularyProhibitionDetailDTO> getVocabularyProhibitionDetail(VocabularyQO qo);

    /**
     * 更新词汇列表
     * @param dto
     * @return
     */
    boolean updateVocabularyProhibitionDetail(VocabularyDTO dto);

    /**
     * 删除词汇
     * @param dto
     * @return
     */
    boolean deleteVocabularyProhibition(VocabularyDTO dto);

    /**
     * 封装查询结果
     * @param vocabularyList
     * @return
     */
    List<VocabularyVO.VocabularyInfo> conventTOVO(List<VocabularyProhibitionDetailDTO> vocabularyList);
}
