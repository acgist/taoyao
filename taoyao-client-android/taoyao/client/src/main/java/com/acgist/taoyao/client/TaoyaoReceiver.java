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
            this.bootTaoyao(context);
        } else {
        }
    }

    /**
     * 拉起预览
     *
     * @param context 上下文
     */
    private void bootTaoyao(Context context) {
        final Intent serviceIntent = new Intent(context, MediaService.class);
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        serviceIntent.setAction(MediaService.Action.BOOT.name());
        context.startForegroundService(serviceIntent);
    }

}
