package com.izhiliu.erp.web.rest.discount.qo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 15:19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VocabularyQO {

    private String login;
    /**
     * 词汇类型ID
     */
    private Long vocabularyTypeId;
    /**
     * 词汇ID
     */
    private Long vocabularyId;
    /**
     * 词汇类型名称
     */
    private String vocabularyTypeName;
    /**
     * 词汇名称
     */
    private String vocabularyName;

    @NotNull
    private Long page;
    @NotNull
    private Long size;
}
