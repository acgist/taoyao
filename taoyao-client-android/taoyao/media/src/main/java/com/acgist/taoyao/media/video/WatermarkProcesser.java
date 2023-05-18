package com.acgist.taoyao.media.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;

import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 水印处理器
 *
 * 性能优化：
 * 没有水印：20~25波动
 * 没有定时：26~32波动
 * 定时水印：28~32
 *
 * TODO：优化调整位置优化性能减少内存拷贝
 *
 * @author acgist
 */
public class WatermarkProcesser extends VideoProcesser {

    private static final WatermarkMatrix[] MATRICES = new WatermarkMatrix[256];

    private final String format;
    private final int width;
    private final int height;
    private final Timer timer;
    private final WatermarkMatrix[] watermark;

    public WatermarkProcesser(String format, int width, int height) {
        this.format = format;
        this.width = width;
        this.height = height;
        final String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
        this.watermark = new WatermarkMatrix[date.length()];
        this.timer = new Timer("Watermark-Timer", true);
        this.init();
    }

    public WatermarkProcesser(String format, int width, int height, VideoProcesser videoProcesser) {
        this(format, width, height);
        this.next = videoProcesser;
    }

    private void init() {
        final String source = "-: 0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final char[] chars = source.toCharArray();
        for (char value : chars) {
            this.build(value);
        }
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int index = 0;
                final char[] chars = LocalDateTime.now().format(DateTimeFormatter.ofPattern(WatermarkProcesser.this.format)).toCharArray();
                for (char value : chars) {
                    WatermarkProcesser.this.watermark[index] = MATRICES[value];
                    index++;
                }
            }
        }, 1000, 1000);
    }

    private void build(char source) {
        // TODO：优化复用bitmap
        final String target = Character.toString(source);
        final Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setDither(true);
        paint.setTextSize(40.0F);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFilterBitmap(true);
        final Paint.FontMetricsInt box = paint.getFontMetricsInt();
        final int width = (int) paint.measureText(target);
        final int height = box.descent - box.ascent;
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawText(target, 0, box.leading - box.ascent, paint);
        canvas.save();
        final boolean[][] matrix = new boolean[width][height];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    matrix[i][j] = bitmap.getColor(i, j).toArgb() != 0;
                } else {
                    matrix[i][j] = bitmap.getPixel(i, j) != 0;
                }
            }
        }
        MATRICES[source] = new WatermarkMatrix(width, height, matrix);
        bitmap.recycle();
    }

    @Override
    protected void doProcess(VideoFrame.I420Buffer i420Buffer) {
        int widthPos = 0;
        int heightPos = 0;
        final ByteBuffer buffer = i420Buffer.getDataY();
        for (WatermarkMatrix matrix : watermark) {
            if(matrix == null) {
                continue;
            }
            for (int height = 0; height < matrix.height; height++) {
                for (int width = 0; width < matrix.width; width++) {
                    if(matrix.matrix[width][height]) {
                        buffer.put(this.width * height + width + widthPos, (byte) 0);
                    }
                }
            }
            widthPos += matrix.width;
            heightPos += matrix.height;
        }
    }

    @Override
    public void close() {
        super.close();
        this.timer.cancel();
    }

    static class WatermarkMatrix {

        int width;
        int height;
        boolean[][] matrix;

        public WatermarkMatrix(int width, int height, boolean[][] matrix) {
            this.width = width;
            this.height = height;
            this.matrix = matrix;
        }

    }

}
