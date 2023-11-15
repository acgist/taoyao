package com.acgist.taoyao.signal.controller;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.signal.protocol.Protocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ProtocolController {

    private final ApplicationContext applicationContext;
    
    @Operation(summary = "信令列表", description = "信令列表Markdown")
    @GetMapping("/list")
    public String list() {
        final String newLine = System.lineSeparator();
        final StringBuilder builder = new StringBuilder("""
            ## 信令格式
            
            ```
            {
                "code"   : "状态编码",
                "message": "状态描述",
                "header": {
                    "v"     : "消息版本",
                    "id"    : "消息标识",
                    "signal": "信令标识"
                },
                "body": {
                    ...
                }
            }
            ```
            
            ### 符号解释
            
            ```
            -[消息类型]> 异步请求 | 单播消息
            =[消息类型]> 同步请求：到达目标终端后沿原路返回
            -[消息类型]) 全员广播：对所有的终端广播信令（排除自己）
            +[消息类型]) 全员广播：对所有的终端广播信令（包含自己）
            ...：其他自定义的透传内容
            ```
            
            > 消息类型可以省略表示和前面一致
            
            """);
        this.applicationContext.getBeansOfType(Protocol.class).entrySet().stream()
        .sorted((a, z) -> a.getValue().signal().compareTo(z.getValue().signal()))
        .forEach(e -> {
            final String   key      = e.getKey();
            final Protocol protocol = e.getValue();
            final String   name     = protocol.name();
            final String   signal   = protocol.signal();
            final Class<?> clazz;
            if(
                AopUtils.isAopProxy(e)              ||
                AopUtils.isCglibProxy(protocol)     ||
                AopUtils.isJdkDynamicProxy(protocol)
            ) {
                // 代理获取
                clazz = AopUtils.getTargetClass(protocol);
            } else {
                // 直接获取
                clazz = protocol.getClass();
            }
            final Description description = AnnotationUtils.findAnnotation(clazz, Description.class);
            if(description == null) {
                log.info("信令没有注解：{} - {}", key, name);
                return;
            }
            // 信令名称
            builder
                .append("### ").append(name)
                .append("（").append(signal).append("）")
                .append(newLine).append(newLine);
            // 描述信息
            final String memo = description.memo().strip();
            if(StringUtils.isNotEmpty(memo)) {
                builder
                    .append(memo)
                    .append(newLine).append(newLine);
            }
            // 消息主体
            builder
                .append("```")
                .append(newLine)
                .append("# 消息主体")
                .append(newLine);
            Stream.of(description.body()).forEach(line -> builder.append(line.strip()).append(newLine));
            // 数据流向
            builder
                .append("# 数据流向")
                .append(newLine);
            Stream.of(description.flow()).forEach(line -> builder.append(line.strip()).append(newLine));
            builder
                .append("```")
                .append(newLine).append(newLine);
        });
        return builder.toString();
    }
    
}
