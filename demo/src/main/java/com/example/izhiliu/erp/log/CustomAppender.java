package com.izhiliu.erp.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.util.EnvUtil;
import ch.qos.logback.core.util.OptionHelper;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.logback.LoghubAppender;
import com.aliyun.openservices.log.logback.LoghubAppenderCallback;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @param <E>
 * @author seriel
 */
@Getter
@Setter
public class CustomAppender<E> extends LoghubAppender<E> {

    private String mdcFields;

    protected ConsoleTarget target = ConsoleTarget.SystemOut;

    PatternLayoutEncoder myEncoder = new PatternLayoutEncoder();

    OutputStream stream;

    @Override
    public void append(E eventObject) {
        try {
            appendEvent(eventObject);
        } catch (Exception e) {
            addError("Failed to append event.", e);
        }
    }


    private void appendEvent(E eventObject) {
        boolean isSeand = false;
        //init Event Object
        if (!(eventObject instanceof LoggingEvent)) {
            return;
        }
        LoggingEvent event = (LoggingEvent) eventObject;

        List<LogItem> logItems = new ArrayList<>();
        LogItem item = new LogItem();
        logItems.add(item);
        item.SetTime((int) (event.getTimeStamp() / 1000));

        if (formatter != null) {
            DateTime dateTime = new DateTime(event.getTimeStamp());
            item.PushBack("time", dateTime.toString(formatter));
        } else {
            Instant instant = Instant.ofEpochMilli(event.getTimeStamp());
            item.PushBack("time", formatter1.format(instant));
        }

        item.PushBack("level", event.getLevel().toString());
        item.PushBack("thread", event.getThreadName());

        StackTraceElement[] caller = event.getCallerData();
        if (caller != null && caller.length > 0) {
            item.PushBack("location", caller[0].toString());
        }

        String message = event.getFormattedMessage();
        if (message.startsWith("{")) {
            isSeand = true;
            final JSONObject jsonObject = JSONObject.parseObject(message);
            jsonObject.forEach((key, cValue) -> {
                item.PushBack(key, CustomLoggerUtils.isJson(cValue));
            });
        } else {
            item.PushBack("message", message);
        }

        IThrowableProxy iThrowableProxy = event.getThrowableProxy();
        if (iThrowableProxy != null) {
            String throwable = getExceptionInfo(iThrowableProxy);
            throwable += fullDump(event.getThrowableProxy().getStackTraceElementProxyArray());
            item.PushBack("throwable", throwable);
            item.PushBack("throwableType", iThrowableProxy.getMessage());
        }
        String value = null;
        if (this.encoder != null) {
            value = new String(this.encoder.encode(eventObject));
            item.PushBack("log", value);
        }

        Optional.ofNullable(mdcFields).ifPresent(
                f -> event.getMDCPropertyMap().entrySet().stream()
                        .filter(v -> Arrays.stream(f.split(",")).anyMatch(i -> i.equals(v.getKey())))
                        .forEach(map -> item.PushBack(map.getKey(), map.getValue()))
        );
        try {
            if (isSeand || event.getLevel().levelInt != Level.DEBUG_INT) {
                producer.send(projectConfig.getProject(), logStore, topic, source, logItems, new LoghubAppenderCallback<>(this,
                        projectConfig.getProject(), logStore, topic, source, logItems));
            }
            if (EnvUtil.isWindows()) {
                if (StringUtils.isNotBlank(value)) {
                    writeBytes(myEncoder.encode(event));
                }
            }
        } catch (Exception e) {
            this.addError(
                    "Failed to send log, project=" + getProject()
                            + ", logStore=" + logStore
                            + ", topic=" + topic
                            + ", source=" + source
                            + ", logItem=" + logItems, e);
        }
    }

    private void writeBytes(byte[] byteArray) throws IOException {
        if (byteArray == null || byteArray.length == 0) {
            return;
        }

//        lock.lock();
        try {
            this.stream.write(byteArray);
            if (true) {
                this.stream.flush();
            }
        } finally {
//            lock.unlock();
        }
    }

