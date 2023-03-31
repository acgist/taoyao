package com.acgist.taoyao.client;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.acgist.taoyao.client.databinding.ActivityMainBinding;
import com.acgist.taoyao.config.Config;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.MediaRecorder;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * 预览界面
 *
 * @author acgist
 */
public class MainActivity extends AppCompatActivity implements Serializable {

    private MainHandler mainHandler;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private MediaProjectionManager mediaProjectionManager;

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
        this.registerMediaProjection();
        this.binding.record.setOnClickListener(this::switchRecord);
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
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if(Stream.of(permissions).map(this.getApplicationContext()::checkSelfPermission).allMatch(v -> v == PackageManager.PERMISSION_GRANTED)) {
            Log.i(MediaService.class.getSimpleName(), "授权成功");
        } else {
            ActivityCompat.requestPermissions(this, permissions, 0);
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
                this.mainHandler = new MainHandler(this);
            }
            intent.setAction(MediaService.Action.CONNECT.name());
            intent.putExtra("mainHandler", this.mainHandler);
            this.startService(intent);
        } else {
            Log.w(MainActivity.class.getSimpleName(), "拉起媒体服务失败");
        }
    }

    private void registerMediaProjection() {
        if(this.activityResultLauncher != null && this.mediaProjectionManager != null) {
            return;
        }
        this.mediaProjectionManager = this.getApplicationContext().getSystemService(MediaProjectionManager.class);
        this.activityResultLauncher = this.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    Log.i(MediaManager.class.getSimpleName(), "屏幕捕获成功");
                    final Intent intent = new Intent(this, MediaService.class);
                    intent.setAction(MediaService.Action.SCREEN_RECORD.name());
                    intent.putExtra("data", result.getData());
                    intent.putExtra("code", result.getResultCode());
                    this.startService(intent);
                } else {
                    Log.w(MainActivity.class.getSimpleName(), "屏幕捕获失败：" + result.getResultCode());
                }
            }
        );
    }

    private void switchRecord(View view) {
        final MediaRecorder mediaRecorder = MediaRecorder.getInstance();
        if(mediaRecorder.isActive()) {
            mediaRecorder.stop();
        } else {
            MediaManager.getInstance().init(this.mainHandler, this.getApplicationContext());
            MediaManager.getInstance().initAudio();
            MediaManager.getInstance().initVideo();
            mediaRecorder.init(System.currentTimeMillis() + ".mp4", null, null, 1, 1);
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
    public static class MainHandler extends Handler implements Serializable {

        private final MainActivity mainActivity;

        public MainHandler(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void handleMessage(@NonNull Message message) {
            super.handleMessage(message);
            Log.d(MainHandler.class.getSimpleName(), "Handler消息：" + message.what + " - " + message.obj);
            switch(message.what) {
                case Config.WHAT_SCREEN_CAPTURE -> this.mainActivity.screenCapture(message);
                case Config.WHAT_NEW_LOCAL_VIDEO -> this.mainActivity.newLocalVideo(message);
            }
        }

    }

    /**
     * 屏幕捕获
     *
     * @param message 消息
     */
    private void screenCapture(Message message) {
        this.activityResultLauncher.launch(this.mediaProjectionManager.createScreenCaptureIntent());
    }

    /**
     * 新建用户视频
     *
     * @param message 消息
     */
    private void newLocalVideo(Message message) {
        final SurfaceView surfaceView = (SurfaceView) message.obj;
        this.addContentView(surfaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

}