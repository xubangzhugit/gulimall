package com.izhiliu.erp.domain.item;

import com.baomidou.mybatisplus.annotation.TableName;
import com.izhiliu.core.domain.common.BaseEntity;
import lombok.Data;

/**
 * @author King
 * @version 1.0
 * @date 2021/2/2 14:37
 */
@Data
@TableName("item_forbidden_words")
public class VocabularyProhibition extends BaseEntity {

    /**
     * 登录名
     */
    private String login;
    /**
     * 词汇类型名称
     */
    private String vocabularyType;

    private Integer deleted;
}
