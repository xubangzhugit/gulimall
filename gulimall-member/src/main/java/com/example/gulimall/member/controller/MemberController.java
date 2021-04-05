package com.example.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.example.exception.PhoneExsistExcption;
import com.example.exception.UsernameExsistExecption;
import com.example.gulimall.member.feign.CouponFeignServer;
import com.example.gulimall.member.vo.RegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import com.example.gulimall.member.entity.MemberEntity;
import com.example.gulimall.member.service.MemberService;
import com.example.common.utils.PageUtils;
import com.example.common.utils.R;



/**
 * 会员
 *
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 16:03:12
 */
@RefreshScope
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;
    @Autowired
    private CouponFeignServer couponFeignServer;
    @Value("${member.user.name}")
    private String username;
    @RequestMapping("feignTest/member/coupon")
    public R getmemberFeignbycoupon(){
        MemberEntity me = new MemberEntity();
        me.setEmail("18773037748@163.com");
        R membercoupons = couponFeignServer.membercoupons();

        return membercoupons.put("member",me).put("member配置",username);
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }
    @PostMapping("/register")
    public R register(@RequestBody RegisterVo registerVo){
        try{
            memberService.register(registerVo);
        }catch(UsernameExsistExecption e){
            return R.error(e.getMessage());
        }catch(PhoneExsistExcption e){
            return R.error(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }
    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
