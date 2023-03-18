#pragma once

#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <sys/shm.h>
#include <sys/types.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include <thread>
#include <iostream>

#define BUFFER_SIZE 8092

namespace acgist {

/**
 * 信令
 */
class Taoyao {
private:
    // 端口
    int port;
    // 地址
    std::string address;
    // 通道
    int socketChannel;
    // 接收线程
    std::thread acceptThread;
    // 超时
    struct timeval timeout;
    // 地址
    struct sockaddr_in serverAddress;
public:
    /**
     * @param port    端口
     * @param address 地址
     * @param timeout 超时
     */
    Taoyao(int port, std::string address, int timeout);
    /**
     * 
     */
    virtual ~Taoyao();
private:
    /**
     * 接收消息
     */
    void acceptSignal();
public:
    /**
     * 连接信令
     */
    void connectSignal();
    /**
     * 推送消息
     */
    void push(std::string message);
    /**
     * 请求消息
     */
    void request(std::string message);
    /**
     * 关闭信令
     */
    void closeSignal();
};

}