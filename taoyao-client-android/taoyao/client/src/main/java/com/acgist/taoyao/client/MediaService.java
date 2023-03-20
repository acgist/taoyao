package com.acgist.taoyao.client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MediaService extends Service {
    public MediaService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}