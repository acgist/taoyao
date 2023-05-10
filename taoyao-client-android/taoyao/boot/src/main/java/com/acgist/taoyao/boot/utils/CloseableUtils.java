package com.acgist.taoyao.boot.utils;

import android.util.Log;

import java.io.Closeable;

/**
 * 关闭资源工具
 *
 * @author acgist
 */
public final class CloseableUtils {

    private CloseableUtils() {
    }

    /**
     * 关闭资源
     *
     * @param closeable 资源
     */
    public static final void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            Log.e(CloseableUtils.class.getSimpleName(), "关闭资源异常", e);
        }
    }

    /**
     * 关闭资源
     *
     * @param closeable 资源
     */
    public static final void close(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            Log.e(CloseableUtils.class.getSimpleName(), "关闭资源异常", e);
        }
    }

}
