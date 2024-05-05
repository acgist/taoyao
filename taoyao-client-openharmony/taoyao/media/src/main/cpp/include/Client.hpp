/**
 * 终端
 * 
 * @author acgist
 */

#ifndef taoyao_Client_HPP
#define taoyao_Client_HPP

#include "MediaManager.hpp"

namespace acgist {

class Client {
    
public:
    /**
     * 资源释放
     * 
     * @return 是否成功
     */
    virtual bool release() = 0;
    
};

class RoomClient : public Client {
    
public:
    acgist::MediaManager* mediaManager;
    
};

class LocalClient : public RoomClient {
    
};

class RemoteClient : public RoomClient {
    
};

}

#endif // taoyao_Client_HPP
