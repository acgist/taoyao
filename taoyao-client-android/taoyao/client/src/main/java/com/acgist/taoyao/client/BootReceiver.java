package com.acgist.taoyao.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

/**
 * 开机启动
 *
 * @author acgist
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(BootReceiver.class.getSimpleName(), "onReceive");
        final Resources resources = context.getResources();
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if(resources.getBoolean(R.bool.preview)) {
                this.launchPreview(context);
            }
        }
    }

    /**
     * 拉起预览
     *
     * @param context 上下文
     */
    private void launchPreview(Context context) {
        final Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startForegroundService(intent);
    }

}
