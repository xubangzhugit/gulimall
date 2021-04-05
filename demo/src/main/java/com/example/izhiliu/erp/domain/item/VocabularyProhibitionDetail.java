package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:39
 */
@Data
@TableName("item_forbidden_words_detail")
public class VocabularyProhibitionDetail extends BaseEntity {

    /**
     * 词汇类型ID
     */
    private Long vocabularyId;
    /**
     * 登录名
     */
    private String login;
    /**
     * 词汇名称
     */
    private String vocabularyName;

    private Integer deleted;
}
