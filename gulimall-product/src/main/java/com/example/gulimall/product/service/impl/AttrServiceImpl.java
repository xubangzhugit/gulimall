package com.example.gulimall.product.service.impl;

import com.example.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.example.gulimall.product.dao.AttrGroupDao;
import com.example.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.example.gulimall.product.entity.AttrGroupEntity;
import com.example.gulimall.product.vo.AttrGroupRelationVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.AttrDao;
import com.example.gulimall.product.entity.AttrEntity;
import com.example.gulimall.product.service.AttrService;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {
    @Autowired
    private AttrAttrgroupRelationDao relationDao;
    @Autowired
    private AttrGroupDao attrGroupDao;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    /**
     *根据分组id 查找关联的所有基本属性
     * @param attrgroupid
     * @return
     */
    //Service 层：每一个需要缓存的数据我们都要指定一个缓存的名字(缓存的分区按业务类型分)
    //@Cacheable(value = "attrs",key = "'getRelationAttr'")//代表方法的结果需要缓存，缓存中有则不需要调用该方法，缓存中没有，则查询数据，将结果放入缓存中
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupid) {
        //根据关系表分组id,查询出属性id
        List<AttrAttrgroupRelationEntity> attr_group_id = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupid));
        List<Long> collect = attr_group_id.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());
        //根据属性id集合查出属性信息
        List<AttrEntity> attrEntities = this.listByIds(collect);
        return attrEntities;
    }
//    //缓存失效模式，修改数据会将该缓存清除；
//    @CacheEvict(value = "attrs",key = "'getRelationAttr'")
//    //集中处理多个缓存
//    @Caching(evict = {
//            @CacheEvict(value = "attrs",key = "'getRelationAttr'"),
//            @CacheEvict(value = "category",key = "'getcategory'")
//    })
    @Override
    public void deleteRelation(AttrGroupRelationVo[] attrgvo) {
       List<AttrAttrgroupRelationEntity> collect = Arrays.asList(attrgvo).stream().map((item) -> {
            AttrAttrgroupRelationEntity newitem = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, newitem);
            return newitem;
        }).collect(Collectors.toList());
        relationDao.deleteBatchRelation(collect);
    }

    /**
     * 获取当前分组没有关联的所有属性
     * @param attrgroupid
     * @param params
     * @return
     */
    @Override
    public PageUtils  getNoRelationAttr(Long attrgroupid, Map<String, Object> params) {
        //当前分组只能关联自己所属分类里面的所有属性
        AttrGroupEntity attrGroupEntity =
                attrGroupDao.selectById(attrgroupid);
                //获取分类的id
        Long categoryId = attrGroupEntity.getCatelogId();
        //当前分组只能关联没有被关联的属性
            //查询当前分类其他分组
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("category_id", categoryId).ne("attr_group_id", attrgroupid));
        //查询其他分组id
        List<Long> collect = attrGroupEntities.stream().map((item) -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());
        //查询其他分组已经关联的attrid
        List<AttrAttrgroupRelationEntity> attr_group_id = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", collect));
        List<Long> collect1 = attr_group_id.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        //查询当前分类下没有关联的所有attr
        QueryWrapper<AttrEntity> attrEntityQueryWrapper = new QueryWrapper<AttrEntity>().eq("category_id", categoryId).eq("attr_type",0);
        if(!collect1.isEmpty()){
            attrEntityQueryWrapper.notIn("attr_id", collect1);
        }
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key))
            attrEntityQueryWrapper.and(item->{
                item.eq("attr_id",key).or().like("attr_name",key);
            });
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), attrEntityQueryWrapper);

        return new PageUtils(page);
    }

}