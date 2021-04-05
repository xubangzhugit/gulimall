package com.example.gulimall.member.vo;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

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
}
