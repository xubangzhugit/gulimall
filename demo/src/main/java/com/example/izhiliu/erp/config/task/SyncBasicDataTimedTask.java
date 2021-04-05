package com.izhiliu.erp.config.task;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.Exception.ShopeeApiException;
import com.izhiliu.core.config.lock.RedisLockHelper;
import com.izhiliu.erp.domain.enums.SyncBasicDataStatus;
import com.izhiliu.erp.service.item.PlatformNodeService;
import com.izhiliu.erp.service.item.ShopeeCategoryService;
import com.izhiliu.erp.service.item.dto.PlatformNodeDTO;
import com.izhiliu.erp.service.item.dto.ShopeeCategoryDTO;
import com.izhiliu.open.shopee.open.sdk.api.publik.PublicApi;
import com.izhiliu.open.shopee.open.sdk.api.publik.result.GetCategoriesByCountryResult;
import com.izhiliu.open.shopee.open.sdk.base.ShopeeResult;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;

/**
 * describe: 同步Shopee基础数据 类目/属性/属性值
 * <p>
 *
 * @author cheng
 * @date 2019/1/29 9:45
 */
@Component
public class SyncBasicDataTimedTask {

    private static final Logger log = LoggerFactory.getLogger(SyncBasicDataTimedTask.class);

    @Resource
    private PublicApi publicApi;

    @Resource
    private SaveDxmCategoryData saveDxmCategoryData;

    @Resource
    private RedisLockHelper redisLockHelper;

    @Resource
    private PlatformNodeService platformNodeService;

    @Resource
    private ShopeeCategoryService shopeeCategoryService;

    private ExecutorService fillChinese;

    private ExecutorService syncCategory;

    private static final int CB = 1;
    private static final String EN = "en";

    public SyncBasicDataTimedTask() {
        fillChinese = Executors.newFixedThreadPool(5);
        syncCategory = Executors.newFixedThreadPool(7);
    }

    public void saveDxmCategory(String cookie, long shopId, long platformNodeId) {
        fillChinese.execute(() -> saveDxmCategoryData.refreshCategory(cookie, shopId, platformNodeId));
    }

    private static final String SYNC_NODE_CATEGORY = "$syncCategory:";


