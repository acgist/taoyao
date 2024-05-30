/**
 * NAPI(NODE-API)
 * 
 * ETS和Native绑定入口
 * 
 * @author acgist
 * 
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/reference/native-lib/napi.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/napi/use-napi-thread-safety.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/performance/native-threads-call-js.md
 * https://docs.openharmony.cn/pages/v4.1/zh-cn/application-dev/performance/develop-Native-modules-using-NAPI-safely-and-efficiently.md
 */

#include <map>
#include <mutex>
#include <string>
#include <future>
#include <thread>
#include <chrono>
#include <functional>

#include <uv.h>

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
#define TAOYAO_JSON_BODY(size)                                                           \
    napi_value ret;                                                                      \
    size_t argc = size;                                                                  \
    napi_value args[size] = { nullptr };                                                 \
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);                          \
    size_t length;                                                                       \
    char* chars = new char[16 * 1024] { 0 };                                             \
    napi_get_value_string_utf8(env, args[0], chars, 16 * 1024, &length);                 \
    if(length <= 0) {                                                                    \
        OH_LOG_WARN(LOG_APP, "TAOYAO ERROR JSON: %{public}d %{public}s", length, chars); \
        napi_create_int32(env, -1, &ret);                                                \
        delete[] chars;                                                                  \
        return ret;                                                                      \
    }                                                                                    \
    OH_LOG_DEBUG(LOG_APP, "TAOYAO JSON: %{public}d %{public}s", length, chars);          \
    nlohmann::json json = nlohmann::json::parse(chars, chars + length);                  \
    delete[] chars;                                                                      \
    nlohmann::json body = json["body"];
#endif

