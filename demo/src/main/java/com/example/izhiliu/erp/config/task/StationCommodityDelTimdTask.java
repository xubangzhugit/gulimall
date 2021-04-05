package com.izhiliu.erp.config.task;

import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.repository.item.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务，定时清理超过三个月未更新的站点商品
 */
@Component
public class StationCommodityDelTimdTask {

    private static final Logger log = LoggerFactory.getLogger(StationCommodityDelTimdTask.class);

    @Resource
    protected ShopeeProductRepository shopeeProductRepository;

    @Resource
    protected ShopeeProductDescRepository shopeeProductDescRepository;

    @Resource
    protected ShopeeProductSkuRepository shopeeProductSkuRepository;

    @Resource
    protected ShopeeSkuAttributeRepository shopeeSkuAttributeRepository;

    @Resource
    protected ShopeeProductAttributeValueRepository shopeeProductAttributeValueRepository;

    @Resource
    protected com.izhiliu.erp.repository.item.ShopeeProductMediaRepository ShopeeProductMediaRepository;

    @Resource
    private BoostItemRepository boostItemRepository;

    @Resource
    private RedissonClient redissonClient;


    /**
     * 定时删除超过三月未更新得站点商品
     * 执行时间：每日凌晨2:30分开始
     */
    @Scheduled(cron = "0 30 2 ? * *")
    public void removeCommodity(){
        RLock rLock = redissonClient.getLock("removeCommodity");
        if (rLock.tryLock()) {
            log.info("定时任务执行删除三个月前的站点商品");
            try {
                batcheRemove();
            } catch (Exception e) {
                log.error("定时任务执行删除三个月前的站点商品异常", e);
            }finally {
                if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                }
            }
        }
    }


    public void batcheRemove(){
        while (true) {
            List<Long> productIds = shopeeProductRepository.selectNoUpdateProduct();
            if (CommonUtils.isBlank(productIds)) {
                break;
            }
            shopeeProductRepository.deleteByIds(productIds);
            shopeeProductSkuRepository.deleteByProductIds(productIds);
            shopeeProductAttributeValueRepository.deleteByProductIds(productIds);
            ShopeeProductMediaRepository.deleteByProductIds(productIds);
            shopeeProductDescRepository.deleteByProductIds(productIds);
            boostItemRepository.deleteBatchProductIds(productIds);
            //5点结束
            if (LocalDateTime.now().getHour() == 5) {
                break;
            }
        }
    }

}
