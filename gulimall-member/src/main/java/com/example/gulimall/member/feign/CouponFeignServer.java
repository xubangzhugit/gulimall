package com.example.gulimall.member.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
@FeignClient("coupon")
public interface CouponFeignServer {

    @RequestMapping("coupon/coupon/member/list")
     R membercoupons();
}