    private final static String WindowsAnsiOutputStream_CLASS_NAME = "org.fusesource.jansi.WindowsAnsiOutputStream";

    private OutputStream getTargetStreamForWindows(OutputStream targetStream) {
        try {
            addInfo("Enabling JANSI WindowsAnsiOutputStream for the console.");
            Object windowsAnsiOutputStream = OptionHelper.instantiateByClassNameAndParameter(WindowsAnsiOutputStream_CLASS_NAME, Object.class, context,
                    OutputStream.class, targetStream);
            return (OutputStream) windowsAnsiOutputStream;
        } catch (Exception e) {
            addWarn("Failed to create WindowsAnsiOutputStream. Falling back on the default stream.", e);
        }
        return targetStream;
    }


    public CustomAppender() {
        super();
    }

    @Override
    public void start() {
        super.start();
        if (EnvUtil.isWindows()) {
            stream = getTargetStreamForWindows(target.getStream());
            myEncoder.setContext(context);
            myEncoder.setPattern("%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p [lux,%X{X-B3-TraceId:-},%X{X-B3-SpanId:-},%X{X-Span-Export:-}]) %clr(28844){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n%wEx");
            myEncoder.start();

        }
    }

    @Override
    public Producer createProducer() {
        return super.createProducer();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public String getTimeFormat() {
        return super.getTimeFormat();
    }

    @Override
    public void setTimeFormat(String timeFormat) {
        super.setTimeFormat(timeFormat);
    }

    @Override
    public String getLogStore() {
        return super.getLogStore();
    }

    @Override
    public void setLogStore(String logStore) {
        super.setLogStore(logStore);
    }

    @Override
    public String getTopic() {
        return super.getTopic();
    }

    @Override
    public void setTopic(String topic) {
        super.setTopic(topic);
    }

    @Override
    public String getSource() {
        return super.getSource();
    }

    @Override
    public void setSource(String source) {
        super.setSource(source);
    }

    @Override
    public String getTimeZone() {
        return super.getTimeZone();
    }

    @Override
    public void setTimeZone(String timeZone) {
        super.setTimeZone(timeZone);
    }

    @Override
    public String getEndpoint() {
        return super.getEndpoint();
    }

    @Override
    public void setEndpoint(String endpoint) {
        super.setEndpoint(endpoint);
    }

    @Override
    public String getAccessKeyId() {
        return super.getAccessKeyId();
    }

    @Override
    public void setAccessKeyId(String accessKeyId) {
        super.setAccessKeyId(accessKeyId);
    }

    @Override
    public String getAccessKeySecret() {
        return super.getAccessKeySecret();
    }

    @Override
    public void setAccessKeySecret(String accessKeySecret) {
        super.setAccessKeySecret(accessKeySecret);
    }

    @Override
    public String getUserAgent() {
        return super.getUserAgent();
    }

    @Override
    public void setUserAgent(String userAgent) {
        super.setUserAgent(userAgent);
    }

    @Override
    public int getTotalSizeInBytes() {
        return super.getTotalSizeInBytes();
    }

    @Override
    public void setTotalSizeInBytes(int totalSizeInBytes) {
        super.setTotalSizeInBytes(totalSizeInBytes);
    }

    @Override
    public long getMaxBlockMs() {
        return super.getMaxBlockMs();
    }

    @Override
    public void setMaxBlockMs(long maxBlockMs) {
        super.setMaxBlockMs(maxBlockMs);
    }

    @Override
    public int getIoThreadCount() {
        return super.getIoThreadCount();
    }

    @Override
    public void setIoThreadCount(int ioThreadCount) {
        super.setIoThreadCount(ioThreadCount);
    }

    @Override
    public int getBatchSizeThresholdInBytes() {
        return super.getBatchSizeThresholdInBytes();
    }

    @Override
    public void setBatchSizeThresholdInBytes(int batchSizeThresholdInBytes) {
        super.setBatchSizeThresholdInBytes(batchSizeThresholdInBytes);
    }

    @Override
    public int getBatchCountThreshold() {
        return super.getBatchCountThreshold();
    }

    @Override
    public void setBatchCountThreshold(int batchCountThreshold) {
        super.setBatchCountThreshold(batchCountThreshold);
    }

    @Override
    public int getLingerMs() {
        return super.getLingerMs();
    }

    @Override
    public void setLingerMs(int lingerMs) {
        super.setLingerMs(lingerMs);
    }

    @Override
    public int getRetries() {
        return super.getRetries();
    }

    @Override
    public void setRetries(int retries) {
        super.setRetries(retries);
    }

    @Override
    public int getMaxReservedAttempts() {
        return super.getMaxReservedAttempts();
    }

    @Override
    public void setMaxReservedAttempts(int maxReservedAttempts) {
        super.setMaxReservedAttempts(maxReservedAttempts);
    }

    @Override
    public long getBaseRetryBackoffMs() {
        return super.getBaseRetryBackoffMs();
    }

    @Override
    public void setBaseRetryBackoffMs(long baseRetryBackoffMs) {
        super.setBaseRetryBackoffMs(baseRetryBackoffMs);
    }

    @Override
    public long getMaxRetryBackoffMs() {
        return super.getMaxRetryBackoffMs();
    }

    @Override
    public void setMaxRetryBackoffMs(long maxRetryBackoffMs) {
        super.setMaxRetryBackoffMs(maxRetryBackoffMs);
    }

    @Override
    public Encoder<E> getEncoder() {
        return super.getEncoder();
    }

    @Override
    public void setEncoder(Encoder<E> encoder) {
        super.setEncoder(encoder);
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public void doAppend(E eventObject) {
        super.doAppend(eventObject);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
    }

    @Override
    public boolean isStarted() {
        return super.isStarted();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public void addFilter(Filter<E> newFilter) {
        super.addFilter(newFilter);
    }

    @Override
    public void clearAllFilters() {
        super.clearAllFilters();
    }

    @Override
    public List<Filter<E>> getCopyOfAttachedFiltersList() {
        return super.getCopyOfAttachedFiltersList();
    }

    @Override
    public FilterReply getFilterChainDecision(E event) {
        return super.getFilterChainDecision(event);
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
    }

    @Override
    public Context getContext() {
        return super.getContext();
    }

    @Override
    public StatusManager getStatusManager() {
        return super.getStatusManager();
    }

    @Override
    protected Object getDeclaredOrigin() {
        return super.getDeclaredOrigin();
    }

    @Override
    public void addStatus(Status status) {
        super.addStatus(status);
    }

    @Override
    public void addInfo(String msg) {
        super.addInfo(msg);
    }

    @Override
    public void addInfo(String msg, Throwable ex) {
        super.addInfo(msg, ex);
    }

    @Override
    public void addWarn(String msg) {
        super.addWarn(msg);
    }

    @Override
    public void addWarn(String msg, Throwable ex) {
        super.addWarn(msg, ex);
    }

    @Override
    public void addError(String msg) {
        super.addError(msg);
    }

    @Override
    public void addError(String msg, Throwable ex) {
        super.addError(msg, ex);
    }

    private String getExceptionInfo(IThrowableProxy iThrowableProxy) {
        String s = iThrowableProxy.getClassName();
        String message = iThrowableProxy.getMessage();
        return (message != null) ? (s + ": " + message) : s;
    }

    private String fullDump(StackTraceElementProxy[] stackTraceElementProxyArray) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElementProxy step : stackTraceElementProxyArray) {
            builder.append(CoreConstants.LINE_SEPARATOR);
            String string = step.toString();
            builder.append(CoreConstants.TAB).append(string);
            ThrowableProxyUtil.subjoinPackagingData(builder, step);
        }
        return builder.toString();
    }

}
