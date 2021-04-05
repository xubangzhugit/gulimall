package com.izhiliu.erp.log;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CustomLoggingEvent extends LoggingEvent {

    private Map<String, String> customPropertyMap;


    public CustomLoggingEvent(Logger logger, Level level, String message, Throwable throwable, Object[] argArray) {
        super(Logger.FQCN, logger, level, message, throwable, argArray);
    }
}
