package com.acgist.taoyao.media.video;

import android.content.Context;
import android.util.Log;

import org.webrtc.CapturerObserver;
import org.webrtc.JavaI420Buffer;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注意：只是功能验证，没有实现资源释放。
 */
public class ShareVideoCapturer implements VideoCapturer {

    private byte[] bytes = new byte[1024 * 1024];
    private boolean running = false;
    private CapturerObserver capturerObserver;
    private final Map<VideoTrack, VideoFrame> frames = new HashMap<>();

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int width, int height, int framerate) {
        this.running = true;
        final Thread thread = new Thread(() -> {
            final int col = 2;
            final int row = 2;
            final int stride  = width;
            final int width_  = width  / col;
            final int height_ = height / row;
            Log.d(ShareVideoCapturer.class.getSimpleName(), "原始宽度：" + width);
            Log.d(ShareVideoCapturer.class.getSimpleName(), "原始高度：" + height);
            Log.d(ShareVideoCapturer.class.getSimpleName(), "目标宽度：" + width_);
            Log.d(ShareVideoCapturer.class.getSimpleName(), "目标高度：" + height_);
            final JavaI420Buffer buffer = JavaI420Buffer.wrap(
                width, height,
                ByteBuffer.allocateDirect(width * height), stride,
                ByteBuffer.allocateDirect(width * height / 2), stride,
                ByteBuffer.allocateDirect(width * height / 2), stride,
                null
            );
            this.clearBuffer(buffer);
            final AtomicInteger index = new AtomicInteger(0);
            while(ShareVideoCapturer.this.running) {
                synchronized (ShareVideoCapturer.this.frames) {
                    do {
                        try {
                            // 25帧
                            ShareVideoCapturer.this.frames.wait(1000 / 25);
                        } catch (Exception e) {
                            Log.e(ShareVideoCapturer.class.getSimpleName(), "等待异常", e);
                        }
                    } while(ShareVideoCapturer.this.frames.isEmpty());
                    index.set(0);
                    ShareVideoCapturer.this.frames.forEach((k, v) -> {
                        final VideoFrame.Buffer c = v.getBuffer();
                        final VideoFrame.Buffer o = c.cropAndScale(0, 0, c.getWidth(), c.getHeight(), width_, height_);
                        final VideoFrame.I420Buffer x = o.toI420();
                        // 如果每个都是独立传输
//                      buffer.getDataY().put(x.getDataY());
//                      buffer.getDataU().put(x.getDataU());
//                      buffer.getDataV().put(x.getDataV());
                        final int row_ = index.get() / col;
                        final int col_ = index.get() % col;
                        final int dstX = col_ * width_;
                        final int dstY = row_ * height_;
                        ShareVideoCapturer.this.copyBuffer(x, buffer, dstX, dstY);
                        x.release();
                        o.release();
                        v.release();
                        index.incrementAndGet();
                        if(index.get() >= col * row) {
                            index.set(0);
                        }
                    });
                    ShareVideoCapturer.this.frames.clear();
                    final VideoFrame frame = new VideoFrame(
                        buffer,
                        0,
                        System.nanoTime()
                    );
                    ShareVideoCapturer.this.capturerObserver.onFrameCaptured(frame);
                }
            }
            buffer.release();
            synchronized (ShareVideoCapturer.this.frames) {
                ShareVideoCapturer.this.frames.forEach((k, v) -> {
                    v.release();
                });
                ShareVideoCapturer.this.frames.clear();
            }
        });
        thread.setName("SHARE-VIDEO-CAPTURER");
        thread.setDaemon(true);
        thread.start();
    }

    private void clearBuffer(VideoFrame.I420Buffer buffer) {
        this.clearBuffer(buffer.getDataY());
        this.clearBuffer(buffer.getDataU());
        this.clearBuffer(buffer.getDataV());
    }

    private void clearBuffer(ByteBuffer buffer) {
        while(buffer.hasRemaining()) {
            buffer.put((byte) 128);
        }
    }

    private void copyBuffer(
        VideoFrame.I420Buffer src,
        VideoFrame.I420Buffer dst,
        int dstX,
        int dstY
    ) {
        final int width  = src.getWidth();
        final int height = src.getHeight();
        // 复制Y平面
        this.copyPlane(
            src.getDataY(), src.getStrideY(),
            dst.getDataY(), dst.getStrideY(),
            width, height, dstX, dstY
        );
        // 复制U平面
        final int uvWidth  = (width  + 1) / 2;
        final int uvHeight = (height + 1) / 2;
        copyPlane(
            src.getDataU(), src.getStrideU(),
            dst.getDataU(), dst.getStrideU(),
            uvWidth, uvHeight, dstX / 2, dstY / 2
        );
        // 复制V平面
        this.copyPlane(
            src.getDataV(), src.getStrideV(),
            dst.getDataV(), dst.getStrideV(),
            uvWidth, uvHeight, dstX / 2, dstY / 2
        );
    }

    private void copyPlane(
        ByteBuffer src, int srcStride,
        ByteBuffer dst, int dstStride,
        int width, int height, int dstX, int dstY
    ) {
        for (int y = 0; y < height; y++) {
            final int srcPos = y * srcStride;
            final int dstPos = (dstY + y) * dstStride + dstX;
            src.position(srcPos);
            src.get(this.bytes, 0, width);
            dst.position(dstPos);
            dst.put(this.bytes, 0, width);
        }
    }

    @Override
    public void stopCapture() throws InterruptedException {
        this.running = false;
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
    }

    @Override
    public void dispose() {
        this.running = false;
    }

    @Override
    public boolean isScreencast() {
        return false;
    }

    public void addSource(MediaStream mediaStream) {
        mediaStream.videoTracks.forEach(track -> {
            track.addSink(new VideoSink() {
                @Override
                public void onFrame(VideoFrame frame) {
                    synchronized (ShareVideoCapturer.this.frames) {
                        if(ShareVideoCapturer.this.running) {
                            frame.retain();
                            final VideoFrame old = ShareVideoCapturer.this.frames.put(track, frame);
                            if(old != null) {
                                old.release();
                            }
                        }
                    }
                }
            });
        });
    }

    public void removeSource(MediaStream mediaStream) {
        mediaStream.videoTracks.forEach(track -> {
            synchronized (ShareVideoCapturer.this.frames) {
                final VideoFrame old = ShareVideoCapturer.this.frames.remove(track);
                if(old != null) {
                    old.release();
                }
            }
        });
    }

}
