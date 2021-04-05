package com.example.exception;

public class PhoneExsistExcption extends  RuntimeException{

    public PhoneExsistExcption(){
        super("电话已经存在");
    }
}
