package com.izhiliu.erp.web.rest.item.vm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.ws.BindingType;
import java.util.List;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 15:02
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyVO {
    /**
     * 词汇类型ID
     */
    private Long vocabularyTypeId;
    /**
     * 词汇类型名称
     */
    private String vocabularyTypeName;
    private List<VocabularyInfo> info;

    @Data
    @Builder
    public static class VocabularyInfo {
        /**
         * 词汇类型ID
         */
        private Long vocabularyId;
        /**
         * 词汇名词
         */
        private String vocabularyName;
    }
}
