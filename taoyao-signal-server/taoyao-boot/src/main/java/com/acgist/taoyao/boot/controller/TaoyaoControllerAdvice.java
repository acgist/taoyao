package com.acgist.taoyao.boot.controller;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ErrorUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 统一异常处理
 * 
 * 不能处理：https://localhost:8888/?\%3E%3C
 * 
 * @author acgist
 */
@Tag(name = "统一异常处理", description = "全局统一异常处理")
@RestControllerAdvice
public class TaoyaoControllerAdvice {

    @Operation(summary = "统一异常处理", description = "全局统一异常处理")
    @ExceptionHandler(Exception.class)
    public Message exception(Exception e, HttpServletRequest request, HttpServletResponse response) {
        return ErrorUtils.message(e, request, response);
    }

}