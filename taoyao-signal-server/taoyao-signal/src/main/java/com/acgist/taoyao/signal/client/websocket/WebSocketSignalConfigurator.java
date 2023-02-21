package com.acgist.taoyao.signal.client.websocket;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.catalina.connector.RequestFacade;

import com.acgist.taoyao.boot.config.Constant;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket信令配置
 * 
 * @author acgist
 */
@Slf4j
public class WebSocketSignalConfigurator extends ServerEndpointConfig.Configurator {
    
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        try {
            final Field field = request.getClass().getDeclaredField(Constant.REQUEST);
            field.setAccessible(true);
            final RequestFacade requestFacade = (RequestFacade) field.get(request);
            final Map<String, Object> userProperties = config.getUserProperties();
            userProperties.put(Constant.IP, requestFacade.getRemoteAddr());
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            log.error("WebSocket终端获取远程IP异常", e);
        }
        super.modifyHandshake(config, request, response);
    }
    
}
