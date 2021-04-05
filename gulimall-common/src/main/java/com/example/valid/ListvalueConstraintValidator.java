package com.example.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 自定义注解校验器
 */
public class ListvalueConstraintValidator implements ConstraintValidator<ListValue,Integer> {
    Set<Integer> set = new HashSet();
    //初始化注解
    @Override
    public void initialize(ListValue constraintAnnotation) {
        Arrays.stream(constraintAnnotation.value()).forEach((e)->{
            set.add(e);
        });
    }
    //判断是否校验成功
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return set.contains(value);
    }
}