    /**
     * 同步类目
     * <p>
     * 每天 凌晨 1:00 执行
     */
    @Scheduled(cron = "0 0 1 ? * *")
    public void syncCategory() {
        syncCategory(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));
    }

    /**
     * 同步多个站点的类别
     */
    public void syncCategory(List<Long> nodeId) {
        fillChinese.execute(() -> {
            final Collection<PlatformNodeDTO> list = platformNodeService.list(nodeId);
            for (PlatformNodeDTO node : list) {
                final String lockKey = SYNC_NODE_CATEGORY + node.getId();
                final boolean lock = redisLockHelper.lock(lockKey);
                if (!lock) {
                    continue;
                }
                try {
                    syncCategory.execute(() -> syncCategory(node));
                } catch (Exception e) {
                    log.error("[同步站点类目失败] : {}", e);
                    redisLockHelper.unlock(lockKey);
                }
            }
        });
    }

    private void syncCategory(PlatformNodeDTO node) {
        log.info("[定时任务-同步站点类目]: nodeName: {}, nodeUrl: {}, date: {}", node.getName(), node.getUrl(), new Date());

        int sort = 1;

        try {
            node.setStatus(SyncBasicDataStatus.SYNC);
            platformNodeService.update(node);

            final boolean much = !node.getLanguage().equals(EN);

            final List<GetCategoriesByCountryResult.Categorie> englishOfCategories = getCategories(node.getCode(), EN).getData().getCategories();

            log.info("[处理类目列表] - {}:{}", node.getCode(), node.getLanguage());

            /*
             * 删除脏数据
             */
            deleteDirtyData(node, englishOfCategories);

            /*
             * 主存英语,主语不为英语的另存本地语言
             */
            if (much) {
                /*
                 * 目的: 与 shopee 同增同减
                 *
                 * 全部删除再新增不可取 耗时太长
                 *
                 * 策略:
                 *  把站点类目全部读出来 删除不存在的 然后进行 新增或更新操作
                 *
                 * TODO 必须保证数据不能重复
                 */
                final List<GetCategoriesByCountryResult.Categorie> categories = getCategories(node.getCode(), node.getLanguage()).getData().getCategories();
                for (int i = 0; i < englishOfCategories.size(); i++) {
                    try {
                        final GetCategoriesByCountryResult.Categorie category = englishOfCategories.get(i);
                        final String categoryName = categories.get(i).getCategoryName();

                        refreshCategory(node.getId(), category, Optional.of(categoryName), sort++);
                    } catch (Exception e) {
                        log.error("[刷新类目异常]: {}", e);
                    }
                }
            } else {
                for (GetCategoriesByCountryResult.Categorie category : englishOfCategories) {
                    try {
                        refreshCategory(node.getId(), category, Optional.empty(), sort++);
                    } catch (Exception e) {
                        log.error("[刷新类目异常]: {}", e);
                    }
                }
            }
            node.setStatus(SyncBasicDataStatus.SYNC_SUCCESS);
            node.setLastSyncTime(Instant.now());
            platformNodeService.update(node);
        } catch (Exception e) {
            node.setStatus(SyncBasicDataStatus.SYNC_FAIL);
            platformNodeService.update(node);
        } finally {
            // 释放锁
            redisLockHelper.unlock(SYNC_NODE_CATEGORY + node.getId());

            shopeeCategoryService.handleOtherToLast(node.getId());
        }
    }

    public void refreshCategory(Long platformNodeId, GetCategoriesByCountryResult.Categorie categorie, Optional<String> local, int sort) {
        /*
         * 需求: ID不变的情况下全覆盖, Shopee 更新了类目关系我这边也更新
         *
         * 1. 全填充
         * 2. 找父亲       ==>     platformNodeId, shopeeCategoryId
         * 3. 找自己       ==>     platformNodeId, shopeeCategoryId (shopeeParentId)
         * 4. 更新或添加
         */
        if (StringUtils.isBlank(categorie.getCategoryName())) {
            return;
        }

        final ShopeeCategoryDTO dto = new ShopeeCategoryDTO();
        dto.setShopeeCategoryId(categorie.getCategoryId());
        dto.setShopeeParentId(categorie.getParentId());
        dto.setPlatformNodeId(platformNodeId);
        dto.setHasChild(categorie.isHasChildren() ? 1 : 0);
        dto.setName(categorie.getCategoryName());
        dto.setSort(sort);
        dto.setSuppSizeChart(categorie.isSuppSizeChart()?1:0);

        local.ifPresent(dto::setLocal);

        if (categorie.getParentId() != 0) {
            final Optional<ShopeeCategoryDTO> parentExist = shopeeCategoryService.findByPlatformNodeAndShopeeCategory(platformNodeId, categorie.getParentId());
            if (parentExist.isPresent()) {
                final ShopeeCategoryDTO parent = parentExist.get();
                dto.setParentId(parent.getId());

                dto.setTier(parent.getTier() == null ? null : parent.getTier() + 1);
            } else {
                /*
                 * TODO 父节点未插入导致找不到父节点的情况: 标识为-1, 等待第二次同步
                 */
                dto.setParentId(-1L);
                dto.setTier(-1);
            }
        } else {
            dto.setTier(1);
            dto.setParentId(0L);
        }

        /*
         * 存在则更新
         */
        dto.setShopeeParentId(categorie.getParentId());
        shopeeCategoryService.findByPlatformNodeAndShopeeCategory(platformNodeId, categorie.getCategoryId()).ifPresent(e -> dto.setId(e.getId()));
        try {
            shopeeCategoryService.saveOrUpdate(dto);
        } catch (Exception e) {
            log.error("[保存或更新类目异常]: {}", e);
        }
    }

    private ShopeeResult<GetCategoriesByCountryResult> getCategories(String code, String language) {
        final ShopeeResult<GetCategoriesByCountryResult> localLanguageOfCategories = publicApi.getCategoriesByCountry(code, CB, language);
        if (!localLanguageOfCategories.isResult()) {
            log.error("[获取类目失败] - {}:{}, {}", code, language, localLanguageOfCategories.getError().getMsg());
            throw new ShopeeApiException(localLanguageOfCategories.getError().getMsg(), "");
        }
        return localLanguageOfCategories;
    }

    private void deleteDirtyData(PlatformNodeDTO node, List<GetCategoriesByCountryResult.Categorie> englishOfCategories) {
        /*
         * Shopee 没有 本地有 删除本地的脏数据
         */
        final List<Long> ids = englishOfCategories.stream().map(GetCategoriesByCountryResult.Categorie::getCategoryId).collect(toList());
        final List<Long> dirtyData = shopeeCategoryService.pageByPlatformNode(node.getId(), new Page(0, Integer.MAX_VALUE)).getRecords().stream()
            .filter(e -> !ids.contains(e.getShopeeCategoryId()))
            .map(ShopeeCategoryDTO::getId)
            .collect(toList());

        if (dirtyData.size() > 0) {
            log.info("[删除脏类目] : {}", dirtyData.size());
            shopeeCategoryService.delete(dirtyData);
        }
    }

    // ----------------------------------------------------------------------------------------------------------

    @Component
    class SaveDxmCategoryData {
        private static final String API = "https://www.dianxiaomi.com/shopeeCategory/list.json?%s";

        @Resource
        private ShopeeCategoryService shopeeCategoryService;

        private ExecutorService executor;

        public SaveDxmCategoryData() {
            this.executor = Executors.newFixedThreadPool(5);
        }

        /**
         * 刷新一级类目
         *
         * @param shopId
         */
        void refreshCategory(String cookie, long shopId, long platformNodeId) {
            final String body = request(getHeaders(cookie), "shopId=" + shopId);
            try {
                final IPage<ShopeeCategoryDTO> shopeeCategoryDTOIPage = shopeeCategoryService.pageByTierAndPlatformNode(1, platformNodeId, new Page(0, Integer.MAX_VALUE));
                final List<ShopeeCategoryDTO> categorys = shopeeCategoryDTOIPage.getRecords();
                log.info("category.size(): {}", categorys.size());
                refreshCategory(cookie, shopId, platformNodeId, JSON.parseArray(body, DxmCategoryResult.class), categorys);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<DxmCategoryResult> getDxmCategoryResult(String cookie, long shopId, long parentId) {
            final String body = request(getHeaders(cookie), "shopId=" + shopId + "&categoryParentId=" + parentId);
            final List<DxmCategoryResult> result;
            try {
                result = JSON.parseArray(body, DxmCategoryResult.class);
                if ("[]".equals(body)) {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
            return result;
        }

        /**
         * 1. 刷新第一级别的
         * 2. 根据父级拿到子集
         * 3. 递归刷新
         */
        private void refreshCategory(String cookie, long shopId, long platformNodeId, List<DxmCategoryResult> refreshCategorys, List<ShopeeCategoryDTO> categorys) {
            try {
                for (DxmCategoryResult refreshCategory : refreshCategorys) {
                    for (ShopeeCategoryDTO category : categorys) {
                        // 匹配上了 填充中文
                        if (refreshCategory.getCategoryId() == category.getShopeeCategoryId()) {
                            category.setChinese(refreshCategory.getChineseName());
                            shopeeCategoryService.update(category);
                            log.info("[refresh-success] : shopeeCategoryId: {}", category.getShopeeCategoryId());

                            final List<DxmCategoryResult> dxmCategoryResult = getDxmCategoryResult(cookie, shopId, category.getShopeeCategoryId());
                            if (dxmCategoryResult == null) {
                                continue;
                            }
                            final List<ShopeeCategoryDTO> childs = shopeeCategoryService.pageByChild(null, category.getId(), platformNodeId, new Page(0, Integer.MAX_VALUE)).getRecords();
                            executor.execute(() -> refreshCategory(cookie, shopId, platformNodeId, dxmCategoryResult, childs));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[刷新类目异常]", e);
            }
        }

        private Map<String, String> getHeaders(String cookie) {
            final Map<String, String> headers = new HashMap<>(5);
            headers.put("cookie", cookie);
            return headers;
        }

        private String request(Map<String, String> headers, String param) {
            final String api = String.format(API, param);
            final String body = HttpRequest.get(api)
                .addHeaders(headers)
                .execute()
                .body();

            log.info("[http-request] : {}", api);
            log.info("[response]: {}", body);
            return body;
        }
    }

    @Data
    static class DxmCategoryResult {
        private int categoryId;
        private String categoryName;
        private int categoryParentId;
        private String chineseName;
        private long createTime;
        private int id;
        private int isDel;
        private int leafCategory;
        private String site;
        private long updateTime;
    }
}
