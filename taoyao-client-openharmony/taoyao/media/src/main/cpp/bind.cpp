/**
 * 方法绑定
 */

#include <map>
#include <mutex>
#include <string>

#include "hilog/log.h"
#include "napi/native_api.h"

#include "mediasoupclient.hpp"

#include "./include/Room.hpp"
#include "./include/MediaManager.hpp"

#include <multimedia/player_framework/native_avcapability.h>
#include <multimedia/player_framework/native_avcodec_base.h>

namespace acgist {

static std::mutex roomMutex;

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
static napi_value shutdown(napi_env env,  napi_callback_info info) {
    if(!initTaoyao) {
        OH_LOG_INFO(LOG_APP, "已经卸载libtaoyao");
        return 0;
    }
    initTaoyao = false;
    OH_LOG_INFO(LOG_APP, "卸载libtaoyao");
    OH_LOG_INFO(LOG_APP, "释放mediasoupclient");
    mediasoupclient::Cleanup();
//     this->roomMap
    if (mediaManager != nullptr) {
        delete mediaManager;
        mediaManager = nullptr;
    }
    // napi_delete_reference(env, acgist::sendRef);
    // napi_delete_reference(env, acgist::requestRef);
    return 0;
}

static void send(const std::string& signal, const std::string& body) {
    napi_value ret;
    napi_value callback = nullptr;
    napi_get_reference_value(env, acgist::sendRef, &callback);
    napi_value data[2];
    napi_create_string_utf8(acgist::env, signal.c_str(), NAPI_AUTO_LENGTH, &data[0]);
    napi_create_string_utf8(acgist::env, body.c_str(),   NAPI_AUTO_LENGTH, &data[1]);
    napi_call_function(acgist::env, nullptr, callback, 2, data, &ret);
    // napi_get_undefined(acgist::env, &ret);
}

static std::string request(const std::string& signal, const std::string& body) {
    napi_value ret;
    napi_value callback = nullptr;
    napi_get_reference_value(env, acgist::requestRef, &callback);
    napi_value data[2];
    napi_create_string_utf8(acgist::env, signal.c_str(), NAPI_AUTO_LENGTH, &data[0]);
    napi_create_string_utf8(acgist::env, body.c_str(),   NAPI_AUTO_LENGTH, &data[1]);
    napi_call_function(acgist::env, nullptr, callback, 2, data, &ret);
    char chars[2048];
    size_t length;
    napi_get_value_string_utf8(env, ret, chars, sizeof(chars), &length);
    return chars;
}

static napi_value mediaConsume(napi_env env,       napi_callback_info info)  { return 0; }

static napi_value mediaConsumerClose(napi_env env, napi_callback_info info)  { return 0; }

static napi_value mediaConsumerPause(napi_env env, napi_callback_info info)  { return 0; }

static napi_value mediaConsumerResume(napi_env env, napi_callback_info info) { return 0; }

static napi_value mediaProducerClose(napi_env env, napi_callback_info info)  { return 0; }

static napi_value mediaProducerPause(napi_env env, napi_callback_info info)  { return 0; }

static napi_value mediaProducerResume(napi_env env, napi_callback_info info) { return 0; }

static napi_value roomClientList(napi_env env,      napi_callback_info info) { return 0; }

static napi_value roomClose(napi_env env,  napi_callback_info info) { return 0; }

/**
 * 其他终端进入房间
 */
static napi_value roomEnter(napi_env env, napi_callback_info info) {
    return 0;
}

static napi_value roomExpel(napi_env env,  napi_callback_info info) { return 0; }

/**
 * 邀请终端进入房间
 */
static napi_value roomInvite(napi_env env, napi_callback_info info) {
    napi_value ret;
    size_t argc = 1;
    napi_value args[1] = { nullptr };
    // TODO: 是否需要释放
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    char chars[2048];
    size_t length;
    // TODO: 是否需要释放
    napi_get_value_string_utf8(env, args[0], chars, sizeof(chars), &length);
    nlohmann::json json  = nlohmann::json::parse(chars);
    nlohmann::json body  = json["body"];
    std::string roomId   = body["roomId"];
    std::string password = body["password"];
    std::lock_guard<std::mutex> guard(roomMutex);
    auto iterator = roomMap.find(roomId);
    if(iterator == roomMap.end()) {
        OH_LOG_INFO(LOG_APP, "进入房间：%s", roomId.c_str());
        auto room = new acgist::Room(roomId, mediaManager);
        roomMap[roomId] = room;
        int enterRet = room->enter(password);
        napi_create_int32(env, enterRet, &ret);
    } else {
        OH_LOG_INFO(LOG_APP, "已经进入房间：%s", roomId.c_str());
        napi_create_int32(env, -1, &ret);
    }
    return ret;
}

static napi_value roomLeave(napi_env env,  napi_callback_info info) { return 0; }

static napi_value registerSend(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1] = { nullptr };
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    napi_create_reference(env, args[0], 1, &acgist::sendRef);
    return 0;
}

static napi_value registerRequest(napi_env env,  napi_callback_info info) {
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
        { "mediaConsume",        nullptr, acgist::mediaConsume,        nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaConsumerClose",  nullptr, acgist::mediaConsumerClose,  nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaConsumerPause",  nullptr, acgist::mediaConsumerPause,  nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaConsumerResume", nullptr, acgist::mediaConsumerResume, nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaProducerClose",  nullptr, acgist::mediaProducerClose,  nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaProducerPause",  nullptr, acgist::mediaProducerPause,  nullptr, nullptr, nullptr, napi_default, nullptr },
        { "mediaProducerResume", nullptr, acgist::mediaProducerResume, nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomClientList",      nullptr, acgist::roomClientList,      nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomClose",           nullptr, acgist::roomClose,           nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomEnter",           nullptr, acgist::roomEnter,           nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomExpel",           nullptr, acgist::roomExpel,           nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomInvite",          nullptr, acgist::roomInvite,          nullptr, nullptr, nullptr, napi_default, nullptr },
        { "roomLeave",           nullptr, acgist::roomLeave,           nullptr, nullptr, nullptr, napi_default, nullptr },
        { "shutdown",            nullptr, acgist::shutdown,            nullptr, nullptr, nullptr, napi_default, nullptr },
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
