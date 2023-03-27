package com.acgist.taoyao.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.acgist.taoyao.client.databinding.ActivityMainBinding;

/**
 * 预览界面
 *
 * @author acgist
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // 启动点亮屏幕
        this.setTurnScreenOn(true);
        // 锁屏显示屏幕
        this.setShowWhenLocked(true);
        // 设置屏幕常亮
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 拉起媒体服务
        this.launchMediaService();
        // 布局
        this.binding = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        // 设置按钮
        this.binding.settings.setOnClickListener(view -> {
            final Intent settings = new Intent(this, SettingsActivity.class);
            this.startActivity(settings);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * 拉起媒体服务
     */
    private void launchMediaService() {
        int times = 0;
        final Display display = this.getWindow().getContext().getDisplay();
        while(Display.STATE_ON != display.getState() && times++ < 10) {
            SystemClock.sleep(100);
        }
        if(display.STATE_ON == display.getState()) {
            // 媒体服务
            Log.i(MainActivity.class.getSimpleName(), "拉起媒体服务");
            final Intent mediaService = new Intent(this, MediaService.class);
            this.startService(mediaService);
        } else {
            Log.w(MainActivity.class.getSimpleName(), "拉起媒体服务失败");
        }
    }

}