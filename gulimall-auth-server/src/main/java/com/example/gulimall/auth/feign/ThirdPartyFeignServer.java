package com.example.gulimall.auth.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-thrid-party")
public interface ThirdPartyFeignServer {
    @GetMapping("/thrid/party/sms/send")
    public R smsSend(String mobile, String Code);
}
