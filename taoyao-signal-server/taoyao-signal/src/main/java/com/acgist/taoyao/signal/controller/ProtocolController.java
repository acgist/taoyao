package com.acgist.taoyao.signal.controller;

import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.property.Constant;
import com.acgist.taoyao.signal.protocol.Protocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 信令
 * 
 * @author acgist
 */
@Tag(name = "信令", description = "信令管理")
@Slf4j
@RestController
@RequestMapping("/protocol")
public class ProtocolController {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Operation(summary = "信令列表", description = "信令列表")
    @GetMapping("/list")
    public String list() {
        final StringBuilder builder = new StringBuilder("""
            ## 信令格式
            
            ```
            {
                "header": {
                    "v": "版本",
                    "id": 请求标识,
                    "signal": "信令标识"
                },
                "code": "响应编码",
                "message": "响应描述",
                "body": {
                    // 信令主体
                }
            }
            ```
            
            ### 术语解释
            
            ```
            请求：终端->信令服务 || 信令服务->媒体服务
            响应：信令服务->终端 || 服务媒体->信令服务
            广播：信令服务-)终端 || 信令服务+)终端
            ```
            
            ### 符号解释
            
            ```
            -[消息类型]> 请求（单播）：定向请求（单播）信令
            -[消息类型]) 全员广播：对所有的终端广播信令（排除自己）
            +[消息类型]) 全员广播：对所有的终端广播信令（包含自己）
            ```
            
            > 注意：没有消息类型表示请求类型
            
            """);
        final Map<String, Protocol> map = this.applicationContext.getBeansOfType(Protocol.class);
        map.entrySet().stream()
        .sorted((a, z) -> a.getValue().signal().compareTo(z.getValue().signal()))
        .forEach(e -> {
            final String key = e.getKey();
            final Protocol protocol = e.getValue();
            final String name = protocol.name();
            final String signal = protocol.signal();
            final Description annotation = protocol.getClass().getDeclaredAnnotation(Description.class);
            if(annotation == null) {
                log.info("没有注解：{}-{}", key, name);
                return;
            }
            final String memo = annotation.memo().strip();
            builder.append("### ").append(name).append("（").append(signal).append("）").append(Constant.LINE).append(Constant.LINE);
            if(StringUtils.isNotEmpty(memo)) {
                builder.append(memo).append(Constant.LINE).append(Constant.LINE);
            }
            builder.append("```").append(Constant.LINE);
            builder.append("# 消息主体").append(Constant.LINE);
            Stream.of(annotation.body()).forEach(line -> builder.append(line.strip()).append(Constant.LINE));
            builder.append("# 数据流向").append(Constant.LINE);
            Stream.of(annotation.flow()).forEach(line -> builder.append(line.strip()).append(Constant.LINE));
            builder.append("```").append(Constant.LINE).append(Constant.LINE);
        });
        return builder.toString();
    }
    
}
