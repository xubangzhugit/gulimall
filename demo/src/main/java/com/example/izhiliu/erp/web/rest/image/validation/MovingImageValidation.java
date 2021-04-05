package com.izhiliu.erp.web.rest.image.validation;

import com.izhiliu.erp.service.image.CustomizeBaseService;
import com.izhiliu.erp.web.rest.image.validator.MovingImageValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Seriel
 * @create 2019-08-28 10:46
 **/
@Documented
@Constraint(validatedBy = MovingImageValidator.class)
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface MovingImageValidation {

    String message() default " 非法操作 ";
    Class<? extends CustomizeBaseService>  check() ;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * 定义List，为了让Bean的一个属性上可以添加多套规则
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    @interface List {
        MovingImageValidation[] value();
    }

}
