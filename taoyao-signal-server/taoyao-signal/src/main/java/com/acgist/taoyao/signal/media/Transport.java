package com.acgist.taoyao.signal.media;

import java.io.Closeable;
import java.util.Map;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.signal.MapBodyGetter;
import com.acgist.taoyao.signal.client.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * 传输通道
 * 
 * @author acgist
 */
@Getter
@Setter
public class Transport implements Closeable, MapBodyGetter {

    /**
     * 终端
     */
    private final Client client;
    private String transportId;
    private Object iceCandidates;
    private Object iceParameters;
    private Object dtlsParameters;
    private Object sctpParameters;
    
    public Transport(Client client) {
        this.client = client;
    }
    
    /**
     * 拷贝属性
     * 
     * @param body 消息主体
     */
    public void copy(Map<?, ?> body) {
        this.transportId = this.get(body, Constant.TRANSPORT_ID);
        this.iceCandidates = this.get(body, Constant.ICE_CANDIDATES);
        this.iceParameters = this.get(body, Constant.ICE_PARAMETERS);
        this.dtlsParameters = this.get(body, Constant.DTLS_PARAMETERS);
        this.sctpParameters = this.get(body, Constant.SCTP_PARAMETERS);
    }
    
    @Override
    public void close() {
    }

}
