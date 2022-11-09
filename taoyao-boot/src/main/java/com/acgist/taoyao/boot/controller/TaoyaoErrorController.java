package com.acgist.taoyao.boot.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.ErrorUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 统一错误页面
 * 
 * @author acgist
 */
@Tag(name = "统一错误页面", description = "全局统一错误页面")
@RestController
public class TaoyaoErrorController implements ErrorController {

	@Operation(summary = "统一错误地址", description = "全局统一错误地址")
	@RequestMapping(value = ErrorUtils.ERROR_PATH)
	public Message<String> index(HttpServletRequest request, HttpServletResponse response) {
		return ErrorUtils.message(request, response);
	}

}
