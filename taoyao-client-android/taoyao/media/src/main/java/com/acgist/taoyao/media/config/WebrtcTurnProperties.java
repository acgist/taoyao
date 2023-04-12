package com.acgist.taoyao.media.config;

/**
 * WebRTC TURN配置
 *
 * @author acgist
 */
public class WebrtcTurnProperties extends WebrtcStunProperties {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAddress() {
        return "turn:" + this.host + ":" + this.port;
    }

}
