package com.example.gulimall.auth.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("serverName")
public interface AuthFeignaServer {

    @RequestMapping("/")
    public R getinfo();
}
