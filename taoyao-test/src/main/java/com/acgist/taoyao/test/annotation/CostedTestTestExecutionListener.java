package com.acgist.taoyao.test.annotation;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import lombok.extern.slf4j.Slf4j;

/**
 * 多线程测试监听
 * 
 * @author acgist
 */
@Slf4j
public class CostedTestTestExecutionListener implements TestExecutionListener {
	
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		final CostedTest costedTest = testContext.getTestMethod().getDeclaredAnnotation(CostedTest.class);
		if(costedTest == null) {
			return;
		}
		final int count  = costedTest.count();
		final int thread  = costedTest.thread();
		final long timeout = costedTest.timeout();
		final TimeUnit timeUnit = costedTest.timeUnit();
		final long aTime = System.currentTimeMillis();
		if(thread == 1) {
			for (int index = 0; index < count; index++) {
				testContext.getTestMethod().invoke(testContext.getTestInstance());
			}
		} else {
			final CountDownLatch countDownLatch = new CountDownLatch(count);
			final ExecutorService executor = Executors.newFixedThreadPool(thread);
			for (int index = 0; index < count; index++) {
				executor.execute(() -> {
					try {
						testContext.getTestMethod().invoke(testContext.getTestInstance());
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.error("多线程测试异常", e);
					} finally {
						countDownLatch.countDown();
					}
				});
			}
			countDownLatch.await(timeout, timeUnit);
		}
		final long zTime = System.currentTimeMillis();
		final long costed = zTime - aTime;
		log.info("多线程测试消耗时间：{}", costed);
	}
	
}
