package com.acgist.taoyao.client;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.acgist.taoyao.client.databinding.ActivityMainBinding;
import com.acgist.taoyao.client.signal.Taoyao;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.VideoSourceType;
import com.acgist.taoyao.media.config.Config;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * 预览界面
 *
 * @author acgist
 */
public class MainActivity extends AppCompatActivity implements Serializable {

    private Handler threadHandler;
    private MainHandler mainHandler;
    private ActivityMainBinding binding;
    private MediaProjectionManager mediaProjectionManager;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.i(MainActivity.class.getSimpleName(), "onCreate");
        super.onCreate(bundle);
        this.requestPermission();
        this.launchMediaService();
        this.setTurnScreenOn(true);
        this.setShowWhenLocked(true);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.binding = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        this.binding.getRoot().setZ(100F);
        this.registerMediaProjection();
        this.binding.action.setOnClickListener(this::action);
        this.binding.record.setOnClickListener(this::record);
        this.binding.settings.setOnClickListener(this::settings);
        this.binding.photograph.setOnClickListener(this::photograph);
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
            Manifest.permission.REBOOT,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (Stream.of(permissions).map(this.getApplicationContext()::checkSelfPermission).allMatch(v -> v == PackageManager.PERMISSION_GRANTED)) {
            Log.i(MediaService.class.getSimpleName(), "授权成功");
        } else {
            ActivityCompat.requestPermissions(this, permissions, 0);
        }
    }

    /**
     * 拉起媒体服务
     */
    private void launchMediaService() {
        if (this.mainHandler != null) {
            return;
        }
        int waitCount = 0;
        this.mainHandler = new MainHandler();
        final Resources resources = this.getResources();
        MediaManager.getInstance().initContext(
            this.mainHandler, this.getApplicationContext(),
            resources.getInteger(R.integer.imageQuantity),
            resources.getString(R.string.audioQuantity),
            resources.getString(R.string.videoQuantity),
            resources.getInteger(R.integer.channelCount),
            resources.getInteger(R.integer.iFrameInterval),
            resources.getString(R.string.storagePathImage),
            resources.getString(R.string.storagePathVideo),
            VideoSourceType.valueOf(resources.getString(R.string.videoSourceType))
        );
        final Display display = this.getWindow().getContext().getDisplay();
        while (Display.STATE_ON != display.getState() && waitCount++ < 10) {
            SystemClock.sleep(100);
        }
        if (display.STATE_ON == display.getState()) {
            Log.i(MainActivity.class.getSimpleName(), "拉起媒体服务");
            final Intent intent = new Intent(this, MediaService.class);
            intent.setAction(MediaService.Action.CONNECT.name());
            // 注意：不能使用intent传递
            MediaService.mainHandler = this.mainHandler;
            this.startService(intent);
        } else {
            Log.w(MainActivity.class.getSimpleName(), "拉起媒体服务失败");
        }
    }

    private void registerMediaProjection() {
        if (this.activityResultLauncher != null && this.mediaProjectionManager != null) {
            return;
        }
        this.mediaProjectionManager = this.getApplicationContext().getSystemService(MediaProjectionManager.class);
        this.activityResultLauncher = this.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
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

    /**
     * 功能测试按钮根据实际情况设置功能
     *
     * @param view View
     */
    private void action(View view) {
        if (this.threadHandler == null) {
            final HandlerThread handlerThread = new HandlerThread("ActionThread");
            handlerThread.start();
            this.threadHandler = new Handler(handlerThread.getLooper());
        }
        this.threadHandler.post(() -> {
            // 进入房间
            Taoyao.taoyao.roomEnter("4f19f6fc-1763-499b-a352-d8c955af5a6e", null);
//          Taoyao.taoyao.sessionCall("taoyao");
        });
    }

    private void record(View view) {
        final MediaManager mediaManager = MediaManager.getInstance();
        if (mediaManager.isRecording()) {
            mediaManager.stopRecord();
        } else {
            mediaManager.startRecord();
        }
    }

    private void settings(View view) {
        final Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    private void photograph(View view) {
        MediaManager.getInstance().photograph();
    }

    /**
     * Handler
     *
     * @author acgist
     */
    public class MainHandler extends Handler implements Serializable {

        @Override
        public void handleMessage(@NonNull Message message) {
            super.handleMessage(message);
            Log.d(MainHandler.class.getSimpleName(), "Handler消息：" + message.what + " - " + message.obj);
            switch (message.what) {
                case Config.WHAT_SCREEN_CAPTURE   -> MainActivity.this.screenCapture(message);
                case Config.WHAT_NEW_LOCAL_VIDEO,
                     Config.WHAT_NEW_REMOTE_VIDEO -> MainActivity.this.previewVideo(message);
                case Config.WHAT_REMOVE_VIDEO     -> MainActivity.this.removeVideo(message);
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
     * 预览用户视频
     *
     * @param message 消息
     */
    private void previewVideo(Message message) {
        final GridLayout video = this.binding.video;
        final int count = video.getChildCount();
        final GridLayout.Spec rowSpec    = GridLayout.spec(count / 2, 1.0F);
        final GridLayout.Spec columnSpec = GridLayout.spec(count % 2, 1.0F);
        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(rowSpec, columnSpec);
        layoutParams.width  = 0;
        layoutParams.height = 0;
        final SurfaceView surfaceView = (SurfaceView) message.obj;
        surfaceView.setZ(0F);
        video.addView(surfaceView, layoutParams);
    }

    private void removeVideo(Message message) {
        synchronized (this) {
            final GridLayout video = this.binding.video;
            final SurfaceView surfaceView = (SurfaceView) message.obj;
            final int index = video.indexOfChild(surfaceView);
            video.removeViewAt(index);
        }
    }

}