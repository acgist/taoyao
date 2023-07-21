package com.acgist.taoyao.signal.client.websocket;

import java.lang.reflect.Field;

import org.apache.catalina.connector.RequestFacade;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.model.MessageCodeException;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * WebSocket信令配置
 * 
 * @author acgist
 */
public class WebSocketSignalConfigurator extends ServerEndpointConfig.Configurator {
    
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        try {
            final Field field = request.getClass().getDeclaredField(Constant.REQUEST);
            field.setAccessible(true);
            config.getUserProperties().put(Constant.IP, ((RequestFacade) field.get(request)).getRemoteAddr());
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            throw MessageCodeException.of(e, "无效终端IP：" + request);
        }
        super.modifyHandshake(config, request, response);
    }
    
}
