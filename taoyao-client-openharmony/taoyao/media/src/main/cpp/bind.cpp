/**
 * NAPI(NODE-API)
 * 
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/napi.md
 */

#include <map>
#include <mutex>
#include <string>

#include "hilog/log.h"
#include "napi/native_api.h"

#include "mediasoupclient.hpp"

#include "./include/Room.hpp"
#include "./include/Signal.hpp"
#include "./include/MediaManager.hpp"

#include <multimedia/player_framework/native_avcapability.h>
#include <multimedia/player_framework/native_avcodec_base.h>

static std::mutex taoyaoMutex;
static std::mutex roomMutex;

#ifndef TAOYAO_JSON_SIZE
#define TAOYAO_JSON_SIZE 2048
#endif

#ifndef TAOYAO_JSON_BODY
#define TAOYAO_JSON_BODY()                                                   \
napi_value ret;                                                              \
    size_t argc = 1;                                                         \
    napi_value args[1] = {nullptr};                                          \
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);              \
    size_t length;                                                           \
    char chars[2048];                                                        \
    napi_get_value_string_utf8(env, args[0], chars, sizeof(chars), &length); \
    nlohmann::json json = nlohmann::json::parse(chars);                      \
    nlohmann::json body = json["body"];
#endif

#ifndef TAOYAO_ROOM_CHECK
#define TAOYAO_ROOM_CHECK(action)                                   \
std::string roomId = body["roomId"];                                \
auto room = acgist::roomMap.find(roomId);                           \
if(room == acgist::roomMap.end()) {                                 \
    OH_LOG_WARN(LOG_APP, "房间无效：%s %s", #action, roomId.data()); \
    napi_create_int32(env, -1, &ret);                               \
    return ret;                                                     \
}
#endif

namespace acgist {

// 终端ID
static std::string clientId = "";
// JS环境
static napi_env env = nullptr;
// 是否加载
static bool initTaoyao = false;
// SEND方法引用
static napi_ref sendRef = nullptr;
// REQUEST方法引用
static napi_ref requestRef = nullptr;
// 媒体功能
static acgist::MediaManager* mediaManager = nullptr;
// 房间管理
static std::map<std::string, acgist::Room*> roomMap;

/**
 * 支持的编解码
 */
static void printSupportCodec() {
    // TODO：是否需要释放
    OH_AVCapability* opus = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_OPUS, false);
    OH_LOG_INFO(LOG_APP, "是否支持OPUS：%o", OH_AVCapability_IsHardware(opus));
    OH_AVCapability* pcmu = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_G711MU, false);
    OH_LOG_INFO(LOG_APP, "是否支持PCMU：%o", OH_AVCapability_IsHardware(pcmu));
    OH_AVCapability* h264 = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_AVC, false);
    OH_LOG_INFO(LOG_APP, "是否支持H264：%o", OH_AVCapability_IsHardware(h264));
    OH_AVCapability* h265 = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_HEVC, false);
    OH_LOG_INFO(LOG_APP, "是否支持H264：%o", OH_AVCapability_IsHardware(h265));
}

/**
 * 加载系统
 */
static void init(napi_env env, napi_callback_info info) {
    {
        std::lock_guard<std::mutex> taoyaoLock(taoyaoMutex);
        if(initTaoyao) {
            return;
        }
        initTaoyao = true;
    }
    TAOYAO_JSON_BODY();
    acgist::clientId = json["clientId"];
    OH_LOG_INFO(LOG_APP, "加载libtaoyao");
    printSupportCodec();
    std::string version = mediasoupclient::Version();
    OH_LOG_INFO(LOG_APP, "加载MediasoupClient：%s", version.data());
    mediasoupclient::Initialize();
    OH_LOG_INFO(LOG_APP, "加载媒体功能");
    mediaManager = new MediaManager();
    mediaManager->init();
}

/**
 * 卸载系统
 */
static napi_value shutdown(napi_env env, napi_callback_info info) {
    {
        std::lock_guard<std::mutex> taoyaoLock(taoyaoMutex);
        if(!initTaoyao) {
            OH_LOG_INFO(LOG_APP, "已经卸载libtaoyao");
            return 0;
        }
        initTaoyao = false;
    }
    OH_LOG_INFO(LOG_APP, "卸载libtaoyao");
    OH_LOG_INFO(LOG_APP, "释放mediasoupclient");
    mediasoupclient::Cleanup();
    OH_LOG_INFO(LOG_APP, "清空房间");
    for(auto iterator = acgist::roomMap.begin(); iterator != acgist::roomMap.end(); ++iterator) {
        delete iterator->second;
        iterator->second = nullptr;
    }
    acgist::roomMap.clear();
    OH_LOG_INFO(LOG_APP, "关闭媒体");
    if (mediaManager != nullptr) {
        delete mediaManager;
        mediaManager = nullptr;
    }
    OH_LOG_INFO(LOG_APP, "释放全局变量");
    // napi_delete_reference(env, acgist::sendRef);
    // napi_delete_reference(env, acgist::requestRef);
    env = nullptr;
    // 返回结果
    napi_value ret;
    napi_create_int32(env, 0, &ret);
    return ret;
}

