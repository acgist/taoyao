package com.acgist.taoyao.client;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;

/**
 * 开机启动
 *
 * @author acgist
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final Resources resources = context.getResources();
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
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
        final Intent mainActivity = new Intent(context, MainActivity.class);
        mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startForegroundService(mainActivity);
    }

}
