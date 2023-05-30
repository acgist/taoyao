package com.acgist.taoyao.signal.party.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.boot.utils.FileUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 媒体录像
 * 
 * @author acgist
 */
@Slf4j
@Getter
@Setter
public class Recorder {

    /**
     * 是否关闭
     */
    private boolean close;
    /**
     * 是否正在运行
     */
    private boolean running;
    /**
     * 音频端口
     */
    private Integer audioPort;
    /**
     * 视频端口
     */
    private Integer videoPort;
    /**
     * 传输通道
     */
    private Transport transport;
    /**
     * 音频消费者
     */
    private Consumer audioConsumer;
    /**
     * 视频消费者
     */
    private Consumer videoConsumer;
    /**
     * 录像进程
     */
    private Process process;
    /**
     * 进程Builder
     */
    private ProcessBuilder processBuilder;
    /**
     * 录制线程
     */
    private Thread thread;
    /**
     * 日志线程
     */
    private Thread inputThread;
    /**
     * 异常线程
     */
    private Thread errorThread;
    /**
     * 命令
     */
    private String command;
    /**
     * 文件路径
     */
    private final String folder;
    /**
     * SDP路径
     */
    private final String sdpfile;
    /**
     * 文件路径
     */
    private final String filepath;
    /**
     * FFmpeg配置
     */
    private final FfmpegProperties ffmpegProperties;

    public Recorder(FfmpegProperties ffmpegProperties) {
        this.close            = false;
        this.running          = false;
        this.ffmpegProperties = ffmpegProperties;
        final String id = UUID.randomUUID().toString();
        this.folder     = Paths.get(ffmpegProperties.getStorageVideoPath(), id).toAbsolutePath().toString();
        this.sdpfile    = Paths.get(this.folder, "taoyao.sdp").toAbsolutePath().toString();
        this.filepath   = Paths.get(this.folder, "taoyao.mp4").toAbsolutePath().toString();
        this.command    = String.format(this.ffmpegProperties.getRecord(), this.sdpfile, this.filepath);
        FileUtils.mkdirs(this.folder);
    }
    
    /**
     * 开始录像
     */
    public void start() {
        synchronized (this) {
            if(this.running) {
                return;
            }
            this.running = true;
            this.thread = new Thread(this::record);
            this.thread.setDaemon(true);
            this.thread.setName("TaoyaoRecord");
            this.thread.start();
        }
    }
    
    /**
     * 录制视频
     */
    private void record() {
        this.buildSdpfile();
        int status = 0;
        final StringBuilder input = new StringBuilder();
        final StringBuilder error = new StringBuilder();
        try {
            final boolean linux = FileUtils.linux();
            if(linux) {
                this.processBuilder = new ProcessBuilder("/bin/bash", "-c", this.command);
                this.process = processBuilder.start();
            } else {
                this.processBuilder = new ProcessBuilder("cmd", "/c", this.command);
                this.process = processBuilder.start();
            }
            log.debug("""
                开始录像：{}
                录像命令：{}
                """, this.filepath, this.command);
            this.inputThread = new Thread(() -> {
                try (final InputStream inputStream = this.process.getInputStream()) {
                    int length;
                    final byte[] bytes = new byte[1024];
                    while(this.running && !this.close && (length = inputStream.read(bytes)) >= 0) {
                        input.append(linux ? new String(bytes, 0, length) : new String(bytes, 0, length, "GBK"));
                    }
                } catch (Exception e) {
                    log.error("读取录像日志异常", e);
                }
            });
            this.inputThread.setDaemon(true);
            this.inputThread.setName("TaoyaoRecordInput");
            this.inputThread.start();
            this.errorThread = new Thread(() -> {
                try (final InputStream inputStream = this.process.getErrorStream();) {
                    int length;
                    final byte[] bytes = new byte[1024];
                    while(this.running && !this.close && (length = inputStream.read(bytes)) >= 0) {
                        error.append(linux ? new String(bytes, 0, length) : new String(bytes, 0, length, "GBK"));
                    }
                } catch (Exception e) {
                    log.error("读取录像错误异常", e);
                }
            });
            this.errorThread.setDaemon(true);
            this.errorThread.setName("TaoyaoRecordError");
            this.errorThread.start();
            status = this.process.waitFor();
        } catch (Exception e) {
            log.error("录像异常：{}", this.command, e);
        } finally {
            this.stop();
        }
        log.debug("""
            结束录像：{}
            结束状态：{}
            录像日志：{}
            异常日志：{}
            """, this.filepath, status, input, error);
    }
    
    /**
     * 创建SDP文件
     */
    private void buildSdpfile() {
        try {
            Files.write(
                Paths.get(this.sdpfile),
                String.format(this.ffmpegProperties.getSdp(), 8888, 9999).getBytes(),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            log.error("创建SDP文件异常：{}", this.sdpfile, e);
        }
    }
    
    /**
     * 结束录像
     */
    public void stop() {
        synchronized (this) {
            if(this.close) {
                return;
            }
            this.close = true;
        }
        if(this.process == null) {
            return;
        }
        log.debug("结束媒体录像：{}", this.filepath);
        // 所有子进程
        this.process.children().forEach(process -> {
            process.destroy();
        });
        // 当前父进程
        this.process.destroy();
    }

}
