package com.example.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.example.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.example.gulimall.product.entity.AttrEntity;
import com.example.gulimall.product.service.AttrAttrgroupRelationService;
import com.example.gulimall.product.service.AttrService;
import com.example.gulimall.product.vo.AttrGroupRelationVo;
import com.example.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.gulimall.product.entity.AttrGroupEntity;
import com.example.gulimall.product.service.AttrGroupService;
import com.example.common.utils.PageUtils;
import com.example.common.utils.R;



/**
 * 属性分组
 *
 * @author xubangzhu
 * @email 18773037748@gmail.com
 * @date 2020-08-29 14:08:29
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    ///product/attrgroup/{attrgroupid}/attr/relation
    @GetMapping("/{attrgroupid}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupid") Long attrgroupid) {
        List<AttrEntity> attrEntities = attrService.getRelationAttr(attrgroupid);
        return R.ok().put("data", attrEntities);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = attrGroupService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }
    //删除关联关系
    //product/attrgroup/attr/relation/delete
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] attrgvo) {
        attrService.deleteRelation(attrgvo);
        return R.ok();
    }
    //根据分组id 查询可以关联的属性
    //product/attrgroup/{attrgroupid}/attr/relation
    @RequestMapping("/{attrgroupid}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupid") Long attrgroupid,
                          @RequestParam Map<String, Object> params) {
        PageUtils page = attrService.getNoRelationAttr(attrgroupid, params);
        return R.ok().put("data", page);
    }
    //添加关联关系
    //product/attrgroup/attr/relation
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody AttrGroupRelationVo[] attrgvo) {
        attrAttrgroupRelationService.saveBatch(Arrays.asList(attrgvo));
        return R.ok();
    }
    //获取某个分类下所有分组和分组下的所有属性
    //product/category/{categoryId}/withattr
    @RequestMapping("/{categoryId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable("categoryId") Long categoryId){

        //查询当前分类下的所有分组
        //查询每个分组下的所欲属性
        List<AttrGroupWithAttrsVo> vos = this.attrGroupService.getAttrGroupWithAttrsByCategoryId(categoryId);
        return R.ok().put("data",vos);
    }
}
