/**
 * 方法绑定
 */

#include <map>
#include <string>

#include "hilog/log.h"
#include "napi/native_api.h"

#include "./include/Room.hpp"
#include "./include/MediaManager.hpp"

namespace acgist {

acgist::MediaManager* mediaManager = nullptr;
std::map<std::string, acgist::Room*> roomMap;

static void init() {
    OH_LOG_INFO(LOG_APP, "加载libtaoyao");
    // TODO：输出编码能力
    mediaManager = new MediaManager();
    mediaManager->initPeerConnectionFactory();
}

static void shutdown() {
    OH_LOG_INFO(LOG_APP, "卸载libtaoyao");
    if (mediaManager != nullptr) {
        delete mediaManager;
        mediaManager = nullptr;
    }
//     this->roomMap
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
static napi_value roomEnter(napi_env env,  napi_callback_info info) { return 0; }
static napi_value roomExpel(napi_env env,  napi_callback_info info) { return 0; }
static napi_value roomInvite(napi_env env, napi_callback_info info) { return 0; }
static napi_value roomLeave(napi_env env,  napi_callback_info info) { return 0; }

}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
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
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(napi_property_descriptor), desc);
    return exports;
}
EXTERN_C_END

static napi_module taoyaoModule = {
    .nm_version       = 1,
    .nm_flags         = 0,
    .nm_filename      = nullptr,
    .nm_register_func = Init,
    .nm_modname       = "taoyao",
    .nm_priv          = ((void*) 0),
    .reserved         = { 0 },
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void) {
    napi_module_register(&taoyaoModule);
    acgist::init();
}
