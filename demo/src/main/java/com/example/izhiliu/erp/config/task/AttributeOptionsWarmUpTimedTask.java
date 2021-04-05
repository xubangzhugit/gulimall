package com.izhiliu.erp.config.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.service.item.ShopeeBasicDataService;
import com.izhiliu.erp.service.item.ShopeeCategoryService;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * describe: 类目属性值预热, 暂不启用
 * <p>
 *
 * @author cheng
 * @date 2019/3/18 19:50
 */
//@Component
public class AttributeOptionsWarmUpTimedTask {

    @Resource
    private ShopeeCategoryService shopeeCategoryService;

    @Resource
    private ShopeeBasicDataService shopeeBasicDataService;

    private static final int BATCH_GET = 100;

    private Executor executor;

    public AttributeOptionsWarmUpTimedTask() {
        this.executor = Executors.newFixedThreadPool(20);
    }

    @Scheduled(cron = "0 0 4 ? * 1")
    private void warmUp() {
        for (long nodeId = 1; nodeId <= 9; nodeId++) {
            handle(nodeId);
        }
    }

    private void handle(Long nodeId) {
        while (true) {
            final List<ShopeeCategoryDTO> list = shopeeCategoryService.pageByPlatformNode(nodeId, new Page(0, BATCH_GET)).getRecords();
            if (list.size() == 0) {
                return;
            }

            for (ShopeeCategoryDTO category : list) {
                executor.execute(() -> shopeeBasicDataService.getAllAttributeByCategoryIdReCache(category.getShopeeCategoryId(), nodeId));
            }
        }
    }
}
