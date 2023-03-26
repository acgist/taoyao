package com.acgist.taoyao.client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

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
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}