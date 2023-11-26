package com.acgist.taoyao.signal.client;

import java.util.stream.Stream;

import com.acgist.taoyao.boot.model.MessageCodeException;

import lombok.Getter;

/**
 * 终端类型
 * 
 * @author acgist
 */
@Getter
public enum ClientType {

    /**
     * 通过浏览器接入的终端
     */
    WEB("Web"),
    /**
     * 媒体服务终端
     */
    MEDIA("媒体服务"),
    /**
     * 没有界面的摄像头
     */
    CAMERA("摄像头"),
    /**
     * 手机APP、平板APP
     */
    MOBILE("移动端"),
    /**
     * 应用服务
     */
    SERVER("应用服务"),
    /**
     * 其他智能终端
     */
    OTHER("其他终端");
    
    /**
     * 终端名称
     */
    private final String name;

    private ClientType(String name) {
        this.name = name;
    }
    
    /**
     * @return 是否是媒体服务
     */
    public boolean isMedia() {
        return this == MEDIA;
    }
    
    /**
     * @return 是否是媒体终端
     */
    public boolean isClient() {
        return
            this == WEB    ||
            this == CAMERA ||
            this == MOBILE;
    }
    
    /**
     * @return 是否是应用服务
     */
    public boolean isServer() {
        return this == SERVER;
    }

    /**
     * @param value 类型
     * 
     * @return 类型
     */
    public static final ClientType of(String value) {
        final ClientType[] types = ClientType.values();
        for (final ClientType type : types) {
            if(type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw MessageCodeException.of("未知终端类型：" + value);
    }
    
    /**
     * 媒体服务类型列表
     */
    public static final ClientType[] MEDIA_TYPE = Stream.of(ClientType.values()).filter(ClientType::isMedia).toArray(ClientType[]::new);
    
    /**
     * 媒体终端类型列表
     */
    public static final ClientType[] CLIENT_TYPE = Stream.of(ClientType.values()).filter(ClientType::isClient).toArray(ClientType[]::new);
    
    /**
     * 应用服务类型列表
     */
    public static final ClientType[] SERVER_TYPE = Stream.of(ClientType.values()).filter(ClientType::isServer).toArray(ClientType[]::new);
    
}
