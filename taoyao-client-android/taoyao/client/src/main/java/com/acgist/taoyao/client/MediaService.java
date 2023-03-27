package com.acgist.taoyao.client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.acgist.taoyao.client.signal.Taoyao;

/**
 * 媒体服务
 *
 * @author acgist
 */
public class MediaService extends Service {

    static {
        System.loadLibrary("taoyao");
    }

    public MediaService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(MediaService.class.getSimpleName(), "启动媒体服务");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}