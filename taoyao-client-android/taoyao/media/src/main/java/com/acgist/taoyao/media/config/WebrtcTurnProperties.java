package com.acgist.taoyao.media.config;

/**
 * WebRTC TURN配置
 *
 * 注意：完全拷贝信令模块`WebrtcTurnProperties`代码
 *
 * @author acgist
 */
public class WebrtcTurnProperties extends WebrtcStunProperties {

    /**
     * 帐号
     */
    private String username;
    /**
     * 密码
     */
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

    @Override
    public String getAddress() {
        return "turn:" + this.host + ":" + this.port;
    }

}
