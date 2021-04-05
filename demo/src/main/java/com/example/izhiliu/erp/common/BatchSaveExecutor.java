package com.izhiliu.erp.common;

import com.izhiliu.erp.repository.discount.DiscountItemRepository;
import com.izhiliu.erp.repository.discount.DiscountItemVariationRepository;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemDTO;
import com.izhiliu.erp.service.discount.dto.ShopeeDiscountItemVariationDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @Author: louis
 * @Date: 2020/6/24 15:05
 */
@Component
public class BatchSaveExecutor {
    @Resource
    private DiscountItemRepository discountItemRepository;
    @Resource
    private DiscountItemVariationRepository discountItemVariationRepository;


    /**
     * 保存折扣商品
     * @param list
     * @return
     */
    @Async
    public Future<Boolean> batchSaveDiscountItem(List<ShopeeDiscountItemDTO> list) {
        return new AsyncResult<>(discountItemRepository.insertBatch(list));
    }

    public List<Future<Boolean>> getDiscountItemResult(List<ShopeeDiscountItemDTO> list) {
        if (CommonUtils.isBlank(list)) {
            return new ArrayList<>();
        }
        List<Future<Boolean>> result = new ArrayList<>();
        while (list.size() > 1000) {
            List<ShopeeDiscountItemDTO> saveList = list.subList(0, 1000);
            result.add(batchSaveDiscountItem(saveList));
            list.removeAll(saveList);
        }
        if (CommonUtils.isNotBlank(list)) {
            result.add(batchSaveDiscountItem(list));
        }
        return result;
    }


    @Async
    public Future<Boolean> batchSaveDiscountItemVariation(List<ShopeeDiscountItemVariationDTO> list) {
        return new AsyncResult<>(discountItemVariationRepository.insertBatch(list));
    }

    public List<Future<Boolean>> getDiscountItemVariationResult(List<ShopeeDiscountItemVariationDTO> list) {
        if (CommonUtils.isBlank(list)) {
            return new ArrayList<>();
        }
        List<Future<Boolean>> result = new ArrayList<>();
        while (list.size() > 1000) {
            List<ShopeeDiscountItemVariationDTO> saveList = list.subList(0, 1000);
            result.add(batchSaveDiscountItemVariation(saveList));
            list.removeAll(saveList);
        }
        if (CommonUtils.isNotBlank(list)) {
            result.add(batchSaveDiscountItemVariation(list));
        }
        return result;
    }
}
