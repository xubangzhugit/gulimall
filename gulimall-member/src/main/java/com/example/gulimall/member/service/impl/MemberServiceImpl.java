package com.example.gulimall.member.service.impl;

import com.example.exception.PhoneExsistExcption;
import com.example.exception.UsernameExsistExecption;
import com.example.gulimall.member.dao.MemberLevelDao;
import com.example.gulimall.member.entity.MemberLevelEntity;
import com.example.gulimall.member.vo.RegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.BitSet;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.member.dao.MemberDao;
import com.example.gulimall.member.entity.MemberEntity;
import com.example.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;
    @Autowired
    MemberDao memberDao;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册实现
     * @param registerVo
     */
    @Override
    public void register(RegisterVo registerVo) {
        MemberDao baseMapper = this.baseMapper;
        MemberEntity entity = new MemberEntity();
        //查询默认会员dengji
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(memberLevelEntity.getId());  //设置默认会员dengji
        //验证数据唯一性
        checkUsernameUnique(registerVo.getUsername());
        checkMobileUnique(registerVo.getMobile());

        entity.setUsername(registerVo.getUsername());
        //密码需要加密处理
        BCryptPasswordEncoder encode  =  new BCryptPasswordEncoder();
        String encode1 = encode.encode(registerVo.getPassword());
        entity.setPassword(encode1);
        entity.setMobile(registerVo.getMobile());
        baseMapper.insert(entity);
    }

    /**
     * 检查用户名唯一
     */
    @Override
    public void checkUsernameUnique(String username) throws UsernameExsistExecption {
        Integer username1 = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if(username1>0){
            throw new UsernameExsistExecption();
        }
    }

    /**
     * 检查电话唯一
     */
    @Override
    public void checkMobileUnique(String mobile) throws  PhoneExsistExcption{
        Integer username1 = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", mobile));
        if(username1>0){
            throw new PhoneExsistExcption();
        }
    }

}