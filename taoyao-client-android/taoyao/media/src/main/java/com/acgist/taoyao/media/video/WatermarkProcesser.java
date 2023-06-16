package com.acgist.taoyao.media.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 水印处理器
 * 只支持时间字符串水印
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

    /**
     * 字符矩阵
     */
    private static final WatermarkMatrix[] MATRICES = new WatermarkMatrix[256];
    /**
     * 水印字符
     */
    private static final String WATERMARK = "-: 0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 时间格式
     */
    private final String format;
    /**
     * 格式
     */
    private final DateTimeFormatter formatter;
    /**
     * 视频宽度
     */
    private final int width;
    /**
     * 视频高度
     */
    private final int height;
    /**
     * 定时器
     */
    private final Timer timer;
    /**
     * 字符矩阵
     */
    private final WatermarkMatrix[] watermark;

    static {
        final char[] chars = WATERMARK.toCharArray();
        for (char value : chars) {
            WatermarkProcesser.build(value);
        }
    }

    /**
     * @param format 时间格式
     * @param width  视频宽度
     * @param height 视频高度
     */
    public WatermarkProcesser(String format, int width, int height) {
        super("水印处理器");
        this.format = format;
        this.width  = width;
        this.height = height;
        this.formatter    = DateTimeFormatter.ofPattern(format);
        final String date = LocalDateTime.now().format(this.formatter);
        this.watermark    = new WatermarkMatrix[date.length()];
        this.timer        = new Timer("Watermark-Timer", true);
        Log.i(WatermarkProcesser.class.getSimpleName(), "水印格式：" + format);
        this.init();
    }

    /**
     * @param format         时间格式
     * @param width          视频宽度
     * @param height         视频高度
     * @param videoProcesser 下个视频处理器
     */
    public WatermarkProcesser(String format, int width, int height, VideoProcesser videoProcesser) {
        this(format, width, height);
        this.next = videoProcesser;
    }

    /**
     * 加载水印
     */
    private void init() {
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int index = 0;
                final char[] chars = LocalDateTime.now().format(WatermarkProcesser.this.formatter).toCharArray();
                for (char value : chars) {
                    WatermarkProcesser.this.watermark[index] = MATRICES[value];
                    index++;
                }
            }
        }, 1000, 1000);
    }

    /**
     * 创建字符矩阵
     *
     * @param source 字符
     */
    private static void build(char source) {
        final Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setDither(true);
        paint.setTextSize(40.0F);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFilterBitmap(true);
        final Paint.FontMetricsInt fontMetricsInt = paint.getFontMetricsInt();
        final String target = Character.toString(source);
        final int width     = (int) paint.measureText(target);
        final int height    = fontMetricsInt.descent - fontMetricsInt.ascent;
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawText(target, 0, fontMetricsInt.leading - fontMetricsInt.ascent, paint);
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
        bitmap.recycle();
        WatermarkProcesser.MATRICES[source] = new WatermarkMatrix(width, height, matrix);
    }

    @Override
    protected void doProcess(VideoFrame.I420Buffer i420Buffer) {
        int widthPos  = 0;
        int heightPos = 0;
        final ByteBuffer buffer = i420Buffer.getDataY();
        for (WatermarkMatrix matrix : this.watermark) {
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
            widthPos  += matrix.width;
            heightPos += matrix.height;
        }
    }

    @Override
    public void close() {
        super.close();
        this.timer.cancel();
    }

    /**
     * 水印矩阵
     *
     * @author acgist
     */
    static class WatermarkMatrix {

        /**
         * 字符宽度
         */
        int width;
        /**
         * 字符高度
         */
        int height;
        /**
         * 字符矩阵
         */
        boolean[][] matrix;

        /**
         * @param width  字符宽度
         * @param height 字符高度
         * @param matrix 字符矩阵
         */
        public WatermarkMatrix(int width, int height, boolean[][] matrix) {
            this.width = width;
            this.height = height;
            this.matrix = matrix;
        }

    }

}
