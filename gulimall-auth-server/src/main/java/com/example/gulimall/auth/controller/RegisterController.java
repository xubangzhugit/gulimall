package com.example.gulimall.auth.controller;

import com.example.common.utils.R;
import com.example.gulimall.auth.vo.RegisterVo;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@RestController
public class RegisterController {

    @PostMapping("/register")
    public R register(@Valid @RequestBody RegisterVo reVo, BindingResult e, RedirectAttributes red){
        //取出bindingResult的异常结果。返回给前端。
        //先校验验证码，验证码不匹配，不用调用远程服务，验证码匹配。redis中删除验证码
        //远程调用member注册服务。

        return R.ok();
    }
}

