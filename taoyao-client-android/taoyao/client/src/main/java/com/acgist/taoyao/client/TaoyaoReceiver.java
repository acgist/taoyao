package com.acgist.taoyao.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机启动
 *
 * @author acgist
 */
public class TaoyaoReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TaoyaoReceiver.class.getSimpleName(), "onReceive：" + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            this.launchPreview(context);
        } else {
        }
    }

    /**
     * 拉起预览
     *
     * @param context 上下文
     */
    private void launchPreview(Context context) {
        final Intent intent = new Intent(context, MediaService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(MediaService.Action.LAUNCH.name());
        context.startForegroundService(intent);
    }

}
