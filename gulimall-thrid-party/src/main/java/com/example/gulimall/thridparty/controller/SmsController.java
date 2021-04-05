package com.example.gulimall.thridparty.controller;

import com.example.common.utils.R;
import com.example.gulimall.thridparty.component.SmsSend;
import org.aspectj.apache.bcel.classfile.Code;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("/third/party")
public class SmsController {
     @Autowired
     SmsSend smsSend;
     @GetMapping("/sms/send")
     public R smsSend(@RequestParam("mobile") String mobile, @RequestParam("Code")String Code){
         smsSend.smsSend(mobile,Code);
         return R.ok();
     };

}
