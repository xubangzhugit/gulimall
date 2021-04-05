package com.izhiliu.erp.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * twitter 随机数生成算法-工具类
 *
 * @author harry(yuzh)
 * @since 2019/02/21
 */
@Component
@Scope("singleton")
public class SnowflakeGenerate {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeGenerate.class);
    private static final Snowflake snowflake = IdUtil.createSnowflake(getWorkId(), getDataCenterId());

    public Long nextId() {
        return snowflake.nextId();
    }
    public static Long staticNextId() {
        return snowflake.nextId();
    }

    /**
     * 生成 workId，避免多个节点 workId 重复造成小几率 ID 不唯一。
     */
    private static Long getWorkId() {
        long workId = 0;
        try {
            String hostAddress = Inet4Address.getLocalHost().getHostAddress();
            // int[] ints = StringUtils.toCodePoints(hostAddress + "erp");
            int[] ints = StringUtils.toCodePoints(UUID.randomUUID().toString());
            int sums = 0;
            for (int b : ints) {
                sums += b;
            }
            return workId = (long) (sums % 32);
        } catch (UnknownHostException e) {
            return workId = RandomUtils.nextLong(0, 31);
        } finally {
            log.info("[ SnowflakeGenerate#getWorkId ] workId: " + workId);
        }
    }

    /**
     * 生成 dataCenterId，避免多个节点 dataCenterId 重复造成小几率 ID 不唯一。
     */
    private static Long getDataCenterId() {
        long centerId;
        // int[] ints = StringUtils.toCodePoints(null == SystemUtils.getHostName() ? "erp" : SystemUtils.getHostName() + "erp");
        int[] ints = StringUtils.toCodePoints(UUID.randomUUID().toString());
        int sums = 0;
        for (int i : ints) {
            sums += i;
        }
        centerId = (long) (sums % 32);
        log.info("[ SnowflakeGenerate#getDataCenterId ] centerId: " + centerId);
        return centerId;
    }

    /**
     * 接取SnowflakeGenerate后13位数字作为skuCodeID
     */
    public Long skuCode(){
        return Long.parseLong(nextId().toString().substring(6));
    }


}
