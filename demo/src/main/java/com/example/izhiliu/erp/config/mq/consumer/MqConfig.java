package com.izhiliu.erp.config.mq.consumer;

import com.izhiliu.erp.config.ApplicationProperties;
import org.slf4j.Logger;

public abstract class MqConfig {

    public MqConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    protected abstract Logger getLogger();

    protected ApplicationProperties applicationProperties;

    public String getAccess() {
        final String access = applicationProperties.getRocketMq().getAccess();
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("access {}", access);
        }
        return access;
    }

    public String getSecret() {
        final String secret = applicationProperties.getRocketMq().getSecret();
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("secret {}", secret);
        }
        return secret;
    }

    public String getAddr() {
        final String addr = applicationProperties.getRocketMq().getAddr();
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("addr {}", addr);
        }
        return addr;
    }

    public Boolean enable() {
        final Boolean enable = applicationProperties.getRocketMq().getEnable();
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("enable {}", enable);
        }
        return enable;
    }
}
