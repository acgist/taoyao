package com.acgist.taoyao.signal.client.gb;

import java.util.Properties;

import javax.sip.SipStack;

import gov.nist.core.ServerLogger;
import gov.nist.javax.sip.message.SIPMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GbServerLogger implements ServerLogger {
    
    @Override
    public void closeLogFile() {
        // -
    }

    @Override
    public void logMessage(SIPMessage message, String from, String to, boolean sender, long time) {
        this.logMessage(message, from, to, "-", sender, time);
    }
    
    @Override
    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender) {
        this.logMessage(message, from, to, status, sender, System.currentTimeMillis());
    }

    @Override
    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender, long time) {
        log.info("""
        SIP Message
        from   : {}
        to     : {}
        status : {}
        sender : {}
        time   : {}
        message:
        {}
        """, from, to, status, sender, time, message);
    }

    @Override
    public void logException(Exception e) {
        log.error(e.getMessage(), e);
    }

    @Override
    public void setSipStack(SipStack sipStack) {
        // -
    }
    
    @Override
    public void setStackProperties(Properties stackProperties) {
        // -
    }

}
