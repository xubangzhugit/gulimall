package com.example.exception;

public class UsernameExsistExecption extends RuntimeException{


    public UsernameExsistExecption(){
        super("用户名存在");
    }
}
