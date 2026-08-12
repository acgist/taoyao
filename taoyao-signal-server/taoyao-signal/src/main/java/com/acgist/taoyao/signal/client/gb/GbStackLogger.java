package com.acgist.taoyao.signal.client.gb;

import java.util.Properties;

import gov.nist.core.StackLogger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GbStackLogger implements StackLogger {

    private int lineCount;

    @Override
    public void logStackTrace() {
        this.logStackTrace(TRACE_DEBUG);
    }

    @Override
    public void logStackTrace(int traceLevel) {
        log.debug("logStackTrace:{}", traceLevel);
    }

    @Override
    public void logTrace(String message) {
        this.countLines(message);
        log.trace(message);
    }

    @Override
    public void logDebug(String message) {
        this.countLines(message);
        log.debug(message);
    }

    @Override
    public void logDebug(String message, Exception e) {
        this.countLines(message);
        log.debug(message, e);
    }

    @Override
    public void logInfo(String message) {
        this.countLines(message);
        log.info(message);
    }

    @Override
    public void logWarning(String message) {
        this.countLines(message);
        log.warn(message);
    }

    @Override
    public void logError(String message) {
        this.countLines(message);
        log.error(message);
    }

    
    @Override
    public void logError(String message, Exception e) {
        this.countLines(message);
        log.error(message, e);
    }

    @Override
    public void logFatalError(String message) {
        this.countLines(message);
        log.error(message);
    }

    @Override
    public void logException(Throwable e) {
        log.error(e.getMessage(), e);
    }

    @Override
    public boolean isLoggingEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public boolean isLoggingEnabled(int logLevel) {
        return switch (logLevel) {
            case TRACE_TRACE -> log.isTraceEnabled();
            case TRACE_DEBUG -> log.isDebugEnabled();
            case TRACE_INFO  -> log.isInfoEnabled();
            case TRACE_WARN  -> log.isWarnEnabled();
            case TRACE_ERROR -> log.isErrorEnabled();
            case TRACE_FATAL -> log.isErrorEnabled();
            case TRACE_NONE  -> false;
            default          -> false;
        };
    }

    @Override
    public void disableLogging() {
        // -
    }

    @Override
    public void enableLogging() {
        // -
    }

    @Override
    public void setBuildTimeStamp(String buildTimeStamp) {
        // -
    }

    @Override
    public void setStackProperties(Properties stackProperties) {
        // -
    }

    @Override
    public String getLoggerName() {
        return log.getName();
    }

    @Override
    public int getLineCount() {
        return this.lineCount;
    }

    private void countLines(String message) {
        final char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\n') {
                ++this.lineCount;
            }
        }
    }

}
