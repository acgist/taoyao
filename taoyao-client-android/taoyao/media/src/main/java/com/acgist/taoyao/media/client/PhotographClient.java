package com.acgist.taoyao.media.client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.util.Log;

import com.acgist.taoyao.boot.utils.DateUtils;

import org.webrtc.VideoFrame;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 拍照终端
 *
 * @author acgist
 */
public class PhotographClient {

    private final int quantity;
    private final String filename;
    private final String filepath;

    public PhotographClient(int quantity, String path) {
        this.quantity = quantity;
        this.filename = DateUtils.format(LocalDateTime.now(), DateUtils.DateTimeStyle.YYYYMMDDHH24MMSS) + ".jpg";
        this.filepath = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), path, this.filename).toString();
        Log.i(RecordClient.class.getSimpleName(), "拍摄照片文件：" + this.filepath);
    }

    public String photograph(VideoFrame videoFrame) {
        final Thread thread = new Thread(() -> this.photographBackground(videoFrame));
        thread.setName("PhotographThread");
        thread.setDaemon(true);
        thread.start();
        return this.filepath;
    }

    private void photographBackground(VideoFrame videoFrame) {
        final File file = new File(this.filepath);
        try (
            final OutputStream output = new FileOutputStream(file);
            final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ) {
            final VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
            final int width = i420.getWidth();
            final int height = i420.getHeight();
            // YuvHelper转换颜色溢出
            final YuvImage image = this.i420ToYuvImage(i420, width, height);
            i420.release();
            videoFrame.release();
            image.compressToJpeg(new Rect(0, 0, width, height), this.quantity, byteArray);
            final byte[] array = byteArray.toByteArray();
            final Bitmap bitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
//          final Matrix matrix = new Matrix();
//          matrix.setRotate(90);
//          final Bitmap matrixBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, this.quantity, output);
        } catch (Exception e) {
            Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
        }
        synchronized (this) {
            this.notifyAll();
        }
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

    private YuvImage i420ToYuvImage(VideoFrame.I420Buffer i420, int width, int height) {
        final ByteBuffer[] yuvPlanes = new ByteBuffer[] {
            i420.getDataY(), i420.getDataU(), i420.getDataV()
        };
        final int[] yuvStrides = new int[] {
            i420.getStrideY(), i420.getStrideU(), i420.getStrideV()
        };
        if (
            yuvStrides[0] != width     ||
            yuvStrides[1] != width / 2 ||
            yuvStrides[2] != width / 2
        ) {
            return i420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }
        final byte[] bytes = new byte[yuvStrides[0] * height + yuvStrides[1] * height / 2 + yuvStrides[2] * height / 2];
        final ByteBuffer yBuffer = ByteBuffer.wrap(bytes, 0, width * height);
        this.copyPlane(yuvPlanes[0], yBuffer);
        final byte[] uvBytes = new byte[width / 2 * height / 2];
        final ByteBuffer uvBuffer = ByteBuffer.wrap(uvBytes, 0, uvBytes.length);
        this.copyPlane(yuvPlanes[2], uvBuffer);
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                bytes[width * height + row * width + col * 2] = uvBytes[row * width / 2 + col];
            }
        }
        this.copyPlane(yuvPlanes[1], uvBuffer);
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                bytes[width * height + row * width + col * 2 + 1] = uvBytes[row * width / 2 + col];
            }
        }
        return new YuvImage(bytes, ImageFormat.NV21, width, height, null);
    }

    private YuvImage i420ToYuvImage(ByteBuffer[] yuvPlanes, int[] yuvStrides, int width, int height) {
        int i = 0;
        final byte[] bytes = new byte[width * height * 3 / 2];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                bytes[i++] = yuvPlanes[0].get(col + row * yuvStrides[0]);
            }
        }
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                bytes[i++] = yuvPlanes[2].get(col + row * yuvStrides[2]);
                bytes[i++] = yuvPlanes[1].get(col + row * yuvStrides[1]);
            }
        }
        return new YuvImage(bytes, ImageFormat.NV21, width, height, null);
    }

    private void copyPlane(ByteBuffer src, ByteBuffer dst) {
        src.position(0).limit(src.capacity());
        dst.put(src);
        dst.position(0).limit(dst.capacity());
    }

}
