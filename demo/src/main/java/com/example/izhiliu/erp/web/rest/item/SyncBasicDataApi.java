package com.izhiliu.erp.web.rest.item;

import com.izhiliu.core.util.ShopeeUtil;
import com.izhiliu.erp.config.task.SyncBasicDataTimedTask;
import com.izhiliu.erp.config.task.TranslationBasicDataTimedTask;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/1/28 14:42
 */
@Validated
@RestController
@RequestMapping("/api")
public class SyncBasicDataApi {

    @Resource
    private SyncBasicDataTimedTask syncShopeeBasicDataTimedTask;

    @Resource
    private TranslationBasicDataTimedTask translation;

    @GetMapping("/sync-basic-data/category")
    public String syncCategory(@RequestParam("nodeId") Long nodeId, @RequestParam("key") String key) {
        if (!key.equals("syncCategory@" + nodeId)) {
            return "key error";
        }
        syncShopeeBasicDataTimedTask.syncCategory(Arrays.asList(nodeId));

        return "EXEC";
    }

    @GetMapping("/sync-basic-data/syncChinese")
    public String syncChinese(@RequestParam("cookie") String cookie, @RequestParam("shopId") Long shopId, @RequestParam("nodeCode") String nodeCode, @RequestParam("key") String key) {
        if (!key.equals("syncChinese@" + nodeCode)) {
            return "key error";
        }
        final Long platformNodeId = ShopeeUtil.nodeId(nodeCode);
        if (platformNodeId == null) {
            return "nodeCode error";
        }
        syncShopeeBasicDataTimedTask.saveDxmCategory(cookie, shopId, platformNodeId);
        return "EXEC";
    }

    @GetMapping("/translation/category")
    public String translationCategory(@RequestParam("nodeId") Integer nodeId, @RequestParam("key") String key) {
        if (!key.equals("translationCategory@" + nodeId)) {
            return "key error";
        }

        translation.translationCategory(nodeId);
        return "EXEC";
    }
}
