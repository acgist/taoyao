package com.acgist.taoyao.client;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.acgist.taoyao.client.databinding.ActivityMainBinding;

import java.io.Serializable;

/**
 * 预览界面
 *
 * @author acgist
 */
public class MainActivity extends AppCompatActivity {

    private MainHandler mainHandler;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.i(MainActivity.class.getSimpleName(), "onCreate");
        super.onCreate(bundle);
        // 请求权限
        this.requestPermission();
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
        this.binding.settings.setOnClickListener(this::launchSettings);
    }

    @Override
    protected void onStart() {
        Log.i(MainActivity.class.getSimpleName(), "onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(MainActivity.class.getSimpleName(), "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(MainActivity.class.getSimpleName(), "onDestroy");
        super.onDestroy();
    }

    /**
     * 请求权限
     */
    private void requestPermission() {
        final String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        boolean allGranted = true;
        for (String permission : permissions) {
            if(this.getApplicationContext().checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                Log.i(MediaService.class.getSimpleName(), "授权成功：" + permission);
            } else {
                allGranted = false;
                Log.w(MediaService.class.getSimpleName(), "授权失败：" + permission);
            }
        }
        if(!allGranted) {
            Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show();
        }
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
            Log.i(MainActivity.class.getSimpleName(), "拉起媒体服务");
            final Intent intent = new Intent(this, MediaService.class);
            if(this.mainHandler == null) {
                this.mainHandler = new MainHandler();
            }
            intent.setAction(MediaService.Action.CONNECT.name());
            intent.putExtra("mainHandler", this.mainHandler);
            this.startService(intent);
        } else {
            Log.w(MainActivity.class.getSimpleName(), "拉起媒体服务失败");
        }
    }

    /**
     * 拉起设置页面
     *
     * @param view View
     */
    private void launchSettings(View view) {
        final Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    /**
     * Handler
     *
     * @author acgist
     */
    private static class MainHandler extends Handler implements Serializable {

        @Override
        public void handleMessage(@NonNull Message message) {
            super.handleMessage(message);
            Log.d(MainActivity.class.getSimpleName(), "Handler消息：" + message.what + " - " + message.obj);
        }

    }

}