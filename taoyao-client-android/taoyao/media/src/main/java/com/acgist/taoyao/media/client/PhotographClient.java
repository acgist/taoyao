package com.acgist.taoyao.media.client;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.acgist.taoyao.boot.utils.DateUtils;

import org.webrtc.VideoFrame;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 拍照终端
 *
 * @author acgist
 */
public class PhotographClient {

    private final String filename;
    private final String filepath;
    private VideoFrame videoFrame;

    public PhotographClient(String path) {
        this.filename = DateUtils.format(LocalDateTime.now(), DateUtils.DateTimeStyle.YYYYMMDDHH24MMSS) + ".mp4";
        final Path filePath   = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), path, filename);
        final File parentFile = filePath.getParent().toFile();
        if(!parentFile.exists()) {
            parentFile.mkdirs();
        }
        this.filepath = filePath.toString();
        Log.i(RecordClient.class.getSimpleName(), "拍摄照片文件：" + this.filepath);
    }

    public String photograph(VideoFrame videoFrame) {
        synchronized (this) {
            this.notifyAll();
        }
        return this.filepath;
    }

    public String waitForPhotograph() {
        synchronized (this) {
            try {
                this.wait(5000);
            } catch (InterruptedException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照等待异常", e);
            }
        }
        return this.filepath;
    }

}
