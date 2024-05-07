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

static std::mutex roomMutex;

namespace acgist {

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
static void init() {
    if(initTaoyao) {
        return;
    }
    initTaoyao = true;
    OH_LOG_INFO(LOG_APP, "加载libtaoyao");
    // 编码能力
    printSupportCodec();
    std::string version = mediasoupclient::Version();
    OH_LOG_INFO(LOG_APP, "加载MediasoupClient：%s", version.data());
    mediasoupclient::Initialize();
    OH_LOG_INFO(LOG_APP, "加载媒体功能");
    mediaManager = new MediaManager();
    mediaManager->initPeerConnectionFactory();
}

/**
 * 卸载系统
 */
static napi_value shutdown(napi_env env, napi_callback_info info) {
    if(!initTaoyao) {
        OH_LOG_INFO(LOG_APP, "已经卸载libtaoyao");
        return 0;
    }
    initTaoyao = false;
    OH_LOG_INFO(LOG_APP, "卸载libtaoyao");
    OH_LOG_INFO(LOG_APP, "释放mediasoupclient");
    mediasoupclient::Cleanup();
    // 删除房间
    for(auto iterator = roomMap.begin(); iterator != roomMap.end(); ++iterator) {
        delete iterator->second;
        iterator->second = nullptr;
    }
    roomMap.clear();
    // 关闭媒体
    if (mediaManager != nullptr) {
        delete mediaManager;
        mediaManager = nullptr;
    }
    // napi_delete_reference(env, acgist::sendRef);
    // napi_delete_reference(env, acgist::requestRef);
    return 0;
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
    char chars[2048];
    size_t length;
    napi_get_value_string_utf8(env, ret, chars, sizeof(chars), &length);
    return chars;
}

/**
 * 房间关闭
 */
static napi_value roomClose(napi_env env, napi_callback_info info) {
    {
        std::lock_guard<std::mutex> guard(roomMutex);
        auto iterator = roomMap.find("roomId");
        if(iterator == roomMap.end()) {
            
        } else {
            delete iterator->second;
            iterator->second = nullptr;
            roomMap.erase(iterator);
        }
    }
    return 0;
}

/**
 * 进入房间
 * 其他终端进入房间，自己进入房间逻辑参考房间邀请。
 */
static napi_value roomEnter(napi_env env, napi_callback_info info) {
    return 0;
}

/**
 * 踢出房间
 * 踢出房间以后终端离开房间
 */
static napi_value roomExpel(napi_env env, napi_callback_info info) { return 0; }

/**
 * 房间邀请
 * 邀请终端进入房间
 */
static napi_value roomInvite(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = { nullptr };
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    size_t length;
    char chars[2048];
    napi_get_value_string_utf8(env, args[0], chars, sizeof(chars), &length);
    nlohmann::json json  = nlohmann::json::parse(chars);
    nlohmann::json body  = json["body"];
    // TODO: 试试引用
    std::string roomId   = body["roomId"];
    std::string password = body["password"];
    napi_value ret;
    {
        std::lock_guard<std::mutex> guard(roomMutex);
        auto iterator = roomMap.find(roomId);
        if(iterator == roomMap.end()) {
            OH_LOG_INFO(LOG_APP, "进入房间：%s", roomId.data());
            auto room = new acgist::Room(roomId, mediaManager);
            roomMap[roomId] = room;
            int enterRet = room->enter(password);
            if(enterRet == acgist::SUCCESS_CODE) {
                room->produceMedia();
            }
            napi_create_int32(env, enterRet, &ret);
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
static napi_value roomLeave(napi_env env, napi_callback_info info) { return 0; }

/**
 * 终端列表
 * 房间所有终端列表首次进入方便加载终端列表信息
 */
static napi_value roomClientList(napi_env env, napi_callback_info info) { return 0; }

/**
 * 媒体消费（被动通知）
 */
static napi_value mediaConsume(napi_env env, napi_callback_info info)  { return 0; }

/**
 * 消费者关闭（被动通知）
 */
static napi_value mediaConsumerClose(napi_env env, napi_callback_info info)  { return 0; }

/**
 * 消费者暂停（被动通知）
 */
static napi_value mediaConsumerPause(napi_env env, napi_callback_info info)  { return 0; }

/**
 * 消费者恢复（被动通知）
 */
static napi_value mediaConsumerResume(napi_env env, napi_callback_info info) { return 0; }

/**
 * 生产者关闭（被动通知）
 */
static napi_value mediaProducerClose(napi_env env, napi_callback_info info)  { return 0; }

/**
 * 生产者暂停（被动通知）
 */
static napi_value mediaProducerPause(napi_env env, napi_callback_info info)  { return 0; }

/**
 * 生产者恢复（被动通知）
 */
static napi_value mediaProducerResume(napi_env env, napi_callback_info info) { return 0; }

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
    // napi_create_promise
    // napi_resolve_deferred
    return 0;
}

}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    acgist::env = env;
    napi_property_descriptor desc[] = {
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
