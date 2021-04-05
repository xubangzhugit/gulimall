package com.izhiliu.erp.common;

import com.alibaba.fastjson.JSON;
import com.izhiliu.erp.web.rest.item.vm.TaskExecuteVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 异步任务操作工具类
 * @Author: louis
 * @Date: 2020/7/16 15:06
 */
@Component
public class TaskExecutorUtils {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void handleTaskSet(TaskExecuteVO.TaskDetail param) {
        String taskId = param.getTaskId();
        if (CommonUtils.isBlank(taskId)) {
            return;
        }
        String s = stringRedisTemplate.opsForValue().get(taskId);
        Integer endCode = param.getEndCode();
        String errorMessage = param.getErrorMessage();
        final String detailId = param.getDetailId();
        final Integer code = param.getCode();
        Object endData = param.getEndData();
        final String taskType = param.getTaskType();

        TaskExecuteVO vo = new TaskExecuteVO();
        if (CommonUtils.isNotBlank(s)) {
            vo = JSON.parseObject(s, TaskExecuteVO.class);
        } else {
            vo = TaskExecuteVO.builder()
                    .taskId(taskId)
                    .taskType(taskType)
                    .build();
        }
        List<TaskExecuteVO.TaskDetail> taskDetailList = CommonUtils.isNotBlank(vo.getTaskDetailList()) ? vo.getTaskDetailList() : new ArrayList<>();
        if (CommonUtils.isNotBlank(endCode)) {
            //整个任务结束
            vo.setErrorMessage(errorMessage);
            vo.setCode(endCode);
            vo.setData(endData);
            //同步修改还在pending的子任务
            if (CommonUtils.isNotBlank(taskDetailList)) {
                taskDetailList.stream().filter(e -> e.getCode().compareTo(TaskExecuteVO.CODE_PENDING) == 0)
                        .forEach(e->{
                            e.setCode(endCode);
                            e.setErrorMessage(errorMessage);
                        });
            }
        }else{
            if (CommonUtils.isNotBlank(taskDetailList)) {
                taskDetailList.removeIf(e -> e.getDetailId().equals(detailId));
            }
            taskDetailList.add(param);
            vo.setTaskDetailList(taskDetailList);
        }
        stringRedisTemplate.opsForValue().set(taskId, JSON.toJSONString(vo), 10, TimeUnit.MINUTES);
    }

    /**
     * 同步begin
     * @param key
     */
    public void syncStart(String key) {
        stringRedisTemplate.opsForValue().set(key, "0", 30, TimeUnit.MINUTES);
    }


    /**
     * 同步处理中
     * @param key
     */
    public void syncProcessing(String key) {
        if (!stringRedisTemplate.hasKey(key)) {
            return;
        }
        stringRedisTemplate.opsForValue().increment(key, 1);
    }

    /**
     * ===================start=====================
     * 初始化同步任务  用于新版同步
     * @param taskId
     * @param i
     * @param size
     */
    public void initSyncTask(String taskId, Long i, Integer size) {
        Map map = new HashMap();
        map.put("code", TaskExecuteVO.CODE_PENDING.toString());
        map.put("total", i.toString());
        if (CommonUtils.isNotBlank(size)) {
            map.put("syncShop", String.valueOf(size));
        }
        stringRedisTemplate.boundHashOps(taskId).putAll(map);
        stringRedisTemplate.boundHashOps(taskId).expire(1, TimeUnit.HOURS);
    }

    /**
     * 计数增加
     * @param taskId
     * @param size
     */
    public void incrementSyncHash(String taskId, String key, int size) {
        if (!stringRedisTemplate.hasKey(taskId)) {
            return;
        }
        stringRedisTemplate.boundHashOps(taskId).increment(key, size);
    }

    /**
     * 店铺计数
     *
     * @param taskId
     * @param key
     * @param shopId
     * @param size
     */
    public void incrementSyncShopHash(String taskId, String key, long shopId, int size) {
        incrementSyncHashBy(taskId, key, shopId, size, "_shop");
    }

    public void incrementSyncHashBy(String taskId, String key, long shopId, int size, String suffix) {
        String shopTaskId = taskId.concat(suffix);
        String shopKey = key + "_" + shopId;
        if (!stringRedisTemplate.hasKey(shopTaskId)) {
            Map map = new HashMap();
            map.put(shopKey, String.valueOf(size));
            stringRedisTemplate.boundHashOps(shopTaskId).putAll(map);
            stringRedisTemplate.boundHashOps(shopTaskId).expire(1, TimeUnit.HOURS);
            return;
        }
        stringRedisTemplate.boundHashOps(shopTaskId).increment(shopKey, size);
    }

    //===================end=====================
}
