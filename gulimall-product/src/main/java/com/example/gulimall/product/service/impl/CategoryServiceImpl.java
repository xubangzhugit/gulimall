package com.example.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.CategoryDao;
import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> ListWithTree() {
        //1,查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //2，组装成树形菜单结构
        List<CategoryEntity> collect = categoryEntities.stream().filter((entity) -> {
            //查找出父菜单
            return entity.getParentCid() == 0;
        }).map((menu)->{
            //设置每个父菜单的子菜单
            menu.setChilder(getChildrens(menu,categoryEntities));
            return menu;
        }).sorted((menu1,menu2)->{
            //对父菜单排序
            return (menu1.getSort()==null?0:menu1.getSort())-(menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());


        return collect;
    }


    /**
     * 查找每个菜单的子菜单
     * @param menu
     * @param categoryEntities
     * @return
     */
    private List<CategoryEntity> getChildrens(CategoryEntity menu, List<CategoryEntity> categoryEntities) {
        List<CategoryEntity> collect = categoryEntities.stream().filter((entity) -> {
            //查找父菜单的一级子菜单
            return menu.getCatId()== entity.getParentCid();
        }).map((map1)->{
            //查找子菜单的子菜单
            map1.setChilder(getChildrens(map1,categoryEntities));
            return map1;
        }).sorted((sorted1,sorted2)->{
            //设置排序
            return (sorted1.getSort()==null?0:sorted1.getSort())-(sorted2.getSort()==null?0:sorted2.getSort());
        }).collect(Collectors.toList());
        return collect;
    }
    /**
     * 批量删除菜单
     * @param asList
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1,检查当前删除的菜单，是否被其他地方引用，未引用才可删除
        int i = baseMapper.deleteBatchIds(asList);
    }
}