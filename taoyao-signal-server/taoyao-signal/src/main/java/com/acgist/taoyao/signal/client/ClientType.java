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

    WEB("Web"),
    MEDIA("媒体服务"),
    CAMERA("摄像头"),
    MOBILE("移动端"),
    OTHER("其他终端");
    
    /**
     * 终端名称
     */
    private final String name;

    private ClientType(String name) {
        this.name = name;
    }
    
    /**
     * @return 是否是媒体终端
     */
    public boolean mediaClient() {
        return this == WEB || this == CAMERA || this == MOBILE;
    }
    
    /**
     * @return 是否是媒体服务
     */
    public boolean mediaServer() {
        return this == MEDIA;
    }

    /**
     * @param value 类型
     * 
     * @return 类型
     */
    public static final ClientType of(String value) {
        for (ClientType type : ClientType.values()) {
            if(type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw MessageCodeException.of("未知终端类型：" + value);
    }
    
    /**
     * 媒体终端
     */
    public static final ClientType[] MEDIA_CLIENT = Stream.of(ClientType.values()).filter(ClientType::mediaClient).toArray(ClientType[]::new);
    /**
     * 媒体服务
     */
    public static final ClientType[] MEDIA_SERVER = Stream.of(ClientType.values()).filter(ClientType::mediaServer).toArray(ClientType[]::new);
    
}
