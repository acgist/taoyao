/**
 * NAPI(NODE-API)
 * 
 * ETS和Native绑定入口
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/napi.md
 */

#include <map>
#include <mutex>
#include <string>

#include <hilog/log.h>

#include <napi/native_api.h>

#include "mediasoupclient.hpp"

#include "./include/Room.hpp"
#include "./include/Signal.hpp"
#include "./include/MediaManager.hpp"

#include <multimedia/player_framework/native_avcapability.h>
#include <multimedia/player_framework/native_avcodec_base.h>

static std::recursive_mutex roomMutex;
static std::recursive_mutex taoyaoMutex;

#ifndef TAOYAO_JSON_SIZE
#define TAOYAO_JSON_SIZE 2048
#endif

// 读取JSON
#ifndef TAOYAO_JSON_BODY
#define TAOYAO_JSON_BODY(size)                                               \
    napi_value ret;                                                          \
    size_t argc = size;                                                      \
    napi_value args[size] = { nullptr };                                     \
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);              \
    size_t length;                                                           \
    char chars[2048] = { 0 };                                                \
    napi_get_value_string_utf8(env, args[0], chars, sizeof(chars), &length); \
    OH_LOG_INFO(LOG_APP, "解析JSON：%s", chars);                              \
    nlohmann::json json = nlohmann::json::parse(chars, chars + length);      \
    nlohmann::json body = json["body"];
#endif

