//package com.izhiliu.erp.service.item.impl;
//
//import com.baomidou.mybatisplus.core.metadata.IPage;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.izhiliu.erp.domain.item.ShopeeAttributeValueOption;
//import com.izhiliu.erp.domain.item.bean.RepeatConditionAndMinId;
//import com.izhiliu.erp.repository.item.ShopeeAttributeValueOptionRepository;
//import com.izhiliu.erp.service.common.IBaseServiceImpl;
//import com.izhiliu.erp.service.item.ShopeeAttributeValueOptionService;
//import com.izhiliu.erp.service.item.ShopeeProductAttributeValueService;
//import com.izhiliu.erp.service.item.dto.ShopeeAttributeValueOptionDTO;
//import com.izhiliu.erp.service.item.mapper.ShopeeAttributeValueOptionMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.annotation.Resource;
//import java.util.Collection;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//
///**
// * Service Implementation for managing ShopeeAttributeValueOption.
// */
//@Service
//public class ShopeeAttributeValueOptionServiceImpl extends IBaseServiceImpl<ShopeeAttributeValueOption, ShopeeAttributeValueOptionDTO, ShopeeAttributeValueOptionRepository, ShopeeAttributeValueOptionMapper> implements ShopeeAttributeValueOptionService {
//
//    private final Logger log = LoggerFactory.getLogger(ShopeeAttributeValueOptionServiceImpl.class);
//
//    @Resource
//    private ShopeeProductAttributeValueService shopeeProductAttributeValueService;
//
//    @Resource
//    private StringRedisTemplate stringRedisTemplate;
//
//    private ExecutorService executor = Executors.newFixedThreadPool(20);
//
//
//    @Override
//    public ShopeeAttributeValueOptionDTO save(ShopeeAttributeValueOptionDTO dto) {
//        return super.save(dto);
//    }
//
//
//    @Override
//    public boolean batchSave(Collection<ShopeeAttributeValueOptionDTO> entityList, int batchSize) {
//        return super.batchSave(entityList, batchSize);
//    }
//
//
//    @Override
//    public boolean saveOrUpdate(ShopeeAttributeValueOptionDTO dto) {
//        return super.saveOrUpdate(dto);
//    }
//
//
//    @Override
//    public boolean batchSaveOrUpdate(Collection<ShopeeAttributeValueOptionDTO> entityList, int batchSize) {
//        return super.batchSaveOrUpdate(entityList, batchSize);
//    }
//
//
//    @Override
//    public boolean delete(Long id) {
//        /*
//         * 删除引用了这个属性值的地方
//         */
//        find(id).ifPresent(e -> shopeeProductAttributeValueService.deleteByAttributeAndValue(e.getAttributeId(), e.getValue()));
//        return super.delete(id);
//    }
//
//
//    @Override
//    public boolean delete(Collection<Long> idList) {
//        for (Long id : idList) {
//            delete(id);
//        }
//        return true;
//    }
//
//
//    @Override
//    public boolean update(ShopeeAttributeValueOptionDTO dto) {
//        return super.update(dto);
//    }
//
//
//    @Override
//    public boolean batchUpdate(Collection<ShopeeAttributeValueOptionDTO> entityList, int batchSize) {
//        return super.batchUpdate(entityList, batchSize);
//    }
//
//
//    @Override
//    public Optional<ShopeeAttributeValueOptionDTO> find(Long id) {
//        return super.find(id);
//    }
//
//
//    @Override
//    public Collection<ShopeeAttributeValueOptionDTO> list(Collection<Long> idList) {
//        return super.list(idList);
//    }
//
//
//    @Override
//    public IPage<ShopeeAttributeValueOptionDTO> page(IPage<ShopeeAttributeValueOption> page) {
//        return super.page(page);
//    }
//
//
//    @Override
//    public IPage<ShopeeAttributeValueOption> page$(IPage<ShopeeAttributeValueOption> page) {
//        return super.page$(page);
//    }
//
//
//    @Override
//    public IPage<ShopeeAttributeValueOptionDTO> pageByAttribute(Long attributeId, Page page) {
//        return toDTO(repository.pageByAttributeId(page, attributeId));
//    }
//
//
//
//    @Override
//    public void deleteByAttribute(Long attributeId) {
//        repository.deleteByAttribute(attributeId);
//    }
//
//    @Transactional(propagation = Propagation.NOT_SUPPORTED)
//    @Override
//    public void clearRepeat() {
//        int max = 22000;
//        int size = 50;
//
//        int start = 11750;
//        int end = start + size;
//
//        while (start < max) {
//            final int s = start;
//            final int e = end;
//            executor.execute(() -> exec(s, e));
//            start = end;
//            end += size;
//        }
//    }
//
//    private static final String CLEAR_REPEAT_DATA_A = "CLEAR_REPEAT_DATA$Interval";
//    private static final String CLEAR_REPEAT_DATA_B = "CLEAR_REPEAT_DATA$Attribute";
//    private static final String CLEAR_REPEAT_DATA_C = "CLEAR_REPEAT_DATA$Options";
//
//    private void exec(int s, int e) {
//        log.info("[清除重复数据] : {}:{}", s, e);
//        final List<RepeatConditionAndMinId> minId = repository.findRepeatConditionAndMinId(s, e);
//        if (minId.size() > 0) {
//            // 确定哪个区间有问题
//            stringRedisTemplate.opsForList().rightPush(CLEAR_REPEAT_DATA_A, s + "$" + e);
//            for (RepeatConditionAndMinId entity : minId) {
//
//                final List<ShopeeAttributeValueOption> options = repository.listByAttributeIdAndValue(entity.getAttributeId(), entity.getValue());
//
//                if (options.size() > 1) {
//                    // 确定哪个属性值有问题
//                    stringRedisTemplate.opsForList().rightPush(CLEAR_REPEAT_DATA_B, entity.getAttributeId() + "$" + entity.getValue());
//                    options.remove(0);
//
//                    final List<Long> ids = options.stream().map(ShopeeAttributeValueOption::getId).collect(Collectors.toList());
//                    for (Long id : ids) {
//                        // 要干掉哪些属性值
//                        stringRedisTemplate.opsForList().rightPush(CLEAR_REPEAT_DATA_C, id.toString());
//                    }
//                    // 哪些属性值是完成了的
//                    stringRedisTemplate.opsForList().set(CLEAR_REPEAT_DATA_B, 0, entity.getAttributeId() + "$" + entity.getValue() + "$OVER");
//                }
//            }
//        }
//    }
//
//    @Transactional(propagation = Propagation.NOT_SUPPORTED)
//    @Override
//    public void deleteRepeat() {
//        while (true) {
//            final String s = stringRedisTemplate.opsForList().rightPop(CLEAR_REPEAT_DATA_C);
//            if (s == null) {
//                if (stringRedisTemplate.opsForValue().get("EXEC") != null) {
//                    log.info("[清除重复数据]: 休息10秒");
//                    try {
//                        Thread.sleep(10000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    break;
//                }
//            } else {
//                try {
//                    log.info("[clear]: {}", s);
//                    repository.clear(Long.parseLong(s));
//                } catch (NumberFormatException e) {
//                    log.error("[清除重复数据]: 删除 {}", e);
//                }
//            }
//        }
//        log.info("[清除重复数据]: 退出执行 ");
//    }
//}
