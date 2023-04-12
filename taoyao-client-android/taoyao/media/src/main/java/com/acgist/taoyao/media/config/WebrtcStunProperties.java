package com.acgist.taoyao.media.config;

/**
 * WebRTC STUN配置
 *
 * @author acgist
 */
public class WebrtcStunProperties {

    protected String host;
    protected Integer port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getAddress() {
        return "stun:" + this.host + ":" + this.port;
    }

}
