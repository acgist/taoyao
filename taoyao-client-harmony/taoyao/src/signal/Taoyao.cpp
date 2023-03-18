#include "../../include/Taoyao.hpp"

namespace acgist {
    
Taoyao::Taoyao(int port, std::string address, int timeout = 5) {
    this->port          = port;
    this->address       = address;
    this->socketChannel = 0;
    this->timeout       = {timeout, 0};
    memset(&this->serverAddress, 0, sizeof(this->serverAddress));
    this->serverAddress.sin_family      = AF_INET;
    this->serverAddress.sin_port        = htons(this->port);
    this->serverAddress.sin_addr.s_addr = inet_addr(this->address.c_str());
    // 接收线程
    this->acceptThread                  = std::thread(&acgist::Taoyao::acceptSignal, this);
}

Taoyao::~Taoyao() {
}

void Taoyao::acceptSignal() {
    int status;
    char recvbuf[BUFFER_SIZE];
    while(true) {
        if(this->socketChannel == 0) {
            this->connectSignal();
            continue;
        }
        /**
         * 0  - 服务断开
         * -1 - 网络故障：如果设置超时也会出现
         */
        status = recv(this->socketChannel, recvbuf, sizeof(recvbuf), 0);
        if(status == 0) {
            this->connectSignal();
            continue;
        }
        std::cout << "接收消息：" << status << " - " << recvbuf << std::endl;
        memset(recvbuf, 0, sizeof(recvbuf));
    }
}

void Taoyao::connectSignal() {
    this->closeSignal();
    this->socketChannel = socket(AF_INET, SOCK_STREAM, 0);
    setsockopt(this->socketChannel, SOL_SOCKET, SO_SNDTIMEO, &this->timeout, sizeof(this->timeout));
    setsockopt(this->socketChannel, SOL_SOCKET, SO_RCVTIMEO, &this->timeout, sizeof(this->timeout));
    if (connect(this->socketChannel, (struct sockaddr *) &this->serverAddress, sizeof(this->serverAddress)) < 0) {
        std::cout << "连接失败：重试" << std::endl;
        this->connectSignal();
        return;
    }
    std::cout << "连接成功：" << this->address << ":" << this->port << std::endl;
}

void Taoyao::push(std::string message) {
    std::cout << "发送消息：" << message << std::endl;
    char sendbuf[message.length() + 1];
    strncpy(sendbuf, message.c_str(), message.length() + 1);
    send(this->socketChannel, sendbuf, sizeof(sendbuf), 0);
    memset(sendbuf, 0, sizeof(sendbuf));
}

void Taoyao::request(std::string message) {
}

void Taoyao::closeSignal() {
    if(this->socketChannel != 0) {
        close(this->socketChannel);
        this->socketChannel = 0;
    }
}

}

int main(int argc, char const *argv[]) {
    acgist::Taoyao taoyao(9999, "192.168.1.100");
    taoyao.connectSignal();
    char sendbuf[BUFFER_SIZE];
    while (fgets(sendbuf, sizeof(sendbuf), stdin) != NULL) {
        taoyao.push(sendbuf);
    }
    return 0;
}
