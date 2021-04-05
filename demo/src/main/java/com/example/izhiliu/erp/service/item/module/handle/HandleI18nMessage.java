package com.izhiliu.erp.service.item.module.handle;

import com.izhiliu.core.config.internation.InternationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * @Author: louis
 * @Date: 2019/6/27 9:54
 */
@Component("handleI18nMessage")
public class HandleI18nMessage {

    @Autowired
    private MessageSource messageSource;

    public String handle(String code) {
        return code == null ? null : messageSource.getMessage(code, null, InternationUtils.getLocale());
    }
}
