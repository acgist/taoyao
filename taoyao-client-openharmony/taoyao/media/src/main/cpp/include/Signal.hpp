/**
 * 信令
 */
#ifndef TAOYAO_SIGNAL_HPP
#define TAOYAO_SIGNAL_HPP

#include <string>

#include <json.hpp>

namespace acgist {

// 成功状态编码
extern const int SUCCESS_CODE = 0;

/**
 * 发送消息
 * 
 * @param signal 信令
 * @param body   主体
 */
extern void send(const std::string& signal, const std::string& body);

/**
 * 发送请求
 * 
 * @param signal 信令
 * @param body   主体
 * 
 * @return 响应
 */
extern std::string request(const std::string& signal, const std::string& body);

}

#endif //TAOYAO_SIGNAL_HPP
