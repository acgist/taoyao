package com.acgist.taoyao.media.config;

/**
 * WebRTC STUN配置
 *
 * 注意：完全拷贝信令模块`WebrtcStunProperties`代码
 *
 * @author acgist
 */
public class WebrtcStunProperties {

    /**
     * 主机
     */
    protected String host;
    /**
     * 端口
     */
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
