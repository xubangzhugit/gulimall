package com.izhiliu.erp.web.rest.image.validator;

import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.service.image.CustomizeBaseService;
import com.izhiliu.erp.web.rest.image.validation.MovingImageValidation;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.*;

/**
 * @author Seriel
 * @create 2019-08-28 10:47
 **/
@Component
public class MovingImageValidator  implements ConstraintValidator<MovingImageValidation,Object> ,ApplicationContextAware{

    private  static Map<Class<? extends CustomizeBaseService>,Object> classObjectMap = Collections.synchronizedMap(new HashMap<>());

    private  static ApplicationContext applicationContext;

    private MovingImageValidation constraintAnnotation;

    @Override
    public void initialize(MovingImageValidation constraintAnnotation) {
      this.constraintAnnotation=constraintAnnotation;
    }

    @Override
    public boolean isValid(Object aLong, ConstraintValidatorContext constraintValidatorContext) {
        final String message = constraintAnnotation.message();
        final Class<? extends CustomizeBaseService> check = constraintAnnotation.check();
        Assert.notNull(check,"必须要 增加对应类");
         Object bean = classObjectMap.get(check);
        if(Objects.isNull(bean)){
            bean  = applicationContext.getBean(check);
            classObjectMap.put(check,bean);
        }
        Assert.notNull(bean," spring  里面没有注入");
        if(aLong instanceof Long){
            final Object optional = ((CustomizeBaseService) bean).checke((Long) aLong);
            if (Objects.nonNull(optional)) {
                return true;
            }
        }else if(aLong instanceof List){
            final List<Long> aLong1 = (List<Long>) aLong;
            final Collection list = ((CustomizeBaseService) bean).checke(aLong1);
            if(list.size()== aLong1.size()){
                return true;
            }
        }
        return false;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
