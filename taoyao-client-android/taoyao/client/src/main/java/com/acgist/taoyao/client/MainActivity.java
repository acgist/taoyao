package com.acgist.taoyao.client;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.acgist.taoyao.client.databinding.ActivityMainBinding;
import com.acgist.taoyao.client.signal.Taoyao;
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

    private Handler threadHandler;
    private MainHandler mainHandler;
    private HandlerThread handlerThread;
    private ActivityMainBinding binding;
    private MediaProjectionManager mediaProjectionManager;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.i(MainActivity.class.getSimpleName(), "onCreate");
        super.onCreate(bundle);
        this.catchAllException();
        this.requestPermission();
        this.launchMediaService();
        this.setTurnScreenOn(true);
        this.setShowWhenLocked(true);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.binding = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        this.binding.getRoot().setZ(100F);
        this.buildHandlerThread();
        this.registerMediaProjection();
        this.binding.action.setOnClickListener(this::action);
        this.binding.record.setOnClickListener(this::switchRecord);
        this.binding.settings.setOnClickListener(this::launchSettings);
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

    private void catchAllException() {
        Log.i(MainActivity.class.getSimpleName(), "全局异常捕获");
        final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.e(MediaService.class.getSimpleName(), "系统异常：" + t.getName(), e);
                final Looper looper = Looper.myLooper();
                if (looper == Looper.getMainLooper()) {
//              if(t.getId() == Looper.getMainLooper().getThread().getId()) {
                    // TODO：重启应用
                    old.uncaughtException(t, e);
                } else {
                    // 子线程
                }
            }
        });
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
        this.mainHandler = new MainHandler(this);
        // 不能写在service里面： Attempt to invoke virtual method 'android.view.View android.view.Window.getDecorView()' on a null object reference
        MediaManager.getInstance().initContext(this.mainHandler, this.getApplicationContext());
        final Display display = this.getWindow().getContext().getDisplay();
        while (Display.STATE_ON != display.getState() && waitCount++ < 10) {
            SystemClock.sleep(100);
        }
        if (display.STATE_ON == display.getState()) {
            Log.i(MainActivity.class.getSimpleName(), "拉起媒体服务");
            final Intent intent = new Intent(this, MediaService.class);
            intent.setAction(MediaService.Action.CONNECT.name());
            intent.putExtra("mainHandler", this.mainHandler);
            this.startService(intent);
        } else {
            Log.w(MainActivity.class.getSimpleName(), "拉起媒体服务失败");
        }
    }

    private void buildHandlerThread() {
        if (this.handlerThread != null) {
            return;
        }
        this.handlerThread = new HandlerThread("MainHandleThread");
        this.handlerThread.start();
        this.threadHandler = new Handler(this.handlerThread.getLooper());
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

    private void action(View view) {
        this.threadHandler.post(() -> {
            // 进入房间
            Taoyao.taoyao.enterRoom("d8f1e91c-58d0-4e58-ad67-decc0fd61df2", null);
        });
    }

    private synchronized void switchRecord(View view) {
        final MediaRecorder mediaRecorder = MediaRecorder.getInstance();
        if (mediaRecorder.isActive()) {
            mediaRecorder.stop();
        } else {
            mediaRecorder.record(this.getResources().getString(R.string.storagePathVideo));
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

    private void photograph(View view) {
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
            switch (message.what) {
                case Config.WHAT_SCREEN_CAPTURE -> this.mainActivity.screenCapture(message);
                case Config.WHAT_NEW_LOCAL_VIDEO -> this.mainActivity.newLocalVideo(message);
                case Config.WHAT_NEW_REMOTE_VIDEO -> this.mainActivity.newRemoteVideo(message);
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
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = 1;
        surfaceView.setZ(0F);
        this.addContentView(surfaceView, layoutParams);
    }

    private void newRemoteVideo(Message message) {
        final SurfaceView surfaceView = (SurfaceView) message.obj;
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = 1;
        surfaceView.setZ(0F);
        this.addContentView(surfaceView, layoutParams);
    }

}