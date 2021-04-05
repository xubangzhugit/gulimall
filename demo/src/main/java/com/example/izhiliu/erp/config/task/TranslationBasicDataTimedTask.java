package com.izhiliu.erp.config.task;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.erp.service.item.ShopeeCategoryService;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;
import com.swjtu.lang.LANG;
import com.swjtu.querier.Querier;
import com.swjtu.trans.AbstractTranslator;
import com.swjtu.trans.impl.GoogleTranslator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/3/18 11:18
 */
@Component
public class TranslationBasicDataTimedTask {

    private Logger log = LoggerFactory.getLogger(TranslationBasicDataTimedTask.class);

    private static final int SLEEP_TIME = 1500;

    private static final int MAX_BATCH_COUNT = 50;

    private static final String LOCK_KEY = "$translationCategory:";

    @Resource
    private RedisLockHelper redisLockHelper;

    @Resource
    private ShopeeCategoryService shopeeCategoryService;

    private Executor executor = Executors.newFixedThreadPool(5);

    /**
     * 翻译类目
     * 打包4 阿弥陀佛
     * <p>
     * 每周 2,5 凌晨 1:40 执行
     */
    @Scheduled(cron = "0 40 1 ? * 2,5")
    public void translationCategory() {
        executor.execute(() -> {
            try {
                log.info("[定时任务-翻译类目]");
                for (int nodeId = 1; nodeId <= 9; nodeId++) {
                    final String lockKey = LOCK_KEY + nodeId;
                    final boolean lock = redisLockHelper.lock(lockKey);
                    if (!lock) {
                        log.info("[其他机器正在执行-跳过任务]");
                        continue;
                    }
                    try {
                        translationCategory(nodeId);
                    } catch (Exception e) {
                        log.error("[翻译类目异常]: {}", e);
                        redisLockHelper.unlock(lockKey);
                    }
                }
            } catch (Exception e) {
                log.error("[翻译类目异常]: {}", e);
            }
        });
    }

    /**
     * 翻译类目
     *
     * @param nodeId 站点ID
     */
    public void translationCategory(int nodeId) {
        executor.execute(() -> {
            try {
                for (int tier = 1; tier <= 4; tier++) {
                    log.info("[翻译站点类目] node:{}, tier:{}", nodeId, tier);

                    final List<ShopeeCategoryDTO> categorys = shopeeCategoryService.pageByTierAndPlatformNodeAndChineseIsNull(tier, nodeId, new Page(0, Integer.MAX_VALUE)).getRecords();

                    /*
                     * 数量大于 最大批量数 分片存取
                     * 否则一次存取
                     */
                    final int blockCount = getBlockCount(categorys);
                    if (blockCount == 0) {
                        translationCategory(categorys);
                    } else {
                        for (List<ShopeeCategoryDTO> list : Lists.partition(new ArrayList<>(categorys), MAX_BATCH_COUNT)) {
                            translationCategory(list);
                        }
                    }
                }
            } finally {
                redisLockHelper.unlock(LOCK_KEY + nodeId);
            }
        });
    }

    private static final String SEPARATOR = ",";

    private void translationCategory(List<ShopeeCategoryDTO> categorys) {
        if (categorys == null || categorys.size() == 0) {
            return;
        }

        final List<String> englishNames = categorys.stream().map(this::filterName).collect(toList());

        final String text = String.join(SEPARATOR, englishNames);
        final List<String> chineseNames;
        try {
            final String translation = translation(text);
            if (StringUtils.isBlank(translation)) {
                return;
            }
            chineseNames = Arrays.asList(translation.split(SEPARATOR));
        } catch (Exception e) {
            log.error("[类目翻译异常]: {}", e);
            return;
        }

        if (chineseNames.size() == categorys.size()) {
            int i = 0;

            /*
             * 保存结果
             */
            final List<ShopeeCategoryDTO> update = new ArrayList<>(chineseNames.size());
            for (ShopeeCategoryDTO category : categorys) {
                final ShopeeCategoryDTO categoryDTO = new ShopeeCategoryDTO();
                categoryDTO.setId(category.getId());
                categoryDTO.setChinese(chineseNames.get(i++));
                update.add(categoryDTO);
            }
            shopeeCategoryService.batchUpdate(update);
        } else {
            /*
             * 翻译出现了意外情况导致无法映射
             *  1. 将出问题的类目分成两半继续执行任务
             *  2. 知道不可分割为止
             *
             * 能翻译99%
             */
            if (categorys.size() > 2) {
                for (List<ShopeeCategoryDTO> list : Lists.partition(categorys, categorys.size() / 2)) {
                    translationCategory(list);
                }
            }
        }
        sleep(SLEEP_TIME);
    }

    private String filterName(ShopeeCategoryDTO e) {
        return e.getName()
            .replaceAll("\\.", "")
            .replaceAll(",", "")
            .replaceAll("，", "")
            .replaceAll("&", " and ");
    }

    /**
     * 翻译接口
     *
     * @param text
     * @return
     */
    private String translation(String text) {

        Querier<AbstractTranslator> querierTrans = new Querier<>();
        querierTrans.setParams(LANG.EN, LANG.ZH, text);
        querierTrans.attach(new GoogleTranslator());

        List<String> result = querierTrans.execute();
        log.info("[翻译前]: {}", text);
        log.info("[翻译后]: {}", result.get(0));
        return result.get(0);
    }

    private void sleep(int time) {
        try {
            log.info("[translation-sleep]");
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 得到区块数量
     *
     * @param entityList 需要分区的List
     * @return 区块数量 0 为不需要分区
     */
    private static int getBlockCount(Collection entityList) {
        return entityList.size() / MAX_BATCH_COUNT == 0 ? 0 : entityList.size() % MAX_BATCH_COUNT == 0 ? entityList.size() / MAX_BATCH_COUNT : entityList.size() / MAX_BATCH_COUNT + 1;
    }
}
