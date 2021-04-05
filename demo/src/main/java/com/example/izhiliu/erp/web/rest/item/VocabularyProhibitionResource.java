package com.izhiliu.erp.web.rest.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.izhiliu.core.config.security.SecurityUtils;
import com.izhiliu.erp.domain.item.VocabularyProhibition;
import com.izhiliu.erp.service.item.VocabularyProhibitionDetailService;
import com.izhiliu.erp.service.item.VocabularyProhibitionService;
import com.izhiliu.erp.service.item.dto.VocabularyDTO;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDTO;
import com.izhiliu.erp.service.item.dto.VocabularyProhibitionDetailDTO;
import com.izhiliu.erp.web.rest.discount.qo.VocabularyQO;
import com.izhiliu.erp.web.rest.item.vm.VocabularyVO;
import com.izhiliu.erp.web.rest.util.PaginationUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 15:14
 */
@RestController
@RequestMapping("/api")
public class VocabularyProhibitionResource {

    @Resource
    private VocabularyProhibitionService vocabularyProhibitionService;
    @Resource
    private VocabularyProhibitionDetailService vocabularyProhibitionDetailService;

    /**
     * 查询词汇列表
     *
     * @param qo
     * @return
     */
    @GetMapping("/item/disable/vocabulary")
    public ResponseEntity<VocabularyVO> getVocabularyProhibitionList(VocabularyQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        IPage<VocabularyProhibitionDTO> pageResult = vocabularyProhibitionService.getVocabularyAll(qo);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(pageResult, "");
        List<VocabularyVO> vocabularyVo = vocabularyProhibitionService.conventTOVO(pageResult.getRecords());
        return new ResponseEntity(vocabularyVo, headers, HttpStatus.OK);
    }

    /**
     *
     * 查询词汇详细信息
     *
     * @param qo
     * @return
     */
    @GetMapping("/item/disable/vocabulary/detail")
    public ResponseEntity<VocabularyVO> getVocabularyProhibition(VocabularyQO qo) {
        qo.setLogin(SecurityUtils.currentLogin());
        IPage<VocabularyProhibitionDetailDTO> vocabularyProhibition = vocabularyProhibitionDetailService.getVocabularyProhibitionDetail(qo);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(vocabularyProhibition, "");
        List<VocabularyVO.VocabularyInfo> vocabularyDetailVo = vocabularyProhibitionDetailService.conventTOVO(vocabularyProhibition.getRecords());
        return new ResponseEntity(vocabularyDetailVo, headers, HttpStatus.OK);
    }

    /**
     * 修改词汇信息
     *
     * @param dto
     * @return
     */
    @PostMapping("/item/disable/vocabulary")
    public ResponseEntity<Boolean> updateVocabularyProhibition(@RequestBody VocabularyDTO dto) {
        dto.setLogin(SecurityUtils.currentLogin());
        boolean result = vocabularyProhibitionService.updateVocabularyProhibition(dto);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除词汇列表
     *
     * @param dto
     * @return
     */
    @DeleteMapping("/item/disable/vocabulary")
    public ResponseEntity<Boolean> deleteVocabularyProhibitionList(@RequestBody VocabularyDTO dto) {
        dto.setLogin(SecurityUtils.currentLogin());
        boolean result = vocabularyProhibitionService.deleteVocabularyType(dto);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除词汇
     *
     * @param dto
     * @return
     */
    @DeleteMapping("/item/disable/vocabulary/detail")
    public ResponseEntity<Boolean> deleteVocabularyProhibition(@RequestBody VocabularyDTO dto) {
        dto.setLogin(SecurityUtils.currentLogin());
        boolean result = vocabularyProhibitionDetailService.deleteVocabularyProhibition(dto);
        return ResponseEntity.ok(result);
    }
}
