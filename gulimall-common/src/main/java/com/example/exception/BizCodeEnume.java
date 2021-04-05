package com.example.exception;


import sun.plugin2.message.Message;

/**
 * 定义错误状态码
 */
public enum  BizCodeEnume {
    VALID_EXCEPTION(400,"校验错误异常"),
    UNKOWN_EXCEPTION(444,"未知异常");
    private int code;
    private String message;
   private BizCodeEnume(int code,String message){
       this.code = code;
       this.message = message;
   }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }}
