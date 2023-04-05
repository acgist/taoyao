package com.acgist.taoyao.boot.utils;

import android.util.Log;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;

/**
 * 指针工具
 *
 * @author acgist
 */
public final class PointerUtils {

    private PointerUtils() {
    }

    /**
     * @param object 对象
     * @param name   指针属性名称
     *
     * @return 指针
     */
    public static final long getNativePointer(Object object, String name) {
        if(object == null) {
            return 0L;
        }
        try {
            final Field reader = FieldUtils.getField(object.getClass(), name, true);
            if(reader == null) {
                return 0L;
            }
            return reader.getLong(object);
        } catch (Exception e) {
            Log.e(PointerUtils.class.getSimpleName(), "获取指针异常", e);
        }
        return 0L;
    }

}