/**
 * 发送消息
 */
void send(const std::string& signal, const std::string& body) {
    napi_value ret;
    napi_value callback = nullptr;
    napi_get_reference_value(env, acgist::sendRef, &callback);
    napi_value data[2];
    napi_create_string_utf8(acgist::env, signal.data(), NAPI_AUTO_LENGTH, &data[0]);
    napi_create_string_utf8(acgist::env, body.data(),   NAPI_AUTO_LENGTH, &data[1]);
    napi_call_function(acgist::env, nullptr, callback, 2, data, &ret);
    // napi_get_undefined(acgist::env, &ret);
}

/**
 * 发送请求
 */
std::string request(const std::string& signal, const std::string& body) {
    napi_value ret;
    napi_value callback = nullptr;
    napi_get_reference_value(env, acgist::requestRef, &callback);
    napi_value data[2];
    napi_create_string_utf8(acgist::env, signal.data(), NAPI_AUTO_LENGTH, &data[0]);
    napi_create_string_utf8(acgist::env, body.data(),   NAPI_AUTO_LENGTH, &data[1]);
    napi_call_function(acgist::env, nullptr, callback, 2, data, &ret);
    char chars[TAOYAO_JSON_SIZE];
    size_t length;
    napi_get_value_string_utf8(env, ret, chars, sizeof(chars), &length);
    // TODO: promise
    // napi_create_promise
    // napi_resolve_deferred
    return chars;
}

/**
 * 房间关闭
 */
static napi_value roomClose(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    {
        std::lock_guard<std::mutex> roomLock(roomMutex);
        std::string roomId = body["roomId"];
        auto iterator = acgist::roomMap.find(roomId);
        if(iterator == acgist::roomMap.end()) {
            OH_LOG_WARN(LOG_APP, "关闭房间无效：%s", roomId.data());
            napi_create_int32(env, -1, &ret);
        } else {
            OH_LOG_INFO(LOG_APP, "关闭房间：%s", roomId.data());
            delete iterator->second;
            iterator->second = nullptr;
            acgist::roomMap.erase(iterator);
            napi_create_int32(env, 0, &ret);
        }
    }
    return ret;
}

/**
 * 进入房间
 * 其他终端进入房间，自己进入房间逻辑参考房间邀请。
 */
