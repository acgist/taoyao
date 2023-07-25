package com.acgist.taoyao.signal.client.websocket;

import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.signal.client.ClientManager;
import com.acgist.taoyao.signal.protocol.ProtocolManager;
import com.acgist.taoyao.signal.protocol.platform.PlatformErrorProtocol;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket信令
 * 
 * @author acgist
 */
@Slf4j
@ServerEndpoint(value = "/websocket.signal", configurator = WebSocketSignalConfigurator.class)
public class WebSocketSignal {
    
    private static ClientManager         clientManager;
    private static ProtocolManager       protocolManager;
    private static TaoyaoProperties      taoyaoProperties;
    private static PlatformErrorProtocol platformErrorProtocol;
    
    @OnOpen
    public void open(Session session) {
        log.debug("WebSocket信令终端连接成功：{}", session);
        WebSocketSignal.clientManager.open(new WebSocketClient(WebSocketSignal.taoyaoProperties.getTimeout(), session));
    }
    
    @OnMessage
    public void message(Session session, String message) {
        log.debug("WebSocket信令消息：{} - {}", session, message);
        try {
            WebSocketSignal.protocolManager.execute(message, session);
        } catch (Exception e) {
            log.error("处理WebSocket信令消息异常：{} - {}", WebSocketSignal.clientManager.getClients(session), message, e);
            WebSocketSignal.clientManager.push(session, WebSocketSignal.platformErrorProtocol.build(e));
        }
    }
    
    @OnClose
    public void close(Session session) {
        log.debug("WebSocket信令终端关闭：{}", session);
        WebSocketSignal.clientManager.close(session);
    }
    
    @OnError
    public void error(Session session, Throwable e) {
        log.error("WebSocket信令终端异常：{}", session, e);
        WebSocketSignal.clientManager.close(session);
    }
    
    @Autowired
    public void setClientManager(ClientManager clientManager) {
        WebSocketSignal.clientManager = clientManager;
    }

    @Autowired
    public void setProtocolManager(ProtocolManager protocolManager) {
        WebSocketSignal.protocolManager = protocolManager;
    }
    
    @Autowired
    public void setTaoyaoProperties(TaoyaoProperties taoyaoProperties) {
        WebSocketSignal.taoyaoProperties = taoyaoProperties;
    }
    
    @Autowired
    public void setPlatformErrorProtocol(PlatformErrorProtocol platformErrorProtocol) {
        WebSocketSignal.platformErrorProtocol = platformErrorProtocol;
    }
    
}
