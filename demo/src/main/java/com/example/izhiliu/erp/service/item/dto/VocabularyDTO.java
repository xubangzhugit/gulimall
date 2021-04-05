package com.izhiliu.erp.service.item.dto;

import com.izhiliu.erp.web.rest.item.vm.VocabularyVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 15:05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyDTO {
    /**
     * 用户名
     */
    private String login;
    /**
     * 词汇类型ID
     */
    private Long vocabularyTypeId;
    /**
     * 词汇类型名称
     */
    private String vocabularyTypeName;
    /**
     * 词汇类型ID集合
     */
    private List<Long> vocabularyTypeIds;
    /**
     * 词汇ID集合
     */
    private List<Long> vocabularyIds;
    private List<VocabularyInfo> info;

    @Data
    public static class VocabularyInfo {
        /**
         * 词汇ID
         */
        private Long vocabularyId;
        /**
         * 词汇名词
         */
        private String vocabularyName;
    }
}
