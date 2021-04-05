package com.izhiliu.erp.service.item.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.domain.common.IBaseServiceImpl;
import com.izhiliu.erp.domain.item.ShopeeCategory;
import com.izhiliu.erp.repository.item.ShopeeCategoryRepository;
import com.izhiliu.erp.service.item.ShopeeCategoryService;
import com.izhiliu.erp.service.item.ShopeeProductService;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;
import com.izhiliu.erp.service.item.dto.ShopeeProductDTO;
import com.izhiliu.erp.service.item.mapper.ShopeeCategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Service Implementation for managing ShopeeCategory.
 */
@Service
public class ShopeeCategoryServiceImpl extends IBaseServiceImpl<ShopeeCategory, ShopeeCategoryDTO, ShopeeCategoryRepository, ShopeeCategoryMapper> implements ShopeeCategoryService {

    private final Logger log = LoggerFactory.getLogger(ShopeeCategoryServiceImpl.class);

    @Resource
    private ShopeeProductService shopeeProductService;

    @Override
    public ShopeeCategoryDTO save(ShopeeCategoryDTO dto) {
        // 查找是否存在逻辑删除 如果存在则恢复并更新 不存在则正常插入
        final Optional<ShopeeCategoryDTO> exist = findDeletedByPlatformNodeAndShopeeCategory(dto.getPlatformNodeId(), dto.getShopeeCategoryId());
        if (exist.isPresent()) {
            try {
                resume(exist.get().getId());
                dto.setId(exist.get().getId());
                dto.setDeleted(0);
                update(dto);
            }catch (Exception e){
                log.error(" save exist delete category error ",e);
            }
            return exist.get();
        } else {
            return super.save(dto);
        }
    }

    @Override
    public boolean delete(Long id) {
        /*
         * 警告提醒绑定该类目的商品
         *
         * 删除该类目下属所有属性
         */
//        shopeeProductService.updateByCategoryId(id, new ShopeeProductDTO().setCategoryId(id).setWarning("The category_id is not exist.Please update category_id"));

        // 如果已存在逻辑删除的数据 则物理删除掉
        find(id).ifPresent(dto -> {
            final Optional<ShopeeCategoryDTO> exist = findDeletedByPlatformNodeAndShopeeCategory(dto.getPlatformNodeId(), dto.getShopeeCategoryId());
            if (exist.isPresent()) {
                exist.ifPresent(e -> deleted(id));
            } else {
                super.delete(id);
            }
        });
        return true;
    }

    @Override
    public boolean delete(Collection<Long> idList) {
        for (Long id : idList) {
            delete(id);
        }
        return true;
    }

    @Override
    public IPage<ShopeeCategoryDTO> pageByPlatformNode(long platformNodeId, Page page) {
        return toDTO(repository.pageByPlatformNodeId(page, platformNodeId));
    }


    @Override
    public List<ShopeeCategoryDTO> listByForebears(long id) {
        final List<ShopeeCategory> list = new ArrayList<>(5);

        ShopeeCategory shopeeCategory = repository.selectById(id);
        if (shopeeCategory == null) {
            return new ArrayList<>(1);
        }
        while (true) {
            list.add(shopeeCategory);
            if (null == shopeeCategory || null == shopeeCategory.getParentId() || shopeeCategory.getParentId() <= 0) {
                break;
            } else {
                shopeeCategory = repository.selectById(shopeeCategory.getParentId());
            }
        }
        Collections.reverse(list);
        return mapper.toDto(list);
    }


    @Override
    public IPage<ShopeeCategoryDTO> pageByTierAndPlatformNode(int tier, long platformNodeId, Page page) {
        return toDTO(repository.pageByTierAndPlatformNode(page, tier, platformNodeId));
    }

    @Override
    public IPage<ShopeeCategoryDTO> pageByTierAndPlatformNodeAndChineseIsNull(int tier, long platformNodeId, Page page) {
        return toDTO(repository.pageByTierAndPlatformNodeAndChineseIsNull(page, tier, platformNodeId));
    }

    @Override
    public IPage<ShopeeCategoryDTO> pageByChild(String keyword, long id, long platformNodeId, Page page) {
        return toDTO(repository.pageByParentId(page, keyword, id, platformNodeId));
    }


    @Override
    public Optional<ShopeeCategoryDTO> findByPlatformNodeAndParentIdAndShopeeCategoryId(long platformNodeId, long parentId, long shopeeCategoryId) {
        return Optional.ofNullable(repository.findByPlatformNodeAndParentIdAndShopeeCategoryId(platformNodeId, parentId, shopeeCategoryId));
    }


    @Override
    public Optional<ShopeeCategoryDTO> findByPlatformNodeAndShopeeCategory(long platformNodeId, long shopeeCategoryId) {
        return Optional.ofNullable(repository.findByPlatformNodeIdAndShopeeCategoryId(platformNodeId, shopeeCategoryId));
    }

    @Override
    public int handleOtherToLast(long platformNodeId) {
        return repository.handleOtherToLast(platformNodeId);
    }

    @Override
    public Optional<ShopeeCategoryDTO> findDeletedByPlatformNodeAndShopeeCategory(long platformNodeId, long shopeeCategoryId) {
        return Optional.ofNullable(mapper.toDto(repository.findDeletedByPlatformNodeAndShopeeCategoryId(platformNodeId, shopeeCategoryId)));
    }

    @Override
    public boolean resume(long id) {
        return repository.resume(id) > 0;
    }

    @Override
    public void deleted(long id) {
        repository.deleted(id);
    }

    @Override
    public boolean invalidCategory(Long platformNodeId, long shopeeCategoryId) {
        return repository.invalidCategory(platformNodeId,shopeeCategoryId) > 0;
    }
}
