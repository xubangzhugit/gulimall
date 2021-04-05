package com.example.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.exception.PhoneExsistExcption;
import com.example.exception.UsernameExsistExecption;
import com.example.gulimall.member.entity.MemberEntity;
import com.example.gulimall.member.vo.RegisterVo;

import java.util.Map;

/**
 * 会员
 *
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 16:03:12
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(RegisterVo registerVo);

    void checkUsernameUnique(String username) throws UsernameExsistExecption;

    void checkMobileUnique(String mobile) throws PhoneExsistExcption;

}

