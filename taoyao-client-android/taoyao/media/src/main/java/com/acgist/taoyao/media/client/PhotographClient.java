package com.acgist.taoyao.media.client;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.acgist.taoyao.boot.utils.DateUtils;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.VideoSourceType;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拍照终端
 *
 * @author acgist
 */
public class PhotographClient implements VideoSink {

    public static final int CAPTURER_SIZE = 1;

    private final int quantity;
    private final String filename;
    private final String filepath;
    private volatile boolean wait;
    private volatile boolean finish;
    private Surface surface;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;

    public PhotographClient(int quantity, String path) {
        this.quantity = quantity;
        this.filename = DateUtils.format(LocalDateTime.now(), DateUtils.DateTimeStyle.YYYYMMDDHH24MMSS) + ".jpg";
        this.filepath = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), path, this.filename).toString();
        this.wait     = true;
        this.finish   = false;
        Log.i(RecordClient.class.getSimpleName(), "拍摄照片文件：" + this.filepath);
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
        videoFrame.retain();
        this.photograph(videoFrame);
    }

    public String photograph(VideoFrame videoFrame) {
        if(this.wait) {
            this.wait = false;
            final Thread thread = new Thread(() -> this.photographBackground(videoFrame));
            thread.setName("PhotographThread");
            thread.setDaemon(true);
            thread.start();
        } else {
            videoFrame.release();
        }
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
            final Rect rect = new Rect(0, 0, width, height);
            image.compressToJpeg(rect, this.quantity, byteArray);
            final byte[] array = byteArray.toByteArray();
            final Bitmap bitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
//          final Matrix matrix = new Matrix();
//          matrix.setRotate(90);
//          final Bitmap matrixBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, this.quantity, output);
        } catch (Exception e) {
            Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
        }
        this.notifyWait();
    }

    private void notifyWait() {
        synchronized (this) {
            this.finish = true;
            this.notifyAll();
        }
    }

    public String waitForPhotograph() {
        synchronized (this) {
            if(this.finish) {
                return this.filepath;
            }
            try {
                this.wait(5000);
            } catch (InterruptedException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照等待异常", e);
            }
        }
        return this.filepath;
    }

    private YuvImage i420ToYuvImage(VideoFrame.I420Buffer i420, int width, int height) {
        int index = 0;
        final int yy = i420.getStrideY();
        final int uu = i420.getStrideU();
        final int vv = i420.getStrideV();
        final ByteBuffer y = i420.getDataY();
        final ByteBuffer u = i420.getDataU();
        final ByteBuffer v = i420.getDataV();
        final byte[] nv21 = new byte[width * height * 3 / 2];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                nv21[index++] = y.get(col + row * yy);
            }
        }
        final int halfWidth = width / 2;
        final int halfHeight = height / 2;
        for (int row = 0; row < halfHeight; row++) {
            for (int col = 0; col < halfWidth; col++) {
                nv21[index++] = v.get(col + row * vv);
                nv21[index++] = u.get(col + row * uu);
            }
        }
        return new YuvImage(nv21, ImageFormat.NV21, width, height, null);
    }

    @SuppressLint("MissingPermission")
    public String photograph(int width, int height, VideoSourceType type, Context context) {
        final CameraManager cameraManager = context.getSystemService(CameraManager.class);
        this.imageReader   = ImageReader.newInstance(width, height, ImageFormat.JPEG, PhotographClient.CAPTURER_SIZE);
        this.surface       = this.imageReader.getSurface();
        this.imageReader.setOnImageAvailableListener(this.imageAvailableListener, null);
        try {
            String cameraId = null;
            final String[] cameraIdList = cameraManager.getCameraIdList();
            for (String id : cameraIdList) {
                final CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK && type == VideoSourceType.BACK) {
                    cameraId = id;
                    break;
                } else if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT && type == VideoSourceType.FRONT) {
                    cameraId = id;
                    break;
                } else {
                }
            }
            if(cameraId == null) {
                PhotographClient.this.closeCamera();
                return null;
            }
            cameraManager.openCamera(cameraId, this.cameraDeviceStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
            PhotographClient.this.closeCamera();
        }
        return this.filepath;
    }

    private ImageReader.OnImageAvailableListener imageAvailableListener = (ImageReader imageReader) -> {
        final Image image = imageReader.acquireLatestImage();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer byteBuffer = planes[0].getBuffer();
        final byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        final File file = new File(PhotographClient.this.filepath);
        try (
            final OutputStream output = new FileOutputStream(file);
        ) {
            output.write(bytes,0,bytes.length);
        } catch (IOException e) {
            Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
        } finally {
            image.close();
            PhotographClient.this.closeCamera();
        }
    };

    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            PhotographClient.this.cameraDevice = cameraDevice;
            try {
                PhotographClient.this.cameraDevice.createCaptureSession(new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    List.of(new OutputConfiguration(PhotographClient.this.surface)),
                    Runnable::run,
                    PhotographClient.this.cameraCaptureSessionStateCallback
                ));
            } catch (CameraAccessException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
                PhotographClient.this.closeCamera();
            }
        }
        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            PhotographClient.this.closeCamera();
        }
        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            PhotographClient.this.closeCamera();
        }
    };

    private CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            try {
                PhotographClient.this.cameraCaptureSession = cameraCaptureSession;
                final CaptureRequest.Builder builder = PhotographClient.this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(PhotographClient.this.surface);
                cameraCaptureSession.setRepeatingRequest(builder.build(), null, null);
            } catch (CameraAccessException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
                PhotographClient.this.closeCamera();
            }
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            PhotographClient.this.closeCamera();
        }
    };

    private void closeCamera() {
        if(this.cameraCaptureSession != null) {
            this.cameraCaptureSession.close();
            this.cameraCaptureSession = null;
        }
        if(this.cameraDevice != null) {
            this.cameraDevice.close();
            this.cameraDevice = null;
        }
        // 最后释放ImageReader
        if(this.surface != null) {
            this.surface.release();
            this.surface = null;
        }
        if(this.imageReader != null) {
            this.imageReader.close();
            this.imageReader = null;
        }
    }

}
