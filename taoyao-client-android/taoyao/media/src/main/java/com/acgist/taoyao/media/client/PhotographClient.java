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

import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
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
 * 没有拉流时使用Camera2拍照
 * 拉流时使用WebRTC帧数据拍照
 *
 * @author acgist
 */
public class PhotographClient implements VideoSink {

    /**
     * 图片质量
     */
    private final int quantity;
    /**
     * 图片名称
     */
    private final String filename;
    /**
     * 图片路径
     */
    private final String filepath;
    /**
     * 是否完成
     */
    private volatile boolean finish;
    /**
     * 是否采集到了图片数据
     */
    private volatile boolean hasImage;
    /**
     * Camera2拍照Surface
     */
    private Surface surface;
    /**
     * WebRTC VideoTrack
     */
    private VideoTrack videoTrack;
    /**
     * Camera2拍照图片处理
     */
    private ImageReader imageReader;
    /**
     * Camera2设备
     */
    private CameraDevice cameraDevice;
    /**
     * 拍照线程
     */
    private HandlerThread handlerThread;
    /**
     * Camera2图片采集线程
     */
    private CameraCaptureSession cameraCaptureSession;

    /**
     * @param quantity 图片质量
     * @param path     图片路径
     */
    public PhotographClient(int quantity, String path) {
        this.quantity = quantity;
        this.filename = DateUtils.format(LocalDateTime.now(), DateUtils.DateTimeStyle.YYYYMMDDHH24MMSS) + ".jpg";
        this.filepath = Paths.get(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(), path, this.filename).toString();
        this.finish   = false;
        this.hasImage = false;
        Log.i(RecordClient.class.getSimpleName(), "拍摄照片文件：" + this.filepath);
    }

    /**
     * 唤醒等待现场
     */
    private void notifyWait() {
        synchronized (this) {
            this.finish = true;
            this.notifyAll();
        }
    }

    /**
     * 等待拍照完成
     *
     * @return 图片路径
     */
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

    /**
     * WebRTC拍照
     *
     * @param videoSource           视频来源
     * @param peerConnectionFactory PeerConnectionFactory
     */
    public void photograph(VideoSource videoSource, PeerConnectionFactory peerConnectionFactory) {
        this.videoTrack = peerConnectionFactory.createVideoTrack("TaoyaoVP", videoSource);
        this.videoTrack.setEnabled(true);
        this.videoTrack.addSink(this);
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
        if(this.hasImage) {
            // 已经完成忽略
        } else {
            synchronized(this) {
                if(this.hasImage) {
                    return;
                }
                this.hasImage      = true;
            }
            this.handlerThread = new HandlerThread("PhotographThread");
            this.handlerThread.start();
            final Handler handler = new Handler(this.handlerThread.getLooper());
            videoFrame.retain();
            handler.post(() -> this.photograph(videoFrame));
        }
    }

    /**
     * WebRTC拍照
     *
     * @param videoFrame 视频帧
     */
    private void photograph(VideoFrame videoFrame) {
        final VideoFrame.I420Buffer i420 = videoFrame.getBuffer().toI420();
        videoFrame.release();
        final File file  = new File(this.filepath);
        final int width  = i420.getWidth();
        final int height = i420.getHeight();
        // YuvHelper转换颜色溢出
        final YuvImage image = this.i420ToYuvImage(i420, width, height);
        i420.release();
        final Rect rect = new Rect(0, 0, width, height);
        try (
            final OutputStream output             = new FileOutputStream(file);
            final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        ) {
            image.compressToJpeg(rect, this.quantity, byteArray);
            final byte[] array  = byteArray.toByteArray();
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

    /**
     * @param i420   I420帧数据
     * @param width  图片宽度
     * @param height 图片高度
     *
     * @return YuvImage
     */
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
        final int halfWidth  = width / 2;
        final int halfHeight = height / 2;
        for (int row = 0; row < halfHeight; row++) {
            for (int col = 0; col < halfWidth; col++) {
                nv21[index++] = v.get(col + row * vv);
                nv21[index++] = u.get(col + row * uu);
            }
        }
        return new YuvImage(nv21, ImageFormat.NV21, width, height, null);
    }

    /**
     * 关闭VideoTrack
     */
    private void closeVideoTrack() {
        if(this.videoTrack != null) {
            this.videoTrack.removeSink(this);
            this.videoTrack.dispose();
            this.videoTrack = null;
        }
    }

    /**
     * Camera2拍照
     *
     * @param width           图片宽度
     * @param height          图片高度
     * @param fps             帧率
     * @param videoSourceType 图片来源
     * @param context         上下文
     */
    @SuppressLint("MissingPermission")
    public void photograph(int width, int height, int fps, VideoSourceType videoSourceType, Context context) {
        if(this.handlerThread != null) {
            return;
        }
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
                    final int lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                    if(
                        lensFacing      == CameraCharacteristics.LENS_FACING_BACK &&
                        videoSourceType == VideoSourceType.BACK
                    ) {
                        cameraId = id;
                        break;
                    } else if(
                        lensFacing      == CameraCharacteristics.LENS_FACING_FRONT &&
                        videoSourceType == VideoSourceType.FRONT
                    ) {
                        cameraId = id;
                        break;
                    } else {
                        // 其他情况：文件、截屏
                    }
                }
                if(cameraId == null) {
                    Log.e(PhotographClient.class.getSimpleName(), "拍照失败没有适配：" + videoSourceType);
                    PhotographClient.this.notifyWait();
                    return;
                }
                cameraManager.openCamera(cameraId, this.cameraDeviceStateCallback, null);
            } catch (CameraAccessException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
            }
        });
    }

    /**
     * Camera2设备回调
     */
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

    /**
     * Camera2会话回调
     */
    private CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            try {
                PhotographClient.this.cameraCaptureSession = cameraCaptureSession;
                final CaptureRequest.Builder builder       = PhotographClient.this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.set(CaptureRequest.JPEG_QUALITY, (byte) PhotographClient.this.quantity);
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

    /**
     * Camera2捕获回调
     */
    private CameraCaptureSession.CaptureCallback cameraCaptureSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private volatile int index = 0;

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            final Image image = PhotographClient.this.imageReader.acquireNextImage();
            if(image == null) {
                return;
            }
            if(this.index++ <= 4 || PhotographClient.this.hasImage) {
                image.close();
                return;
            }
            PhotographClient.this.hasImage = true;
            final Image.Plane[] planes  = image.getPlanes();
            final ByteBuffer byteBuffer = planes[0].getBuffer();
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            final File file = new File(PhotographClient.this.filepath);
            try (final OutputStream output = new FileOutputStream(file)) {
//              final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//              bitmap.compress(Bitmap.CompressFormat.JPEG, PhotographClient.this.quantity, output);
                output.write(bytes, 0, bytes.length);
                cameraCaptureSession.stopRepeating();
                PhotographClient.this.notifyWait();
            } catch (IOException | CameraAccessException e) {
                Log.e(PhotographClient.class.getSimpleName(), "拍照异常", e);
            } finally {
                image.close();
            }
        }

    };

    /**
     * 关闭Camera2
     */
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
            // 包含释放Surface
            this.imageReader.close();
            this.imageReader = null;
        }
    }

}
