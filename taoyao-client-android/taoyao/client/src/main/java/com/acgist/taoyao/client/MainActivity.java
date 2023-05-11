package com.acgist.taoyao.client;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.acgist.taoyao.client.databinding.ActivityMainBinding;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.VideoSourceType;
import com.acgist.taoyao.media.config.Config;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.commons.lang3.RandomUtils;

import java.util.stream.Stream;

/**
 * 预览界面
 *
 * @author acgist
 */
public class MainActivity extends AppCompatActivity {

    private Handler actionHandler;
    private MainHandler mainHandler;
    private ActivityMainBinding binding;
    private MediaProjectionManager mediaProjectionManager;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.i(MainActivity.class.getSimpleName(), "onCreate");
        super.onCreate(bundle);
        final Window window = this.getWindow();
        // 强制横屏
//      this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.buildAction();
        this.requestPermission();
        this.launchMediaService();
        this.registerMediaProjection();
        this.setTurnScreenOn(true);
        this.setShowWhenLocked(true);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.binding = ActivityMainBinding.inflate(this.getLayoutInflater());
        final View root = this.binding.getRoot();
        root.setZ(100F);
        this.setContentView(root);
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
        // 资源释放
    }

    /**
     * 设置按钮线程
     * 防止按钮任务主线程等待导致主线程死锁
     */
    private void buildAction() {
        if(this.actionHandler != null) {
            return;
        }
        final HandlerThread handlerThread = new HandlerThread("ActionThread");
        handlerThread.start();
        this.actionHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * 请求权限
     */
    private void requestPermission() {
        final String[] permissions = new String[] {
            Manifest.permission.CAMERA,
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
            ActivityCompat.requestPermissions(this, permissions, RandomUtils.nextInt());
        }
    }

    /**
     * 拉起媒体服务
     */
    private void launchMediaService() {
        if (this.mainHandler != null) {
            return;
        }
        Log.i(MainActivity.class.getSimpleName(), "拉起媒体服务");
        this.mainHandler = new MainHandler();
        final Context context = this.getApplicationContext();
        final Resources resources = this.getResources();
        final MediaManager mediaManager = MediaManager.getInstance();
        mediaManager.initContext(
            this.mainHandler, context,
            resources.getInteger(R.integer.imageQuantity),
            resources.getString(R.string.audioQuantity),
            resources.getString(R.string.videoQuantity),
            resources.getInteger(R.integer.channelCount),
            resources.getInteger(R.integer.iFrameInterval),
            resources.getString(R.string.storagePathImage),
            resources.getString(R.string.storagePathVideo),
            resources.getString(R.string.watermark),
            VideoSourceType.valueOf(resources.getString(R.string.videoSourceType))
        );
        if(resources.getBoolean(R.bool.broadcaster)) {
            mediaManager.initTTS(context);
        }
        // 注意：不能使用intent传递
        MediaService.mainHandler = this.mainHandler;
        final Intent intent = new Intent(this, MediaService.class);
        intent.setAction(MediaService.Action.CONNECT.name());
        this.startService(intent);
    }

    /**
     * 注册捕获屏幕功能
     */
    private void registerMediaProjection() {
        if (this.mediaProjectionManager != null && this.activityResultLauncher != null) {
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
     * 功能按钮（测试使用）
     *
     * @param view View
     */
    private void action(View view) {
        this.actionHandler.post(() -> {
            // 进入房间
//          Taoyao.taoyao.roomEnter("4f19f6fc-1763-499b-a352-d8c955af5a6e", null);
            // 监控终端
//          Taoyao.taoyao.sessionCall("taoyao");
        });
    }

    /**
     * 录像按钮
     *
     * @param view View
     */
    private void record(View view) {
        this.actionHandler.post(() -> {
            final MediaManager mediaManager = MediaManager.getInstance();
            if (mediaManager.isRecording()) {
                mediaManager.stopRecord();
            } else {
                mediaManager.startRecord();
            }
        });
    }

    /**
     * 设置按钮
     *
     * @param view View
     */
    private void settings(View view) {
        this.actionHandler.post(() -> {
            final Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
        });
    }

    /**
     * 拍照按钮
     *
     * @param view View
     */
    private void photograph(View view) {
        this.actionHandler.post(() -> {
            MediaManager.getInstance().photograph();
        });
    }

    /**
     * MainHandler
     * 后台线程和UI线程关联线程Handler
     *
     * @author acgist
     */
    public class MainHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message message) {
            super.handleMessage(message);
            Log.d(MainHandler.class.getSimpleName(), "MainHandler消息：" + message.what);
            switch (message.what) {
                case Config.WHAT_SCREEN_CAPTURE   -> MainActivity.this.screenCapture(message);
                case Config.WHAT_RECORD           -> MainActivity.this.record(message);
                case Config.WHAT_NEW_LOCAL_VIDEO,
                     Config.WHAT_NEW_REMOTE_VIDEO -> MainActivity.this.previewVideo(message);
                case Config.WHAT_REMOVE_VIDEO     -> MainActivity.this.removePreviewVideo(message);
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
     * 录制按钮
     *
     * @param message 消息
     */
    private void record(Message message) {
        final Resources resources         = this.getResources();
        final Resources.Theme theme       = this.getTheme();
        final FloatingActionButton record = this.binding.record;
        if(Boolean.TRUE.equals(message.obj)) {
            record.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(R.color.purple_500, theme)));
        } else {
            record.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(R.color.teal_200,   theme)));
        }
    }

    /**
     * 视频预览
     *
     * @param message 消息
     */
    private synchronized void previewVideo(Message message) {
        final GridLayout video = this.binding.video;
        final int count        = video.getChildCount();
        final GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
            GridLayout.spec(count / 2, 1.0F),
            GridLayout.spec(count % 2, 1.0F)
        );
        layoutParams.width  = 0;
        layoutParams.height = 0;
        final SurfaceView surfaceView = (SurfaceView) message.obj;
        surfaceView.setZ(0F);
        video.addView(surfaceView, layoutParams);
    }

    /**
     * 移除视频预览
     *
     * @param message 消息
     */
    private synchronized void removePreviewVideo(Message message) {
        final GridLayout video        = this.binding.video;
        final SurfaceView surfaceView = (SurfaceView) message.obj;
        final int index = video.indexOfChild(surfaceView);
        if(index < 0) {
            return;
        }
        video.removeViewAt(index);
    }

}