// 房间检查
#ifndef TAOYAO_ROOM_CHECK
#define TAOYAO_ROOM_CHECK(action)                                      \
    std::string roomId = body["roomId"];                               \
    auto room = acgist::roomMap.find(roomId);                          \
    if(room == acgist::roomMap.end()) {                                \
        OH_LOG_WARN(LOG_APP, "房间无效：%s %s", #action, roomId.data()); \
        napi_create_int32(env, -1, &ret);                              \
        return ret;                                                    \
    }
#endif

namespace acgist {

uint32_t width   = 720;
uint32_t height  = 480;
uint64_t bitrate = 3'000'000L;
double frameRate = 30.0;
int32_t samplingRate  = 48'000;
int32_t channelCount  = 2;
int32_t bitsPerSample = 16;

// 终端ID
static std::string clientId = "";
// 终端名称
static std::string name     = "";
// ETS环境
static napi_env env    = nullptr;
// 是否加载
static bool initTaoyao = false;
// PUSH方法引用
static napi_ref pushRef    = nullptr;
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
    // TODO: 验证是否需要释放
    OH_AVCapability* format = nullptr;
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_OPUS, true);
    OH_LOG_INFO(LOG_APP, "是否支持OPUS硬件解码：%o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_OPUS, false);
    OH_LOG_INFO(LOG_APP, "是否支持OPUS硬件解码：%o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_G711MU, true);
    OH_LOG_INFO(LOG_APP, "是否支持PCMU硬件编码：%o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_G711MU, false);
    OH_LOG_INFO(LOG_APP, "是否支持PCMU硬件解码：%o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_AVC, true);
    OH_LOG_INFO(LOG_APP, "是否支持H264硬件编码：%o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_AVC, false);
    OH_LOG_INFO(LOG_APP, "是否支持H264硬件解码：%o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_HEVC, true);
    OH_LOG_INFO(LOG_APP, "是否支持H265硬件编码：%o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_HEVC, false);
    OH_LOG_INFO(LOG_APP, "是否支持H265硬件解码：%o", OH_AVCapability_IsHardware(format));
}

/**
 * 发送消息
 */
void push(const std::string& signal, const std::string& body, uint64_t id) {
    // TODO: 验证是否需要释放
    napi_value ret;
    napi_value callback = nullptr;
    napi_get_reference_value(env, acgist::pushRef, &callback);
    napi_value data[3];
    napi_create_string_utf8(acgist::env, signal.data(), NAPI_AUTO_LENGTH, &data[0]);
    napi_create_string_utf8(acgist::env, body.data(), NAPI_AUTO_LENGTH, &data[1]);
    napi_create_int64(acgist::env, id, &data[2]);
    napi_call_function(acgist::env, nullptr, callback, 3, data, &ret);
    napi_get_undefined(acgist::env, &ret);
}

/**
 * 发送请求
 */
std::string request(const std::string& signal, const std::string& body, uint64_t id) {
    napi_value ret;
    napi_value callback = nullptr;
    napi_get_reference_value(env, acgist::requestRef, &callback);
    napi_value data[3];
    napi_create_string_utf8(acgist::env, signal.data(), NAPI_AUTO_LENGTH, &data[0]);
    napi_create_string_utf8(acgist::env, body.data(), NAPI_AUTO_LENGTH, &data[1]);
    napi_create_int64(acgist::env, id, &data[2]);
    napi_call_function(acgist::env, nullptr, callback, 3, data, &ret);
    char chars[TAOYAO_JSON_SIZE];
    size_t length;
    napi_get_value_string_utf8(env, ret, chars, sizeof(chars), &length);
    // TODO: promise
    // napi_create_promise
    // napi_resolve_deferred
    return chars;
}

/**
 * 加载系统
 */
static napi_value init(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(3);
    {
        std::lock_guard<std::recursive_mutex> taoyaoLock(taoyaoMutex);
        if(initTaoyao) {
            napi_create_int32(env, -1, &ret);
            OH_LOG_WARN(LOG_APP, "libtaoyao已经加载");
            return ret;
        }
        initTaoyao = true;
    }
    napi_create_reference(env, args[1], 1, &acgist::pushRef);
    napi_create_reference(env, args[2], 1, &acgist::requestRef);
    printSupportCodec();
//     acgist::clientId = json["clientId"];
//     acgist::name     = json["name"];
//     OH_LOG_INFO(LOG_APP, "加载libtaoyao");
//     std::string version = mediasoupclient::Version();
//     OH_LOG_INFO(LOG_APP, "加载MediasoupClient：%s", version.data());
//     mediasoupclient::Initialize();
//     OH_LOG_INFO(LOG_APP, "加载媒体功能");
//     mediaManager = new MediaManager();
//     mediaManager->init();
    napi_create_int32(env, 0, &ret);
    return ret;
}

/**
 * 卸载系统
 */
static napi_value shutdown(napi_env env, napi_callback_info info) {
    napi_value ret;
    {
        std::lock_guard<std::recursive_mutex> taoyaoLock(taoyaoMutex);
        if (!initTaoyao) {
            napi_create_int32(env, -1, &ret);
            OH_LOG_INFO(LOG_APP, "libtaoyao已经卸载");
            return ret;
        }
        initTaoyao = false;
    }
    OH_LOG_INFO(LOG_APP, "卸载libtaoyao");
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
    OH_LOG_INFO(LOG_APP, "释放mediasoupclient");
    mediasoupclient::Cleanup();
    OH_LOG_INFO(LOG_APP, "释放全局变量");
    napi_delete_reference(env, acgist::pushRef);
    napi_delete_reference(env, acgist::requestRef);
    // 置空即可
    env = nullptr;
    // 返回结果
    napi_create_int32(env, 0, &ret);
    return ret;
}

/**
 * 房间关闭
 */
static napi_value roomClose(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    {
        std::lock_guard<std::recursive_mutex> roomLock(roomMutex);
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
    TAOYAO_JSON_BODY(1);
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
    int result = room->second->newRemoteClient(clientId, name);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 踢出房间
 * 踢出房间以后终端离开房间
 */
static napi_value roomExpel(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    TAOYAO_ROOM_CHECK("踢出房间");
    OH_LOG_INFO(LOG_APP, "进入房间：%s", roomId.data());
    nlohmann::json requestBody = {
        { "roomId", roomId },
    };
    acgist::push("room::leave", requestBody.dump());
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
    TAOYAO_JSON_BODY(1);
    {
        std::lock_guard<std::recursive_mutex> roomLock(roomMutex);
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
    TAOYAO_JSON_BODY(1);
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
    TAOYAO_JSON_BODY(1);
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
    TAOYAO_JSON_BODY(1);
    TAOYAO_ROOM_CHECK("媒体消费");
    int result = room->second->newConsumer(body);
    nlohmann::json header = json["header"];
    acgist::push(header["signal"], body.dump(), header["id"]);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 消费者关闭（被动通知）
 */
static napi_value mediaConsumerClose(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    TAOYAO_ROOM_CHECK("消费者关闭");
    std::string consumerId = body["consumerId"];
    OH_LOG_INFO(LOG_APP, "消费者关闭：%s", consumerId.data());
    int result = room->second->closeConsumer(consumerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 消费者暂停（被动通知）
 */
static napi_value mediaConsumerPause(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    TAOYAO_ROOM_CHECK("消费者暂停");
    std::string consumerId = body["consumerId"];
    OH_LOG_INFO(LOG_APP, "消费者暂停：%s", consumerId.data());
    int result = room->second->pauseConsumer(consumerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 消费者恢复（被动通知）
 */
static napi_value mediaConsumerResume(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    TAOYAO_ROOM_CHECK("消费者恢复");
    std::string consumerId = body["consumerId"];
    OH_LOG_INFO(LOG_APP, "消费者恢复：%s", consumerId.data());
    int result = room->second->resumeConsumer(consumerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 生产者关闭（被动通知）
 */
static napi_value mediaProducerClose(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    TAOYAO_ROOM_CHECK("生产者关闭");
    std::string producerId = body["producerId"];
    OH_LOG_INFO(LOG_APP, "生产者关闭：%s", producerId.data());
    int result = room->second->closeProducer(producerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 生产者暂停（被动通知）
 */
static napi_value mediaProducerPause(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    TAOYAO_ROOM_CHECK("生产者暂停");
    std::string producerId = body["producerId"];
    OH_LOG_INFO(LOG_APP, "生产者暂停：%s", producerId.data());
    int result = room->second->pauseProducer(producerId);
    napi_create_int32(env, result, &ret);
    return ret;
}

/**
 * 生产者恢复（被动通知）
 */
static napi_value mediaProducerResume(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    TAOYAO_ROOM_CHECK("生产者恢复");
    std::string producerId = body["producerId"];
    OH_LOG_INFO(LOG_APP, "生产者恢复：%s", producerId.data());
    int result = room->second->resumeProducer(producerId);
    napi_create_int32(env, result, &ret);
    return ret;
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
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
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
}
