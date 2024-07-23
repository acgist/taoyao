package com.acgist.taoyao.media.audio;

import android.util.Log;

/**
 * Rnnoise降噪
 *
 * 注意：暂时只支持48K采样率，其他采样率需要先重新采样。
 *
 * https://github.com/xiph/rnnoise.git
 */
public class RnnoiseProcesser {

    /**
     * 声道数据
     */
    private byte[] src;
    /**
     * 降噪配置对象指针
     */
    private long pointer;
    /**
     * 是否启用
     */
    private final boolean enabled = true;

    private static final int BITS          = 16;
    private static final int RATE          = 48000;
    private static final int SIZE_MONO     = 960;
    private static final int SIZE_STEREO   = 1920;
    private static final int CHANNEL_COUNT = 1;

    /**
     * @see #init()
     */
    public final void init() {
        this.init(BITS, SIZE_MONO, RATE);
    }

    /**
     * @see #Init(int, int, int)
     */
    public final void init(int bits, int size, int rate) {
        if(!this.enabled) {
            return;
        }
        if(CHANNEL_COUNT == 1) {
            this.src = new byte[SIZE_MONO];
            this.pointer = Init(bits, size, rate);
        } else {
            this.src = new byte[SIZE_MONO];
            this.pointer = Init(bits, size, rate);
        }
        Log.i(RnnoiseProcesser.class.getSimpleName(), String.format("配置降噪参数：%d - %d - %d", bits, size, rate));
    }

    /**
     * @see #rnnoise(int, int, byte[])
     */
    public final byte[] rnnoise(byte[] pcm) {
        return this.rnnoise(0, pcm.length, pcm);
    }

    /**
     * @see #Rnnoise(long, byte[])
     */
    public final byte[] rnnoise(final int offset, final int capacity, final byte[] pcm) {
        if(!this.enabled) {
            return pcm;
        }
        if(this.pointer == 0L) {
            Log.w(RnnoiseProcesser.class.getSimpleName(), "降噪对象没有初始成功原样返回");
            return pcm;
        }
        if(capacity == SIZE_MONO) {
            System.arraycopy(pcm, offset, this.src, 0, SIZE_MONO);
            final byte[] dst = Rnnoise(this.pointer, this.src);
            System.arraycopy(dst, 0, pcm, offset, SIZE_MONO);
            return pcm;
        } else if(capacity == SIZE_STEREO) {
            // 提取单个声道
            for (int index = offset, jndex = 0; index < capacity + offset; index += 4, jndex += 2) {
                this.src[jndex]     = pcm[index];
                this.src[jndex + 1] = pcm[index + 1];
            }
            final byte[] dst = Rnnoise(this.pointer, this.src);
            for (int index = offset, jndex = 0; index < capacity + offset; index += 4, jndex += 2) {
                pcm[index]     = dst[jndex];
                pcm[index + 1] = dst[jndex + 1];
                pcm[index + 2] = dst[jndex];
                pcm[index + 3] = dst[jndex + 1];
            }
            return pcm;
        } else {
            return pcm;
        }
    }

    /**
     * @see #Release(long)
     */
    public final void release() {
        if(!this.enabled) {
            return;
        }
        Log.i(RnnoiseProcesser.class.getSimpleName(), "释放降噪对象");
        if(this.pointer == 0L) {
            return;
        }
        Release(this.pointer);
    }

    /**
     * 初始化
     *
     * @param bits 采样位深
     * @param size 数据大小
     * @param rate 采样率
     *
     * @return 降噪配置对象指针
     */
    private native final long Init(int bits, int size, int rate);

    /**
     * 降噪
     *
     * @param pointer 降噪配置对象指针
     * @param pcm     PCM数据
     *
     * @return 降噪后的PCM数据
     */
    private native final byte[] Rnnoise(long pointer, byte[] pcm);

    /**
     * 释放资源
     *
     * @param pointer 降噪配置对象指针
     */
    private native final void Release(long pointer);

}
