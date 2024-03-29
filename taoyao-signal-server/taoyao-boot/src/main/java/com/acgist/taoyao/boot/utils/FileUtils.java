package com.acgist.taoyao.boot.utils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;

import org.apache.commons.lang3.ArrayUtils;

/**
 * 文件工具
 * 
 * @author acgist
 */
public final class FileUtils {

    private FileUtils() {
    }
    
    /**
     * 数据进制
     */
    private static final int SCALE = 1024;
    /**
     * 文件大小单位
     */
    private static final String[] UNITS = { "B", "KB", "MB", "GB", "TB", "PB" };
    
    /**
     * @param size 文件大小
     * 
     * @return 文件大小
     */
    public static final String formatSize(Long size) {
        return FileUtils.formatSize(size, "B");
    }
    
    /**
     * @param size 文件大小
     * @param unit 当前单位
     * 
     * @return 文件大小
     */
    public static final String formatSize(Long size, String unit) {
        if(size == null || size <= 0L) {
            return "0B";
        }
        int index    = ArrayUtils.indexOf(UNITS, unit);
        double value = size;
        while(value >= SCALE) {
            if(++index >= UNITS.length) {
                index = UNITS.length - 1;
                break;
            }
            value /= SCALE;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_EVEN) + UNITS[index];
    }
    
    /**
     * @return 是否Linux平台
     */
    public static final boolean linux() {
        return File.separatorChar == '/';
    }

    /**
     * 创建目录
     * 
     * @param path 目录
     */
    public static final void mkdirs(String path) {
        final File file = new File(path);
        if(file.exists()) {
            return;
        }
        file.mkdirs();
    }
    
    /**
     * 创建上级目录
     * 
     * @param path 目录
     */
    public static final void mkdirsParent(String path) {
        FileUtils.mkdirs(Paths.get(path).getParent().toFile().getAbsolutePath());
    }
    
}
