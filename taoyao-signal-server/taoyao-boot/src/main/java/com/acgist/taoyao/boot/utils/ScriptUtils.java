package com.acgist.taoyao.boot.utils;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;

import lombok.extern.slf4j.Slf4j;

/**
 * 脚本工具
 * 
 * @author acgist
 */
@Slf4j
public class ScriptUtils {

    /**
     * 执行命令
     * 
     * @param script 命令
     * 
     * @return 执行结果
     */
    public static final String execute(String script) {
        if(StringUtils.isEmpty(script)) {
            throw MessageCodeException.of(MessageCode.CODE_1002, "无效命令：" + script);
        }
        String result = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(script);
            try(
                final InputStream input = process.getInputStream();
                final InputStream error = process.getErrorStream();
            ) {
                final String inputValue = new String(input.readAllBytes());
                final String errorValue = new String(input.readAllBytes());
                log.info("""
                    执行命令：{}
                    执行结果：{}
                    失败结果：{}
                    """, script, inputValue, errorValue);
                result = StringUtils.isEmpty(inputValue) ? errorValue : inputValue;
            } catch (Exception e) {
                log.error("命令执行异常：{}", script, e);
                result = e.getMessage();
            }
        } catch (Exception e) {
            log.error("执行命令异常：{}", script, e);
            result = e.getMessage();
        } finally {
            if(process != null) {
                process.destroy();
            }
        }
        return result;
    }
    
}
