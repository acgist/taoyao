package com.acgist.taoyao.signal.client;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import com.acgist.taoyao.boot.annotation.Manager;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.event.client.ClientCloseEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 终端管理器
 * 
 * @author acgist
 */
@Slf4j
@Manager
public class ClientManager {
    
    private final ApplicationContext applicationContext;
    
    /**
     * 终端列表
     */
    private final List<Client> clients;
    
    public ClientManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.clients            = new CopyOnWriteArrayList<>();
    }
    
    @Scheduled(cron = "${taoyao.scheduled.client:0 * * * * ?}")
    public void scheduled() {
        this.closeTimeout();
    }
    
    /**
     * 终端打开加入管理
     * 
     * @param client 终端
     */
    public void open(Client client) {
        this.clients.add(client);
    }
    
    /**
     * 授权终端单播消息
     * 
     * @param to      接收终端
     * @param message 消息
     */
    public void unicast(String to, Message message) {
        this.getClients().stream()
        .filter(v -> Objects.equals(to, v.getClientId()))
        .forEach(v -> v.push(message));
    }
    
    /**
     * 授权终端单播消息
     * 
     * @param to      接收终端
     * @param message 消息
     */
    public void unicast(Client to, Message message) {
        this.getClients().stream()
        .filter(v -> v == to)
        .forEach(v -> v.push(message));
    }
    
    /**
     * 授权终端广播消息
     * 
     * @param message     消息
     * @param clientTypes 终端类型
     */
    public void broadcast(Message message, ClientType ... clientTypes) {
        this.getClients(clientTypes).forEach(v -> v.push(message));
    }
    
    /**
     * 授权终端广播消息
     * 
     * @param from        发送终端
     * @param message     消息
     * @param clientTypes 终端类型
     */
    public void broadcast(String from, Message message, ClientType ... clientTypes) {
        this.getClients(clientTypes).stream()
        .filter(v -> !Objects.equals(from, v.getClientId()))
        .forEach(v -> v.push(message));
    }
    
    /**
     * 授权终端广播消息
     * 
     * @param from        发送终端
     * @param message     消息
     * @param clientTypes 终端类型
     */
    public void broadcast(Client from, Message message, ClientType ... clientTypes) {
        this.getClients(clientTypes).stream()
        .filter(v -> v != from)
        .forEach(v -> v.push(message));
    }
    
    /**
     * @param instance 终端实例
     * 
     * @return 终端（包含授权和未授权）
     */
    public Client getClients(AutoCloseable instance) {
        return this.clients.stream()
            .filter(v -> v.getInstance() == instance)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * @param clientId 终端ID
     * 
     * @return 授权终端
     */
    public Client getClients(String clientId) {
        return this.getClients().stream()
            .filter(v -> Objects.equals(clientId, v.getClientId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * @param clientTypes 终端类型
     * 
     * @return 授权终端列表
     */
    public List<Client> getClients(ClientType ... clientTypes) {
        return this.clients.stream()
            .filter(Client::authorized)
            .filter(client -> ArrayUtils.isEmpty(clientTypes) || ArrayUtils.contains(clientTypes, client.getClientType()))
            .toList();
    }
    
    /**
     * @param instance 终端实例
     * 
     * @return 终端状态
     */
    public ClientStatus getStatus(AutoCloseable instance) {
        final Client client = this.getClients(instance);
        return client == null ? null : client.getStatus();
    }
    
    /**
     * @param clientId 终端ID
     * 
     * @return 授权终端状态
     */
    public ClientStatus getStatus(String clientId) {
        final Client client = this.getClients(clientId);
        return client == null ? null : client.getStatus();
    }

    /**
     * @param clientTypes 终端类型
     * 
     * @return 授权终端状态列表
     */
    public List<ClientStatus> getStatus(ClientType ... clientTypes) {
        return this.getClients(clientTypes).stream()
            .map(Client::getStatus)
            .toList();
    }

    /**
     * 推送消息
     * 
     * @param instance 终端实例
     * @param message  消息
     */
    public void push(AutoCloseable instance, Message message) {
        final Client client = this.getClients(instance);
        if(client == null) {
            log.warn("推送消息终端无效：{} - {}", instance, message);
            return;
        }
        client.push(message);
    }
    
    /**
     * 关闭终端
     * 
     * @param instance 终端实例
     */
    public void close(AutoCloseable instance) {
        final Client client = this.getClients(instance);
        try {
            if(client != null) {
                client.close();
            } else {
                instance.close();
            }
        } catch (Exception e) {
            log.error("关闭终端异常：{}", instance, e);
        } finally {
            if(client != null) {
                this.clients.remove(client);
                this.applicationContext.publishEvent(new ClientCloseEvent(client));
            }
        }
    }
    
    /**
     * 定时关闭超时终端
     */
    private void closeTimeout() {
        final int oldSize = this.clients.size();
        this.clients.stream()
        .filter(v -> {
            if(v.unauthorized()) {
                return v.authorizeTimeout();
            } else {
                return v.heartbeatTimeout();
            }
        })
        .forEach(v -> {
            log.debug("关闭超时终端：{}", v);
            this.close(v);
        });
        final int newSize = this.clients.size();
        log.debug("定时关闭超时终端：{}", newSize - oldSize);
    }

}
