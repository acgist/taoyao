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

#include <ctime>
#include <mutex>
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
private:
    // 当前索引
    int index       = 0;
    // 最大索引
    int maxIndex    = 999;
    // 终端索引
    int clientIndex = 99999;
    // 索引互斥
    std::mutex indexMutex;
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
     * 生成ID
     */
    long long buildId();
    /**
     * @param signal 信令标识
     * @param body   消息主体
     * @param id     消息ID
     * @param v      消息版本
     *
     * @returns 信令消息
     */
    std::string buildMessage(std::string signal, json body, long long id, std::string v);
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
    // void request(std::string message);
    /**
     * 关闭信令
     */
    void closeSignal();
};

}