// 房间检查
#ifndef TAOYAO_ROOM_CHECK
#define TAOYAO_ROOM_CHECK(action)                                                                    \
    std::string roomId = body["roomId"];                                                             \
    auto room = acgist::roomMap.find(roomId);                                                        \
    if(room == acgist::roomMap.end()) {                                                              \
        OH_LOG_WARN(LOG_APP, "TAOYAO ERROR ROOM ID: %{public}s %{public}s", #action, roomId.data()); \
        napi_create_int32(env, -1, &ret);                                                            \
        return ret;                                                                                  \
    }
#endif

namespace acgist {

// 索引：667-999
static uint32_t index       = 667;
// 最小索引
static uint32_t minIndex    = 667;
// 最大索引
static uint32_t maxIndex    = 999;
// 终端索引
static uint32_t clientIndex = 99999;
// 是否加载
static bool initTaoyao = false;
// push方法引用
static napi_ref pushRef = nullptr;
// request方法引用
static napi_ref requestRef = nullptr;
// 图片收集引用
static napi_ref imageReceiverRef = nullptr;
// 媒体功能
static acgist::MediaManager* mediaManager = nullptr;
// 房间管理
static std::map<std::string, acgist::Room*> roomMap;
// 异步回调
static std::map<uint64_t, std::promise<nlohmann::json>*> promiseMap;

/**
 * 支持的编解码
 */
static void printSupportCodec() {
    // TODO: 验证是否需要释放
    OH_AVCapability* format = nullptr;
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_OPUS, true);
    OH_LOG_INFO(LOG_APP, "是否支持OPUS硬件解码：%{public}o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_OPUS, false);
    OH_LOG_INFO(LOG_APP, "是否支持OPUS硬件解码：%{public}o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_G711MU, true);
    OH_LOG_INFO(LOG_APP, "是否支持PCMU硬件编码：%{public}o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_AUDIO_G711MU, false);
    OH_LOG_INFO(LOG_APP, "是否支持PCMU硬件解码：%{public}o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_AVC, true);
    OH_LOG_INFO(LOG_APP, "是否支持H264硬件编码：%{public}o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_AVC, false);
    OH_LOG_INFO(LOG_APP, "是否支持H264硬件解码：%{public}o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_HEVC, true);
    OH_LOG_INFO(LOG_APP, "是否支持H265硬件编码：%{public}o", OH_AVCapability_IsHardware(format));
    format = OH_AVCodec_GetCapability(OH_AVCODEC_MIMETYPE_VIDEO_HEVC, false);
    OH_LOG_INFO(LOG_APP, "是否支持H265硬件解码：%{public}o", OH_AVCapability_IsHardware(format));
}

struct Message {
    uint64_t    id;
    std::string signal;
    std::string body;
};

static void pushCallback(uv_work_t* work) {
}

static void afterPushCallback(uv_work_t* work, int status) {
    Message* message = (Message*) work->data;
    napi_handle_scope scope = nullptr;
    napi_open_handle_scope(acgist::env, &scope);
    if(scope == nullptr) {
        delete message;
        delete work;
        return;
    };
    // 开始执行ETS函数
    napi_value ret;
    napi_value callback = nullptr;
    napi_get_reference_value(acgist::env, acgist::pushRef, &callback);
    napi_value data[3];
    napi_create_string_utf8(acgist::env, message->signal.data(), NAPI_AUTO_LENGTH, &data[0]);
    napi_create_string_utf8(acgist::env, message->body.data(), NAPI_AUTO_LENGTH, &data[1]);
    napi_create_int64(acgist::env, message->id, &data[2]);
    napi_call_function(acgist::env, nullptr, callback, 3, data, &ret);
    napi_get_undefined(acgist::env, &ret);
    // 释放资源
    napi_close_handle_scope(acgist::env, scope);
    delete message;
    delete work;
}

/**
 * 发送消息
 */
void push(const std::string& signal, const std::string& body, uint64_t id) {
    uv_loop_s* loop = nullptr;
    napi_get_uv_event_loop(acgist::env, &loop);
    uv_work_t* work = new uv_work_t{};
    work->data = new Message{ id, signal, body };
    uv_queue_work(loop, work, pushCallback, afterPushCallback);
}

static void requestCallback(uv_work_t* work) {
}

static void afterRequestCallback(uv_work_t* work, int status) {
    Message* message = (Message*) work->data;
    napi_handle_scope scope = nullptr;
    napi_open_handle_scope(acgist::env, &scope);
    if(scope == nullptr) {
        delete message;
        delete work;
        return;
    };
    // 开始执行ETS函数
    napi_value ret;
    napi_value callback = nullptr;
    napi_get_reference_value(env, acgist::requestRef, &callback);
    napi_value data[3];
    napi_create_string_utf8(acgist::env, message->signal.data(), NAPI_AUTO_LENGTH, &data[0]);
    napi_create_string_utf8(acgist::env, message->body.data(), NAPI_AUTO_LENGTH, &data[1]);
    napi_create_int64(acgist::env, message->id, &data[2]);
    napi_call_function(acgist::env, nullptr, callback, 3, data, &ret);
    napi_get_undefined(acgist::env, &ret);
    // 释放资源
    napi_close_handle_scope(acgist::env, scope);
    delete message;
    delete work;
}

/**
 * 发送请求
 */
nlohmann::json request(const std::string& signal, const std::string& body, uint64_t id) {
    uv_loop_s* loop = nullptr;
    napi_get_uv_event_loop(acgist::env, &loop);
    uv_work_t* work = new uv_work_t{};
    if(id <= 0L) {
        if (++acgist::index > acgist::maxIndex) {
          acgist::index = acgist::minIndex;
        }
        auto now         = std::chrono::system_clock::now();
        std::time_t time = std::chrono::system_clock::to_time_t(now);
        std::tm*    tm   = std::localtime(&time);
        id =
            100000000000000L * tm->tm_mday          +
            1000000000000    * tm->tm_hour          +
            10000000000      * tm->tm_min           +
            100000000        * tm->tm_sec           +
            1000             * acgist::clientIndex  +
            acgist::index;
    }
    std::promise<nlohmann::json>* promise = new std::promise<nlohmann::json>{};
    acgist::promiseMap.insert({ id, promise });
    work->data = new Message{ id, signal, body };
    uv_queue_work(loop, work, requestCallback, afterRequestCallback);
    std::future<nlohmann::json> future = promise->get_future();
    if(future.wait_for(std::chrono::seconds(5)) == std::future_status::timeout) {
        OH_LOG_WARN(LOG_APP, "请求超时：%{public}s %{public}s", signal.data(), body.data());
        acgist::promiseMap.erase(id);
        delete promise;
        return nlohmann::json{};
    } else {
        OH_LOG_DEBUG(LOG_APP, "请求响应：%{public}lld", id);
        acgist::promiseMap.erase(id);
        delete promise;
        return future.get();
//      return std::move(future.get());
    }
}

struct Adync {
    std::function<void()>* function;
};

static void asyncCallback(uv_work_t* work) {
    Adync* async = (Adync*) work->data;
    (*async->function)();
    delete async;
}

static void afterAsyncCallback(uv_work_t* work, int status) {
    delete work;
}

/**
 * 异步执行
 * 注意：此处不能使用promise-future等待
 * 
 * @param function 方法
 * 
 * @return 结果
 */
static int asyncExecute(std::function<void()> function) {
//    uv_loop_s* loop = nullptr;
//    napi_get_uv_event_loop(acgist::env, &loop);
//    uv_work_t* work = new uv_work_t{};
//    work->data = new Adync { &function };
//    uv_queue_work(loop, work, asyncCallback, afterAsyncCallback);
    std::thread thread(function);
    thread.detach();
    return 0;
}

/**
 * 加载系统
 */
static napi_value init(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(4);
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
    napi_create_reference(env, args[3], 1 ,&acgist::imageReceiverRef);
    napi_value imageReceiver;
    napi_get_reference_value(env, acgist::imageReceiverRef, &imageReceiver);
    acgist::imageReceiverNative = OH_Image_Receiver_InitImageReceiverNative(env, imageReceiver);
    OH_LOG_INFO(LOG_APP, "配置图片接收：%{public}lld %{public}lld", imageReceiver, acgist::imageReceiverNative);
    printSupportCodec();
    acgist::clientId    = json["clientId"];
    acgist::name        = json["name"];
    acgist::surfaceId   = json["surfaceId"];
    acgist::clientIndex = json["clientIndex"];
    OH_LOG_INFO(LOG_APP, "加载libtaoyao：%{public}s %{public}s", acgist::clientId.data(), acgist::surfaceId.data());
    std::string version = mediasoupclient::Version();
    OH_LOG_INFO(LOG_APP, "加载MediasoupClient：%{public}s", version.data());
    mediasoupclient::Initialize();
    OH_LOG_INFO(LOG_APP, "加载媒体功能");
    mediaManager = new MediaManager();
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
    if(acgist::pushRef != nullptr) {
        napi_delete_reference(env, acgist::pushRef);
        acgist::pushRef = nullptr;
    }
    if(acgist::requestRef != nullptr) {
        napi_delete_reference(env, acgist::requestRef);
        acgist::requestRef = nullptr;
    }
    if(acgist::imageReceiverRef != nullptr) {
        napi_delete_reference(env, acgist::imageReceiverRef);
        acgist::imageReceiverRef = nullptr;
    }
    if(acgist::imageReceiverNative != nullptr) {
        delete acgist::imageReceiverNative;
        acgist::imageReceiverNative = nullptr;
    }
    // 置空即可
    env = nullptr;
    // 返回结果
    napi_create_int32(env, 0, &ret);
    return ret;
}

/**
 * Promise回调
 */
static napi_value callback(napi_env env, napi_callback_info info) {
    TAOYAO_JSON_BODY(1);
    nlohmann::json header = json["header"];
    uint64_t id = header["id"];
    auto promise = acgist::promiseMap.find(id);
    if(promise == acgist::promiseMap.end()) {
        napi_create_int32(env, -1, &ret);
        OH_LOG_DEBUG(LOG_APP, "Promise回调无效：%{public}lld", id);
    } else {
        napi_create_int32(env, 0, &ret);
        promise->second->set_value(std::move(json));
        OH_LOG_DEBUG(LOG_APP, "Promise回调成功：%{public}lld", id);
    }
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
        std::string roomId = body["roomId"];
        std::string password;
        if(body.find("password") != body.end()) {
            password = body["password"];
        }
        auto oldRoom = acgist::roomMap.find(roomId);
        if(oldRoom == acgist::roomMap.end()) {
            OH_LOG_INFO(LOG_APP, "进入房间：%{public}s", roomId.data());
            auto room = new acgist::Room(roomId, mediaManager);
            acgist::roomMap.insert({ roomId, room });
            int result = asyncExecute([room, roomId, password]() {
                int code = room->enter(password);
                if(code == acgist::SUCCESS_CODE) {
                    room->produceMedia();
                } else {
                    acgist::roomMap.erase(roomId);
                    delete room;
                }
            });
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
        { "callback",            nullptr, acgist::callback,            nullptr, nullptr, nullptr, napi_default, nullptr },
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
