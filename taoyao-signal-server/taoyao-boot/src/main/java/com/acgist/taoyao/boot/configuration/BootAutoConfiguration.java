package com.acgist.taoyao.boot.configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
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

import com.acgist.taoyao.boot.config.IdProperties;
import com.acgist.taoyao.boot.config.IpRewriteProperties;
import com.acgist.taoyao.boot.config.MediaProperties;
import com.acgist.taoyao.boot.config.ScriptProperties;
import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.config.SocketProperties;
import com.acgist.taoyao.boot.config.TaoyaoProperties;
import com.acgist.taoyao.boot.config.WebrtcProperties;
import com.acgist.taoyao.boot.controller.TaoyaoControllerAdvice;
import com.acgist.taoyao.boot.controller.TaoyaoErrorController;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.runner.OrderedCommandLineRunner;
import com.acgist.taoyao.boot.service.IdService;
import com.acgist.taoyao.boot.service.impl.IdServiceImpl;
import com.acgist.taoyao.boot.utils.ErrorUtils;
import com.acgist.taoyao.boot.utils.FileUtils;
import com.acgist.taoyao.boot.utils.HTTPUtils;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.boot.utils.NetUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局自动配置
 * 
 * @author acgist
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Import({ TaskExecutionAutoConfiguration.class, TaskSchedulingAutoConfiguration.class })
@EnableAsync
@EnableScheduling
@AutoConfiguration
@EnableAspectJAutoProxy(exposeProxy = false)
@EnableConfigurationProperties({
    IdProperties.class,
    MediaProperties.class,
    ScriptProperties.class,
    SocketProperties.class,
    TaoyaoProperties.class,
    WebrtcProperties.class,
    SecurityProperties.class,
    IpRewriteProperties.class
})
public class BootAutoConfiguration {
    
    private final TaoyaoProperties taoyaoProperties;
    private final ApplicationContext applicationContext;
    
    public BootAutoConfiguration(TaoyaoProperties taoyaoProperties, ApplicationContext applicationContext) {
        this.taoyaoProperties = taoyaoProperties;
        this.applicationContext = applicationContext;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public IdService idService(IdProperties idProperties) {
        return new IdServiceImpl(idProperties);
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
    @ConditionalOnMissingBean
    public TaoyaoErrorController taoyaoErrorController() {
        return new TaoyaoErrorController();
    }

    @Bean
    @ConditionalOnMissingBean
    public TaoyaoControllerAdvice taoyaoControllerAdvice() {
        return new TaoyaoControllerAdvice();
    }
    
    @Bean
    public CommandLineRunner successCommandLineRunner(
        TaskExecutor taskExecutor,
        TaoyaoProperties taoyaoProperties,
        IpRewriteProperties ipRewriteProperties
    ) {
        return new OrderedCommandLineRunner() {
            @Override
            public void run(String ... args) throws Exception {
                NetUtils.init(ipRewriteProperties);
                HTTPUtils.init(taoyaoProperties.getTimeout(), taskExecutor);
                BootAutoConfiguration.this.registerException();
            }
        };
    }

    @PostConstruct
    public void init() {
        final Runtime runtime = Runtime.getRuntime();
        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        final String maxMemory = FileUtils.formatSize(runtime.maxMemory());
        final String freeMemory = FileUtils.formatSize(runtime.freeMemory());
        final String totalMemory = FileUtils.formatSize(runtime.totalMemory());
        log.info("操作系统架构：{}", System.getProperty("os.arch"));
        log.info("操作系统名称：{}", System.getProperty("os.name"));
        log.info("操作系统版本：{}", System.getProperty("os.version"));
        log.info("可用的处理器数量：{}", runtime.availableProcessors());
        log.info("Java版本：{}", System.getProperty("java.version"));
        log.info("Java主目录：{}", System.getProperty("java.home"));
        log.info("Java库目录：{}", System.getProperty("java.library.path"));
        log.info("ClassPath：{}", System.getProperty("java.class.path"));
        log.info("虚拟机名称：{}", System.getProperty("java.vm.name"));
        log.info("虚拟机参数：{}", runtimeMXBean.getInputArguments().stream().collect(Collectors.joining(" ")));
        log.info("虚拟机最大内存：{}", maxMemory);
        log.info("虚拟机空闲内存：{}", freeMemory);
        log.info("虚拟机已用内存：{}", totalMemory);
        log.info("工作目录：{}", System.getProperty("user.dir"));
        log.info("用户目录：{}", System.getProperty("user.home"));
        log.info("临时目录：{}", System.getProperty("java.io.tmpdir"));
        log.info("文件编码：{}", System.getProperty("file.encoding"));
        this.applicationContext.getBeansOfType(TaskExecutor.class).forEach((k, v) -> {
            log.info("系统任务线程池：{} - {}", k, v);
        });
        this.applicationContext.getBeansOfType(TaskScheduler.class).forEach((k, v) -> {
            log.info("系统定时任务线程池：{} - {}", k, v);
        });
    }
    
    /**
     * 异常注册
     */
    public void registerException() {
        ErrorUtils.register(MessageCode.CODE_3400, BindException.class);
        ErrorUtils.register(MessageCode.CODE_3400, ValidationException.class);
        ErrorUtils.register(MessageCode.CODE_3400, TypeMismatchException.class);
        ErrorUtils.register(MessageCode.CODE_3404, NoHandlerFoundException.class);
        ErrorUtils.register(MessageCode.CODE_3503, AsyncRequestTimeoutException.class);
        ErrorUtils.register(MessageCode.CODE_3400, ConstraintViolationException.class);
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
        log.info("系统关闭：{}", this.taoyaoProperties.getName());
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
//              System.exit(0);
                // 强制关机
                Runtime.getRuntime().halt(0);
            }
        }, 5000);
    }

}