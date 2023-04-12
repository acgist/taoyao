package com.acgist.taoyao.boot.utils;

import android.util.Log;

import java.util.List;
import java.util.function.Function;

/**
 * 集合工具
 *
 * @author acgist
 */
public final class ListUtils {

    private ListUtils() {
    }

    /**
     * @param list     集合
     * @param function 执行函数
     *
     * @return 集合首个元素
     *
     * @param <T> 集合类型
     */
    public static final <T> T getOnlyOne(List<T> list, Function<T, T> function) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        final int size = list.size();
        if(size > 1) {
            Log.w(ListUtils.class.getSimpleName(), "集合不止一条数据：" + size);
        }
        return function.apply(list.get(0));
    }

}
