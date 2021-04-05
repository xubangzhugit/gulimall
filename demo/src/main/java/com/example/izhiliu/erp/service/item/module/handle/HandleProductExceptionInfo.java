package com.izhiliu.erp.service.item.module.handle;

import com.izhiliu.core.Exception.AbstractException;
import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatusV2;
import com.izhiliu.erp.domain.enums.enumsclasses.LocalProductStatusClass;
import com.izhiliu.erp.domain.enums.enumsclasses.ShopeeItemStatusClass;
import com.izhiliu.erp.domain.item.PlatformNode;
import com.izhiliu.erp.web.rest.errors.*;
import com.izhiliu.erp.web.rest.item.vm.SearchOptionsVM;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.zalando.problem.AbstractThrowableProblem;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.*;

/**
 * describe: 处理商品异常现象
 * <p>
 *
 * @author cheng
 * @date 2019/3/20 16:43
 */
@Component(value = "handleProductExceptionInfo")
public class HandleProductExceptionInfo {

    @Resource
    private MessageSource messageSource;

    /**
     * 根据key获取信息
     *
     * @param key
     * @return
     */
    public String getMessage(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }

        return key;
    }

    /**
     * 将错误信息存储为Key
     *
     * @param info
     * @return
     */
    public String getMessageKey(String info) {
        if (StringUtils.isBlank(info)) {
            return null;
        }
        if (info.contains("Invalid shopid or Invalid account status")) {
            return messageSource.getMessage("shopee.invalid.shop", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Name is too long")) {
            return messageSource.getMessage("shopee.name.length", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("invalid category id")) {
            return messageSource.getMessage("shopee.invalid.category", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Count of hash tags is more than 18")) {
            return messageSource.getMessage("shopee.description.flag", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Must contains all mandatory")) {
            return messageSource.getMessage("shopee.must.attribute", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("error_unknown") || info.contains("The info you quried doesn't exsit in database") || info.contains("Interal error, please contact openapi team")) {
            return messageSource.getMessage("shopee.serve.error", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Can't edit deleted/invalid items.")) {
            return messageSource.getMessage("shopee.invalid.product", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Item or variation doesn't exist")) {
            return messageSource.getMessage("shopee.invalid.sku", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("price should bigger than 0.1")) {
            return messageSource.getMessage("shopee.price.min", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Variations' price differences are too large. Should be under 10 times") || info.contains("The price differences of variation is more than 10 times")) {
            return messageSource.getMessage("shopee.price.more.than.10.times", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("item is duplicate")) {
            return messageSource.getMessage("shopee.repeat.item", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Remote host closed connection") || info.contains("Connection timed out") || info.contains("HttpException")) {
            return messageSource.getMessage("shopee.api.time.out", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("You provided an invalid variation id")) {
            return messageSource.getMessage("shopee.invalid.variation", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("not in shop logistics list")) {
            return messageSource.getMessage("shopee.logistics.not.found", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("All images download fail")) {
            return messageSource.getMessage("shopee.image.not.download", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Item published item count reaches limit")) {
            return messageSource.getMessage("shopee.max.item.count", null, Locale.SIMPLIFIED_CHINESE);
        }
        if (info.contains("Contains invalid attribute value")) {
            return messageSource.getMessage("shopee.invalid.attribute.value", null, Locale.SIMPLIFIED_CHINESE);
        }

        return info;
    }


    /**
     *
     * @param feature
     * @return
     */
    public String doMessage(String feature) {
        if (Objects.isNull(feature)) {
            return null;
        }
        return doMessage(feature, null);
    }

    /**
     *   带参的真正取获取的方法
     * @param feature
     * @param params
     * @return
     */
    public String doMessage(String feature, String[] params) {
        try {
            return messageSource.getMessage(feature, params, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            e.printStackTrace();
        }
        return feature;
    }


    private static final Map<Class<?>, Field> METADATA = new HashMap<>();
    private static final Map<String, Class<?>> CLASSES = new HashMap<>();


    static {
        try {
            Class<?> clazz = BadRequestAlertException.class;
            Class<?> abstractThrowableProblemClass = AbstractThrowableProblem.class;
            Class<?> illegalOperationExceptionClass = IllegalOperationException.class;
            Class<?> dataNotFoundExceptionClass = DataNotFoundException.class;
            Class<?> dataAlreadyExistedExceptionClass = DataAlreadyExistedException.class;
            Class<?> repeatSubmitExceptionClass = RepeatSubmitException.class;

            CLASSES.put(clazz.getName(), clazz);
            CLASSES.put(abstractThrowableProblemClass.getName(), abstractThrowableProblemClass);
            CLASSES.put(illegalOperationExceptionClass.getName(), illegalOperationExceptionClass);
            CLASSES.put(dataNotFoundExceptionClass.getName(), dataNotFoundExceptionClass);
            CLASSES.put(dataAlreadyExistedExceptionClass.getName(), dataAlreadyExistedExceptionClass);
            CLASSES.put(repeatSubmitExceptionClass.getName(), repeatSubmitExceptionClass);

            Field title = clazz.getSuperclass().getDeclaredField("title");
            title.setAccessible(true);
            METADATA.put(clazz, title);
            METADATA.put(abstractThrowableProblemClass, title);
            METADATA.put(illegalOperationExceptionClass, title);
            METADATA.put(dataNotFoundExceptionClass, title);
            METADATA.put(dataAlreadyExistedExceptionClass, title);
            METADATA.put(repeatSubmitExceptionClass, title);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }



    public LocalProductStatusClass country(LocalProductStatus status) {
         if(Objects.isNull(status)){
            return  null;
        }
        final LocalProductStatusV2 values = LocalProductStatusV2.getValues(status.getValue());
        return new LocalProductStatusClass().setStatusCode(status.getValue()).setStatusName(doMessage(values.getStatus()));
    }
    public String doCountry(LocalProductStatus status) {
        if(Objects.isNull(status)){
            return  null;
        }
         final LocalProductStatusV2 values = LocalProductStatusV2.getValues(status.getValue());
         return doMessage(values.getStatus());
    }

    public ShopeeItemStatusClass country(ShopeeItemStatus status) {
        if(Objects.isNull(status)){
            return  null;
        }
        return new ShopeeItemStatusClass().setShopeeItemStatusCode(status.getValue()).setShopeeItemStatusName(doMessage(status.msg));
    }
    public String doCountry(ShopeeItemStatus status) {
        return doMessage(status.getMsg());
    }

    public PlatformNode country(PlatformNode platformNode) {
        platformNode.setName(doMessage(platformNode.getFeature()));
        return platformNode;
    }

    /**
     *
     * @param vo
     */
    public void country(SearchOptionsVM vo) {
        List<SearchOptionsVM.Entry> fields = vo.getFields();
        for (SearchOptionsVM.Entry field : fields) {
            field.setDisplayName(doMessage(field.getCountry()));
        }
    }






    public void country(@NonNull BadRequestAlertException x) {
        try {
            String errorKey = x.getErrorKey();
            if (Objects.nonNull(errorKey)) {
                final String s = doMessage(errorKey);
                final Field field = METADATA.get(CLASSES.get(x.getClass().getName()));
                field.set(x, s);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    public void country(AbstractThrowableProblem x) {
        try {
            String title = x.getTitle();
            if (Objects.nonNull(title)) {
               METADATA.get(CLASSES.get(x.getClass().getName())).set(x, doMessage(title));;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void country(AbstractException x) {
        try {
            String title = x.getTitle();
            if (Objects.nonNull(title)) {
               METADATA.get(CLASSES.get(x.getClass().getName())).set(x,doMessage(title,x.getParam()));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
