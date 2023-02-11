package com.acgist.taoyao.boot.config;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.acgist.taoyao.boot.controller.TaoyaoControllerAdvice;
import com.acgist.taoyao.boot.controller.TaoyaoErrorController;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.property.IdProperties;
import com.acgist.taoyao.boot.property.MediaProperties;
import com.acgist.taoyao.boot.property.ScriptProperties;
import com.acgist.taoyao.boot.property.SecurityProperties;
import com.acgist.taoyao.boot.property.TaoyaoProperties;
import com.acgist.taoyao.boot.property.WebrtcProperties;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.boot.service.impl.IdServiceImpl;
import com.acgist.taoyao.boot.utils.ErrorUtils;
import com.acgist.taoyao.boot.utils.FileUtils;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局配置
 * 
 * @author acgist
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Import({ TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class })
@EnableAsync
@Configuration
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = false)
@EnableConfigurationProperties({
	IdProperties.class,
	MediaProperties.class,
	ScriptProperties.class,
	TaoyaoProperties.class,
	WebrtcProperties.class,
	SecurityProperties.class
})
public class BootAutoConfiguration {

	@Value("${spring.application.name:taoyao}")
	private String name;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Bean
	@ConditionalOnMissingBean
	public IdService idService() {
		return new IdServiceImpl();
	}
	
	@Bean
	@Primary
	@ConditionalOnMissingBean
	public ObjectMapper objectMapper() {
		return JSONUtils.buildMapper();
	}
	
	@Bean
	@Primary
	@ConditionalOnMissingBean
	public TaskExecutor taskExecutor(TaskExecutorBuilder builder) {
		return builder.build();
	}
	
	@Bean
	@Primary
	@ConditionalOnMissingBean
	public TaskScheduler taskScheduler(TaskSchedulerBuilder builder) {
		return builder.build();
	}
	
	@Bean
	public CommandLineRunner successCommandLineRunner() {
		return new CommandLineRunner() {
			@Override
			public void run(String ... args) throws Exception {
				log.info("项目启动成功：{}", BootAutoConfiguration.this.name);
			}
		};
	}
	
	@Bean
	@ConditionalOnMissingBean
	public TaoyaoErrorController taoyaoErrorController() {
		return new TaoyaoErrorController();
	}

	@Bean
	@ConditionalOnMissingBean
	public TaoyaoControllerAdvice taoyaoControllerAdvice() {
		return new TaoyaoControllerAdvice();
	}

	@PostConstruct
	public void init() {
		final Runtime runtime = Runtime.getRuntime();
		final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		final String freeMemory = FileUtils.formatSize(runtime.freeMemory());
		final String totalMemory = FileUtils.formatSize(runtime.totalMemory());
		final String maxMemory = FileUtils.formatSize(runtime.maxMemory());
		log.info("操作系统名称：{}", System.getProperty("os.name"));
		log.info("操作系统架构：{}", System.getProperty("os.arch"));
		log.info("操作系统版本：{}", System.getProperty("os.version"));
		log.info("可用的处理器数量：{}", runtime.availableProcessors());
		log.info("Java版本：{}", System.getProperty("java.version"));
		log.info("Java主目录：{}", System.getProperty("java.home"));
		log.info("Java库目录：{}", System.getProperty("java.library.path"));
		log.info("ClassPath：{}", System.getProperty("java.class.path"));
		log.info("虚拟机名称：{}", System.getProperty("java.vm.name"));
		log.info("虚拟机参数：{}", runtimeMXBean.getInputArguments().stream().collect(Collectors.joining(" ")));
		log.info("虚拟机空闲内存：{}", freeMemory);
		log.info("虚拟机已用内存：{}", totalMemory);
		log.info("虚拟机最大内存：{}", maxMemory);
		log.info("工作目录：{}", System.getProperty("user.dir"));
		log.info("用户目录：{}", System.getProperty("user.home"));
		log.info("临时目录：{}", System.getProperty("java.io.tmpdir"));
		log.info("文件编码：{}", System.getProperty("file.encoding"));
		this.applicationContext.getBeansOfType(TaskExecutor.class).forEach((k, v) -> {
			log.info("系统任务线程池：{}-{}", k, v);
		});
		this.applicationContext.getBeansOfType(TaskScheduler.class).forEach((k, v) -> {
			log.info("系统定时任务线程池：{}-{}", k, v);
		});
		this.registerException();
	}
	
	/**
	 * 异常注册
	 */
	public void registerException() {
		ErrorUtils.register(MessageCode.CODE_3400, BindException.class);
		ErrorUtils.register(MessageCode.CODE_3400, TypeMismatchException.class);
		ErrorUtils.register(MessageCode.CODE_3404, NoHandlerFoundException.class);
		ErrorUtils.register(MessageCode.CODE_3503, AsyncRequestTimeoutException.class);
		ErrorUtils.register(MessageCode.CODE_3500, MissingPathVariableException.class);
		ErrorUtils.register(MessageCode.CODE_3400, ServletRequestBindingException.class);
		ErrorUtils.register(MessageCode.CODE_3400, HttpMessageNotReadableException.class);
		ErrorUtils.register(MessageCode.CODE_3400, MethodArgumentNotValidException.class);
		ErrorUtils.register(MessageCode.CODE_3500, ConversionNotSupportedException.class);
		ErrorUtils.register(MessageCode.CODE_3500, HttpMessageNotWritableException.class);
		ErrorUtils.register(MessageCode.CODE_3400, MissingServletRequestPartException.class);
		ErrorUtils.register(MessageCode.CODE_3415, HttpMediaTypeNotSupportedException.class);
		ErrorUtils.register(MessageCode.CODE_3406, HttpMediaTypeNotAcceptableException.class);
		ErrorUtils.register(MessageCode.CODE_3405, HttpRequestMethodNotSupportedException.class);
		ErrorUtils.register(MessageCode.CODE_3400, MissingServletRequestParameterException.class);
	}
	
	@PreDestroy
	public void destroy() {
		log.info("系统关闭：{}", this.name);
		// 刷出日志缓存
		final ILoggerFactory factory = LoggerFactory.getILoggerFactory();
		if (factory instanceof LoggerContext context) {
			context.stop();
		}
		// 定时强制关机
		final Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// 强制关机：无效
//				System.exit(0);
				// 强制关机
				Runtime.getRuntime().halt(0);
			}
		}, 5000);
	}

}