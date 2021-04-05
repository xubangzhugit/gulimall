package com.example.gulimall.product.exception;

import com.example.common.utils.R;
import com.example.exception.BizCodeEnume;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 异常处理类
 */
@Slf4j
//@ResponseBody
//@ControllerAdvice(basePackages = "com.example.gulimall.product.controller") 等同于RestControllerAdvice一个注解
@RestControllerAdvice(basePackages = "com.example.gulimall.product.controller")
public class GulimallException {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidExcetion(MethodArgumentNotValidException e ){
        log.error("数据校验出现问题{}，异常类型{}",e.getMessage(),e.getClass());
        Map<String,String> errorMap = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach((error)->{
            errorMap.put(error.getField(),error.getDefaultMessage());
        });
        return R.error(BizCodeEnume.VALID_EXCEPTION.getCode(),"数据校验异常").put("data",errorMap);
    }
}

