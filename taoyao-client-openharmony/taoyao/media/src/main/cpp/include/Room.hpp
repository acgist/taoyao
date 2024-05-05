/**
 * 房间
 * 
 * @author acgist
 */

#ifndef taoyao_Room_HPP
#define taoyao_Room_HPP

#include <string>
#include <vector>

#include "./Client.hpp"
#include "./MediaManager.hpp"

namespace acgist {

class Room {
    
public:
    bool audioProduce = true;
    bool videoProduce = true;
    bool audioConsume = true;
    bool videoConsume = true;
    std::string roomId = "";
    // 本地终端
    acgist::LocalClient* client = nullptr;
    // 远程终端
    std::vector<acgist::RemoteClient*> clients;
    // 媒体管理
    acgist::MediaManager* mediaManager = nullptr;
    
public:
    Room(const std::string& roomId, acgist::MediaManager* mediaManager);
    virtual ~Room();
    
public:
    /**
     * 进入房间
     * 
     * @param password 密码
     * 
     * @return 是否成功
     */
    int enter(const std::string& password);
    int close();
    int newRemoteClient();
    
};

}

#endif // taoyao_Room_HPP
