package com.example.gulimall.auth.controller;

import com.example.common.utils.R;
import com.example.gulimall.auth.feign.ThirdPartyFeignServer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class LoginController {

    @Autowired
    ThirdPartyFeignServer thirdPartyFeignServer;

    @Autowired
    StringRedisTemplate StringRedisTemplate;

    /**
     * 发送验证码
     * @param mobile
     * @return
     */
    @GetMapping("/auth/send")
    public R smsSend(String mobile) {
        //TODO 接口防刷
        String s = StringRedisTemplate.opsForValue().get("sms:send:" + mobile);
        if(!StringUtils.isEmpty(s)){
            Long s1 = Long.parseLong(s.split("_")[1]);
             if(System.currentTimeMillis()-s1<60000){
                 //还在倒计时，静止发送验证码
                 return R.error("重复发送验证码");
             }
        }
        //验证码存入redis()
        String encode = UUID.randomUUID().toString().substring(0, 5);
        //存入redis,加上当前系统时间，下次发送判断是否大于过期时间，
        StringRedisTemplate.opsForValue().set("sms:send:"+mobile,encode+"_"+System.currentTimeMillis(),60000, TimeUnit.SECONDS);
        //发送验证码
        thirdPartyFeignServer.smsSend(mobile, encode);
        return R.ok();
    }
    /*public static void main(String[] args){
        BCryptPasswordEncoder BCrypt = new BCryptPasswordEncoder();
        String encode = BCrypt.encode("123456"); //加密
        boolean bool =  BCrypt.matches("123456","$2a$10$CKq.5.tl.di3hvQ3QezYtO5MfCEzP.3FaaVmj1UI0Q4G4KQU/bE72");
        System.out.println(bool);
        //$2a$10$CKq.5.tl.di3hvQ3QezYtO5MfCEzP.3FaaVmj1UI0Q4G4KQU/bE72
        //$2a$10$kxdUcMQkA/H7bELPjcdFo.JExGkNgwuYfA8s43NT5JghmPHDj9Tzm
    }*/
}
