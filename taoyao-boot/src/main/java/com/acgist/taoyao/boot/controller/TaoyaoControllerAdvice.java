package com.acgist.taoyao.boot.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ErrorUtils;

/**
 * 统一异常处理
 * 
 * @author acgist
 */
@RestControllerAdvice
public class TaoyaoControllerAdvice {

	@ExceptionHandler(Exception.class)
	public Message exception(Exception e, HttpServletRequest request, HttpServletResponse response) {
		return ErrorUtils.message(e, request, response);
	}

}