static napi_value roomEnter(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("进入房间");
    std::string clientId = body["clientId"];
    OH_LOG_INFO(LOG_APP, "进入房间：%s %s", roomId.data(), clientId.data());
    if(clientId == acgist::clientId) {
        // 忽略关闭自己
        napi_create_int32(env, -2, &ret);
        return ret;
    }
    nlohmann::json status = body["status"];
    std::string name = status["name"];
    int result = room->newRemoteClient(clientId, name);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 踢出房间
 * 踢出房间以后终端离开房间
 */
static napi_value roomExpel(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("踢出房间");
    OH_LOG_INFO(LOG_APP, "进入房间：%s", roomId.data());
    nlohmann::json requestBody = {
        { "roomId", roomId },
    };
    acgist::send("room::leave", requestBody.dump());
    delete room->second;
    room->second = nullptr;
    acgist::roomMap.erase(room);
    napi_create_int32(env, 0, &ret);
    return ret;
}

/**
 * 房间邀请
 * 邀请终端进入房间
 */
static napi_value roomInvite(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    {
        std::lock_guard<std::mutex> roomLock(roomMutex);
        // TODO: 试试引用
        std::string roomId   = body["roomId"];
        std::string password = body["password"];
        auto oldRoom = acgist::roomMap.find(roomId);
        if(oldRoom == acgist::roomMap.end()) {
            OH_LOG_INFO(LOG_APP, "进入房间：%s", roomId.data());
            auto room = new acgist::Room(roomId, mediaManager);
            int result = room->enter(password);
            if(result == acgist::SUCCESS_CODE) {
                acgist::roomMap.insert({ roomId, room });
                room->produceMedia();
            } else {
                delete room;
            }
            napi_create_int32(env, result, &ret);
        } else {
            OH_LOG_INFO(LOG_APP, "已经进入房间：%s", roomId.data());
            napi_create_int32(env, -1, &ret);
        }
    }
    return ret;
}

/**
 * 离开房间
 * 其他终端离开房间
 */
static napi_value roomLeave(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("离开房间");
    std::string clientId = body["clientId"];
    OH_LOG_INFO(LOG_APP, "离开房间：%s %s", roomId.data(), clientId.data());
    if(clientId == acgist::clientId) {
        // 忽略关闭自己
        napi_create_int32(env, -2, &ret);
        return ret;
    }
    int result = room->second->closeRemoteClient(clientId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 终端列表
 * 房间所有终端列表首次进入方便加载终端列表信息
 */
static napi_value roomClientList(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("终端列表");
    nlohmann::json clients = body["clients"];
    if(clients.empty()) {
        napi_create_int32(env, 0, &ret);
        return ret;
    }
    for(auto client = clients.begin(); client != clients.end(); ++client) {
        std::string clientId = (*client)["clientId"];
        std::string name     = (*client)["name"];
        if(clientId == acgist::clientId) {
            // 忽略关闭自己
            continue;
        }
        room->second->newRemoteClient(clientId, name);
    }
    napi_create_int32(env, 0, &ret);
    return ret;
}

/**
 * 媒体消费（被动通知）
 */
static napi_value mediaConsume(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK();
    TAOYAO_ROOM_CHECK("媒体消费");
    int result = room->second->newConsumer(body);
    acgist::send(chars);
    // acgist::send(json.dump());
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 消费者关闭（被动通知）
 */
static napi_value mediaConsumerClose(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("消费者关闭");
    std::string consumerId = body["consumerId"];
    TAOYAO_ROOM_CHECK("消费者关闭：%s", consumerId.data());
    int result = room->second->closeConsumer(consumerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 消费者暂停（被动通知）
 */
static napi_value mediaConsumerPause(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("消费者暂停");
    std::string consumerId = body["consumerId"];
    TAOYAO_ROOM_CHECK("消费者暂停：%s", consumerId.data());
    int result = room->second->pauseConsumer(consumerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 消费者恢复（被动通知）
 */
static napi_value mediaConsumerResume(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("消费者恢复");
    std::string consumerId = body["consumerId"];
    TAOYAO_ROOM_CHECK("消费者恢复：%s", consumerId.data());
    int result = room->second->resumeConsumer(consumerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 生产者关闭（被动通知）
 */
static napi_value mediaProducerClose(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("生产者关闭");
    std::string producerId = body["producerId"];
    TAOYAO_ROOM_CHECK("生产者关闭：%s", producerId.data());
    int result = room->second->closeProducer(producerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 生产者暂停（被动通知）
 */
static napi_value mediaProducerPause(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("生产者暂停");
    std::string producerId = body["producerId"];
    TAOYAO_ROOM_CHECK("生产者暂停：%s", producerId.data());
    int result = room->second->pauseProducer(producerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 生产者恢复（被动通知）
 */
static napi_value mediaProducerResume(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY();
    TAOYAO_ROOM_CHECK("生产者恢复");
    std::string producerId = body["producerId"];
    TAOYAO_ROOM_CHECK("生产者恢复：%s", producerId.data());
    int result = room->second->resumeProducer(producerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 注册发送回调
 */
static napi_value registerSend(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = { nullptr };
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    napi_create_reference(env, args[0], 1, &acgist::sendRef);
    return 0;
}

/**
 * 注册请求回调
 */
static napi_value registerRequest(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = { nullptr };
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    napi_create_reference(env, args[0], 1, &acgist::requestRef);
    return 0;
}

}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    acgist::env = env;
    napi_property_descriptor desc[] = {
        { "init",                nullptr, acgist::init,                nullptr, nullptr, nullptr, napi_default, nullptr },
        { "shutdown",            nullptr, acgist::shutdown,            nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomClose",           nullptr, acgist::roomClose,           nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomEnter",           nullptr, acgist::roomEnter,           nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomExpel",           nullptr, acgist::roomExpel,           nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomInvite",          nullptr, acgist::roomInvite,          nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomLeave",           nullptr, acgist::roomLeave,           nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomClientList",      nullptr, acgist::roomClientList,      nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaConsume",        nullptr, acgist::mediaConsume,        nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaConsumerClose",  nullptr, acgist::mediaConsumerClose,  nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaConsumerPause",  nullptr, acgist::mediaConsumerPause,  nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaConsumerResume", nullptr, acgist::mediaConsumerResume, nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaProducerClose",  nullptr, acgist::mediaProducerClose,  nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaProducerPause",  nullptr, acgist::mediaProducerPause,  nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaProducerResume", nullptr, acgist::mediaProducerResume, nullptr, nullptr, nullptr, napi_default, nullptr },
        { "registerSend",        nullptr, acgist::registerSend,        nullptr, nullptr, nullptr, napi_default, nullptr },
        { "registerRequest",     nullptr, acgist::registerRequest,     nullptr, nullptr, nullptr, napi_default, nullptr },
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(napi_property_descriptor), desc);
    return exports;
}
EXTERN_C_END

static napi_module libtaoyaoModule = {
    .nm_version       = 1,
    .nm_flags         = 0,
    .nm_filename      = nullptr,
    .nm_register_func = Init,
    .nm_modname       = "taoyao",
    .nm_priv          = ((void*) 0),
    .reserved         = { 0 },
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void) {
    napi_module_register(&libtaoyaoModule);
    acgist::init();
}
