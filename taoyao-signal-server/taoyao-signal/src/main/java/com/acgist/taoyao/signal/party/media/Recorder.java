package com.acgist.taoyao.signal.party.media;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.boot.utils.FileUtils;
import com.acgist.taoyao.boot.utils.NetUtils;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.boot.utils.ScriptUtils.ScriptExecutor;

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
     * 音频流ID
     */
    private String audioStreamId;
    /**
     * 音频生产者ID
     */
    private String audioProducerId;
    /**
     * 音频消费者ID
     */
    private String audioConsumerId;
    /**
     * 音频通道ID
     */
    private String audioTransportId;
    /**
     * 视频流ID
     */
    private String videoStreamId;
    /**
     * 视频生产者ID
     */
    private String videoProducerId;
    /**
     * 视频消费者ID
     */
    private String videoConsumerId;
    /**
     * 视频通道ID
     */
    private String videoTransportId;
    /**
     * 录制线程
     */
    private Thread thread;
    /**
     * 视频时长
     */
    private Double duration;
    /**
     * 命令执行器
     */
    private ScriptExecutor scriptExecutor;
    /**
     * 文件路径
     */
    private final String folder;
    /**
     * SDP路径
     */
    private final String sdpfile;
    /**
     * 预览图片
     */
    private final String preview;
    /**
     * 文件路径
     */
    private final String filepath;
    /**
     * FFmpeg配置
     */
    private final FfmpegProperties ffmpegProperties;

    /**
     * @param name             录像名称
     * @param ffmpegProperties FFmpeg配置
     */
    public Recorder(String name, FfmpegProperties ffmpegProperties) {
        this.close            = false;
        this.running          = false;
        this.ffmpegProperties = ffmpegProperties;
        this.folder           = Paths.get(ffmpegProperties.getStorageVideoPath(), name).toAbsolutePath().toString();
        this.sdpfile          = Paths.get(this.folder, "taoyao.sdp").toAbsolutePath().toString();
        this.preview          = Paths.get(this.folder, "taoyao.jpg").toAbsolutePath().toString();
        this.filepath         = Paths.get(this.folder, "taoyao.mp4").toAbsolutePath().toString();
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
        }
        this.thread = new Thread(this::record);
        this.thread.setDaemon(true);
        this.thread.setName("TaoyaoRecord");
        this.thread.start();
    }
    
    /**
     * 录制视频
     */
    private void record() {
        this.buildSdpfile();
        final String recordScript = String.format(this.ffmpegProperties.getRecord(), this.sdpfile, this.filepath);
        this.scriptExecutor = new ScriptExecutor(recordScript);
        try {
            log.debug("""
                开始录像：{}
                录像端口：{} - {}
                """, this.folder, this.audioPort, this.videoPort);
            this.scriptExecutor.execute();
        } catch (Exception e) {
            log.error("录像异常：{}", recordScript, e);
        } finally {
            this.stop();
        }
    }
    
    /**
     * 创建SDP文件
     */
    private void buildSdpfile() {
        try {
            this.audioPort = NetUtils.scanPort(this.ffmpegProperties.getMinPort(), this.ffmpegProperties.getMaxPort());
            // 预留控制端口
            this.videoPort = NetUtils.scanPort(this.audioPort + 16, this.ffmpegProperties.getMaxPort());
            final String sdp = String.format(
                this.ffmpegProperties.getSdp(),
                this.ffmpegProperties.getHost(),
                this.audioPort,
                this.ffmpegProperties.getHost(),
                this.videoPort,
                this.ffmpegProperties.getHost()
            );
            Files.write(
                Paths.get(this.sdpfile),
                sdp.getBytes(),
                StandardOpenOption.WRITE, StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            log.error("创建SDP文件异常：{}", this.sdpfile, e);
        }
    }
    
    /**
     * 视频预览截图
     */
    private void preview() {
        int time = 2;
        final File file = Paths.get(this.preview).toFile();
        while(time > 0 && !(file.exists() && file.length() > 0L)) {
            log.debug("视频预览截图：{}", this.preview);
            final String previewScript = String.format(this.ffmpegProperties.getPreview(), this.filepath, time, this.preview);
            ScriptUtils.execute(previewScript);
            time /= 2;
        }
    }

    /**
     * 视频时长
     */
    private void duration() {
        log.debug("视频时长：{}", this.filepath);
        final String durationScript = String.format(this.ffmpegProperties.getDuration(), this.filepath);
        final ScriptExecutor executor = ScriptUtils.execute(durationScript);
        final Pattern pattern = Pattern.compile(".*duration\\=([0-9\\.]+).*");
        final Matcher matcher = pattern.matcher(executor.getResult());
        String duration = null;
        if(matcher.find()) {
            duration = matcher.group(matcher.groupCount()).strip();
        }
        if(NumberUtils.isCreatable(duration)) {
            this.duration = Double.parseDouble(duration);
        } else {
            this.duration = 0D;
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
        if(this.scriptExecutor == null) {
            return;
        }
        log.debug("结束媒体录像：{}", this.folder);
        this.scriptExecutor.stop("q");
        this.preview();
        this.duration();
    }

}
