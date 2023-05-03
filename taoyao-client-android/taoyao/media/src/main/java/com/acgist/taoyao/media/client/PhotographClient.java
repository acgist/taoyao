package com.acgist.taoyao.media.client;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.acgist.taoyao.boot.utils.DateUtils;
import com.acgist.taoyao.media.VideoSourceType;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

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

    private final int quantity;
    private final String filename;
    private final String filepath;
    private volatile boolean done;
    private volatile boolean finish;
    private Surface surface;
    private VideoTrack videoTrack;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private HandlerThread handlerThread;
    private CameraCaptureSession cameraCaptureSession;

    public PhotographClient(int quantity, String path) {
        this.quantity = quantity;
        this.filename = DateUtils.format(LocalDateTime.now(), DateUtils.DateTimeStyle.YYYYMMDDHH24MMSS) + ".jpg";
        this.filepath = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath(), path, this.filename).toString();
        this.done     = false;
        this.finish   = false;
        Log.i(RecordClient.class.getSimpleName(), "拍摄照片文件：" + this.filepath);
    }

    private void notifyWait() {
        synchronized (this) {
            this.finish = true;
            this.notifyAll();
        }
    }

    public String waitForPhotograph() {
        synchronized (this) {
            try {
                if(this.finish) {
                    return this.filepath;
                }
                this.wait(5000);
            } catch (InterruptedException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照等待异常", e);
            } finally {
                this.closeVideoTrack();
                this.closeCamera();
                this.handlerThread.quitSafely();
            }
        }
        return this.filepath;
    }

    public void photograph(VideoTrack videoTrack) {
        videoTrack.setEnabled(true);
        videoTrack.addSink(this);
        this.videoTrack = videoTrack;
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
        if(this.done) {
            // 已经完成忽略
        } else {
            this.done = true;
            this.handlerThread = new HandlerThread("PhotographThread");
            this.handlerThread.start();
            final Handler handler = new Handler(this.handlerThread.getLooper());
            videoFrame.retain();
            handler.post(() -> this.photograph(videoFrame));
        }
    }

    private void photograph(VideoFrame videoFrame) {
        final VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
        videoFrame.release();
        final File file = new File(this.filepath);
        final int width = i420.getWidth();
        final int height = i420.getHeight();
        // YuvHelper转换颜色溢出
        final YuvImage image = this.i420ToYuvImage(i420, width, height);
        i420.release();
        final Rect rect = new Rect(0, 0, width, height);
        try (
            final OutputStream output = new FileOutputStream(file);
            final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ) {
            image.compressToJpeg(rect, this.quantity, byteArray);
            final byte[] array = byteArray.toByteArray();
            final Bitmap bitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
//          final Matrix matrix = new Matrix();
//          matrix.setRotate(90);
//          final Bitmap matrixBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, this.quantity, output);
            bitmap.recycle();
        } catch (Exception e) {
            Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
        } finally {
            this.notifyWait();
        }
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

    private void closeVideoTrack() {
        if(this.videoTrack != null) {
            this.videoTrack.removeSink(this);
            this.videoTrack.dispose();
            this.videoTrack = null;
        }
    }

    @SuppressLint("MissingPermission")
    public void photograph(int width, int height, int fps, VideoSourceType videoSourceType, Context context) {
        this.handlerThread = new HandlerThread("PhotographThread");
        this.handlerThread.start();
        final Handler handler = new Handler(this.handlerThread.getLooper());
        handler.post(() -> {
            final CameraManager cameraManager = context.getSystemService(CameraManager.class);
            PhotographClient.this.imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, fps);
            PhotographClient.this.surface     = PhotographClient.this.imageReader.getSurface();
            try {
                String cameraId = null;
                final String[] cameraIdList = cameraManager.getCameraIdList();
                for (String id : cameraIdList) {
                    final CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
                    if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK && videoSourceType == VideoSourceType.BACK) {
                        cameraId = id;
                        break;
                    } else if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT && videoSourceType == VideoSourceType.FRONT) {
                        cameraId = id;
                        break;
                    } else {
                        // TODO：截屏
                    }
                }
                cameraManager.openCamera(cameraId, this.cameraDeviceStateCallback, null);
            } catch (CameraAccessException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
            }
        });
    }

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
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
        }

    };

    private CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            try {
                PhotographClient.this.cameraCaptureSession = cameraCaptureSession;
                final CaptureRequest.Builder builder = PhotographClient.this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.set(CaptureRequest.JPEG_QUALITY, (byte) PhotographClient.this.quantity);
//              builder.set(CaptureRequest.JPEG_ORIENTATION, 90);
                builder.addTarget(PhotographClient.this.surface);
                cameraCaptureSession.setRepeatingRequest(builder.build(), PhotographClient.this.cameraCaptureSessionCaptureCallback, null);
            } catch (CameraAccessException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }

    };

    private CameraCaptureSession.CaptureCallback cameraCaptureSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private volatile int index = 0;

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            final Image image = PhotographClient.this.imageReader.acquireNextImage();
            if(image == null) {
                return;
            }
            if(this.index++ <= 4 || PhotographClient.this.done) {
                image.close();
                return;
            }
            PhotographClient.this.done = true;
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer byteBuffer = planes[0].getBuffer();
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            final File file = new File(PhotographClient.this.filepath);
            try (final OutputStream output = new FileOutputStream(file)) {
//              final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//              bitmap.compress(Bitmap.CompressFormat.JPEG, PhotographClient.this.quantity, output);
                output.write(bytes, 0, bytes.length);
                cameraCaptureSession.stopRepeating();
            } catch (IOException | CameraAccessException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
            } finally {
                image.close();
                PhotographClient.this.notifyWait();
            }
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
        if(this.imageReader != null) {
            this.imageReader.close();
            this.imageReader = null;
        }
    }

}
