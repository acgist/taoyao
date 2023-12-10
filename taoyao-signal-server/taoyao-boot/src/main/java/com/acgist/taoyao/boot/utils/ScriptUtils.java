package com.acgist.taoyao.boot.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 命令工具
 * 
 * @author acgist
 */
@Slf4j
public final class ScriptUtils {
    
    private ScriptUtils() {
    }

    /**
     * 执行命令
     * 
     * @param args 命令参数
     * 
     * @return 执行结果
     */
    public static final ScriptExecutor execute(String ... args) {
        return ScriptUtils.execute(String.join(" ", args));
    }

    /**
     * 执行命令
     * 
     * @param script 命令
     * 
     * @return 执行结果
     */
    public static final ScriptExecutor execute(String script) {
        if(StringUtils.isEmpty(script)) {
            throw MessageCodeException.of(MessageCode.CODE_1002, "无效命令：" + script);
        }
        final ScriptExecutor executor = new ScriptExecutor(script);
        try {
            executor.execute();
        } catch (Exception e) {
            log.error("执行命令异常：{}", script, e);
        }
        return executor;
    }

    /**
     * 命令执行器
     * 
     * @author acgist
     */
    @Getter
    @Setter
    public static final class ScriptExecutor {
        
        /**
         * 执行结果
         */
        private int code;
        /**
         * 是否正在运行
         */
        private boolean running;
        /**
         * 命令进程
         */
        private Process process;
        /**
         * 命令进程Builder
         */
        private ProcessBuilder processBuilder;
        /**
         * 执行命令
         */
        private final String script;
        /**
         * 日志输出
         */
        private final StringBuilder input;
        /**
         * 错误输出
         */
        private final StringBuilder error;
        
        /**
         * @param script 执行命令
         */
        public ScriptExecutor(String script) {
            this.script = script;
            this.input  = new StringBuilder();
            this.error  = new StringBuilder();
        }

        /**
         * 执行命令
         * 
         * @throws IOException          IO异常
         * @throws InterruptedException 线程异常
         */
        public void execute() throws IOException, InterruptedException {
            final boolean linux = FileUtils.linux();
            if(linux) {
                this.processBuilder = new ProcessBuilder("/bin/bash", "-c", this.script);
                this.process = this.processBuilder.start();
            } else {
                this.processBuilder = new ProcessBuilder("cmd", "/c", this.script);
                this.process = this.processBuilder.start();
            }
            log.debug("开始执行命令：{}", this.script);
            this.running = true;
            final CountDownLatch latch = new CountDownLatch(2);
            try (
                final InputStream input = this.process.getInputStream();
                final InputStream error = this.process.getErrorStream();
            ) {
                this.streamThread(linux, "TaoyaoScriptInput", this.input, input, latch);
                this.streamThread(linux, "TaoyaoScriptError", this.error, error, latch);
                this.code = this.process.waitFor();
                latch.await();
            }
            this.running = false;
            log.debug("""
                结束执行命令：{}
                执行状态：{}
                执行日志：{}
                错误日志：{}
                """, this.script, this.code, this.input, this.error);
        }
        
        /**
         * @param linux   是否Linux
         * @param name    线程名称
         * @param builder 日志记录
         * @param input   日志输入流
         * @param latch   计数器
         * 
         * @return 线程
         */
        private Thread streamThread(boolean linux, String name, StringBuilder builder, InputStream input, CountDownLatch latch) {
            final Thread streamThread = new Thread(() -> {
                try {
                    int length;
                    final byte[] bytes = new byte[1024];
                    while(this.running && (length = input.read(bytes)) >= 0) {
                        builder.append(linux ? new String(bytes, 0, length) : new String(bytes, 0, length, "GBK"));
                    }
                } catch (Exception e) {
                    log.error("读取执行命令日志异常", e);
                } finally {
                    latch.countDown();
                }
            });
            streamThread.setName(name);
            streamThread.setDaemon(true);
            streamThread.start();
            return streamThread;
        }
        
        /**
         * 结束命令
         */
        public void stop() {
            this.stop(null);
        }
        
        /**
         * 结束命令
         * 
         * @param script 结束命令
         */
        public void stop(String script) {
            // 等待时间
            long wait = 0;
            // 使用按键结束
            if(StringUtils.isNotEmpty(script)) {
                try (final OutputStream output = this.process.getOutputStream();) {
                    output.write(script.getBytes());
                } catch (Exception e) {
                    log.error("结束命令异常：{}", this.script, e);
                }
                wait = 5000;
            }
            // 等待正常结束
            while(this.process.isAlive() && wait > 0) {
                wait -= 10;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.yield();
                }
            }
            if(this.process.isAlive()) {
                log.info("强制结束命令：{}", this.script);
                // 所有子进程
                this.process.children().forEach(this::destroy);
                // 所有派生进程
                this.process.descendants().forEach(this::destroy);
                // 当前父进程
                this.process.destroy();
//              this.process.destroyForcibly();
            } else {
                log.debug("正常结束命令：{}", this.script);
            }
        }
        
        /**
         * @return 执行结果
         */
        public String getResult() {
            if(this.code == 0) {
                return this.input.toString();
            } else {
                return this.error.toString();
            }
        }

        /**
         * 销毁线程
         * 
         * @param handle 线程
         */
        private void destroy(ProcessHandle handle) {
            try {
                handle.destroy();
//              handle.destroyForcibly();
            } catch (Exception e) {
                log.error("销毁线程异常", e);
            }
        }
        
    }
    
}
