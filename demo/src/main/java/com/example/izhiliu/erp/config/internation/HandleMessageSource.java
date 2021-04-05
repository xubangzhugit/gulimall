package com.izhiliu.erp.config.internation;

import com.izhiliu.core.config.internation.InternationUtils;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.util.Locale;

/**
 * @Author: louis
 * @Date: 2020/7/16 16:01
 */
@Component
public class HandleMessageSource {

    @Resource
    private MessageSource messageSource;


    public String getMessage(String var1, @Nullable Object[] var2) {
        String message = "";
        try {
            message = messageSource.getMessage(var1, var2, InternationUtils.getLocale());
        } catch (Exception e) {
            message = var1;
        }
        return message;
    }

    public String getMessage(String var1) {
        String message = "";
        try {
            message = messageSource.getMessage(var1, null, InternationUtils.getLocale());
        } catch (Exception e) {
            message = var1;
        }
        return message;
    }

    public String getMessage(String var1, Locale var3) {
        String message = "";
        try {
            message = messageSource.getMessage(var1, null, var3);
        } catch (Exception e) {
            message = var1;
        }
        return message;
    }

    public String getMessage(String var1, @org.springframework.lang.Nullable Object[] var2, Locale var3) {
        String message = "";
        try {
            message = messageSource.getMessage(var1, var2, var3);
        } catch (Exception e) {
            message = var1;
        }
        return message;
    }
}
