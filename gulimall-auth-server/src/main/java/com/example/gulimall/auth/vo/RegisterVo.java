package com.example.gulimall.auth.vo;

import lombok.Data;
import lombok.ToString;
import org.checkerframework.checker.units.qual.Length;

import javax.validation.constraints.*;

@Data
@ToString
public class RegisterVo {
    @NotNull
    private String username;
    @NotNull
    @Min(value = 6,message = "密码长度大于6")
    @Max(value = 12,message = "密码长度小于12")
    private String password;
    @NotNull
    @Pattern(regexp = "/^1[3-9][0-9]$/")
    private String mobile;
    @NotNull(message = "验证码必须填写")
    private String code;
}
