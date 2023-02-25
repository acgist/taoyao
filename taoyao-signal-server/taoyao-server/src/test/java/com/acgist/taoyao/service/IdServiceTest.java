package com.acgist.taoyao.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.acgist.taoyao.annotation.CostedTest;
import com.acgist.taoyao.annotation.TaoyaoTest;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.main.TaoyaoApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TaoyaoTest(classes = TaoyaoApplication.class)
//@SpringBootTest(classes = TaoyaoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class IdServiceTest {

	@Autowired
	private IdService idService;
	
	@Test
//	@Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
//	@Rollback()
//	@RepeatedTest(10)
	void testId() {
		final long id = this.idService.buildId();
		log.info("生成ID：{}", id);
		log.info("生成ID：{}", String.valueOf(id).length());
	}
	
	@Test
	@CostedTest(count = 100000, thread = 10)
	void testIdCosted() {
		this.idService.buildId();
	}
	
}
