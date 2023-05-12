package com.acgist.taoyao.client;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.acgist.taoyao.boot.utils.IdUtils;
import com.acgist.taoyao.client.signal.Taoyao;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.VideoSourceType;
import com.acgist.taoyao.media.signal.ITaoyaoListener;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 媒体服务
 *
 * @author acgist
 */
public class MediaService extends Service {

    static {
        Log.i(MediaService.class.getSimpleName(), "加载C++库文件");
        System.loadLibrary("taoyao");
        System.loadLibrary("jingle_peerconnection_so");
    }

    /**
     * 动作类型
     *
     * @author acgist
     */
    public enum Action {

        /**
         * 系统启动
         */
        BOOT,
        /**
         * 连接信令
         */
        LAUNCH,
        /**
         * 重连信令
         */
        RECONNECT,
        /**
         * 屏幕录制
         */
        SCREEN_CAPTURE;

    }

    private Taoyao taoyao;
    private static Handler mainHandler;
    private static final String TAOYAO = "TAOYAO";
    private final ITaoyaoListener taoyaoListener = new ITaoyaoListener() {
    };

    @Override
    public void onCreate() {
        Log.i(MediaService.class.getSimpleName(), "onCreate");
        Log.i(MediaService.class.getSimpleName(), """
        庭院深深深几许，杨柳堆烟，帘幕无重数。玉勒雕鞍游冶处，楼高不见章台路。
        雨横风狂三月暮，门掩黄昏，无计留春住。泪眼问花花不语，乱红飞过秋千去。

        凌波不过横塘路。但目送、芳尘去。锦瑟华年谁与度。月桥花院，琐窗朱户。只有春知处。
        飞云冉冉蘅皋暮。彩笔新题断肠句。若问闲情都几许。一川烟草，满城风絮。梅子黄时雨。

        :: https://gitee.com/acgist/taoyao
        """);
        super.onCreate();
        final Resources resources = this.getResources();
        this.mkdir(resources.getString(R.string.storagePathImage), Environment.DIRECTORY_PICTURES);
        this.mkdir(resources.getString(R.string.storagePathVideo), Environment.DIRECTORY_MOVIES);
        this.settingAudio();
        this.buildNotificationChannel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(MediaService.class.getSimpleName(), "onBind");
        return new Binder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(MediaService.class.getSimpleName(), "onStartCommand：" + intent.getAction());
        if (Action.BOOT.name().equals(intent.getAction())) {
            this.boot();
            this.openConnect();
        } else if (Action.LAUNCH.name().equals(intent.getAction())) {
            this.launch();
            this.openConnect();
        } else if (Action.RECONNECT.name().equals(intent.getAction())) {
            this.reconnect();
        } else if (Action.SCREEN_CAPTURE.name().equals(intent.getAction())) {
            this.screenCapture(intent);
        } else {
            Log.w(MediaService.class.getSimpleName(), "未知动作：" + intent.getAction());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(MediaService.class.getSimpleName(), "onDestroy");
        super.onDestroy();
        this.close();
    }

    /**
     * 启动
     */
    private void boot() {
        final Intent activityIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, IdUtils.nextInt(), activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, TAOYAO)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher_foreground))
            .setContentTitle("桃夭后台")
            .setContentText("桃夭正在后台运行")
            .setContentIntent(pendingIntent);
        final Notification notification = notificationBuilder.build();
        this.startForeground(IdUtils.nextInt(), notification);
    }

    /**
     * 启动
     */
    private void launch() {
        final Context context = this.getApplicationContext();
        final Resources resources = this.getResources();
        final MediaManager mediaManager = MediaManager.getInstance();
        mediaManager.initContext(
            MediaService.mainHandler, context,
            resources.getInteger(R.integer.imageQuantity),
            resources.getString(R.string.audioQuantity),
            resources.getString(R.string.videoQuantity),
            resources.getInteger(R.integer.channelCount),
            resources.getInteger(R.integer.iFrameInterval),
            resources.getString(R.string.storagePathImage),
            resources.getString(R.string.storagePathVideo),
            resources.getBoolean(R.bool.broadcaster),
            resources.getString(R.string.watermark),
            VideoSourceType.valueOf(resources.getString(R.string.videoSourceType))
        );
    }

    /**
     * 连接信令
     */
    private void openConnect() {
        if (this.taoyao == null) {
            Log.d(MediaService.class.getSimpleName(), "打开信令连接");
            this.connect();
        } else {
            Log.d(MediaService.class.getSimpleName(), "信令已经连接");
        }
    }

    /**
     * 重连信令
     */
    private void reconnect() {
        Log.d(MediaService.class.getSimpleName(), "重新连接信令");
        this.connect();
    }

    /**
     * 连接信令
     */
    private synchronized void connect() {
        // 加载配置
        final SharedPreferences sharedPreferences = this.getSharedPreferences("settings", Context.MODE_PRIVATE);
        final int port        = sharedPreferences.getInt("settings.port", 9999);
        final String host     = sharedPreferences.getString("settings.host", "192.168.1.100");
        final String name     = sharedPreferences.getString("settings.name", "移动端");
        final String clientId = sharedPreferences.getString("settings.clientId", "mobile");
        final String username = sharedPreferences.getString("settings.username", "taoyao");
        final String password = sharedPreferences.getString("settings.password", "taoyao");
        final Context context     = this.getApplicationContext();
        final Resources resources = this.getResources();
        this.close();
        // 连接信令
        this.taoyao = new Taoyao(
            port, host, resources.getString(R.string.version),
            name, clientId, resources.getString(R.string.clientType), username, password,
            resources.getInteger(R.integer.timeout), resources.getString(R.string.encrypt), resources.getString(R.string.encryptSecret),
            this.mainHandler, context, this.taoyaoListener
        );
        MediaManager.getInstance().initTaoyao(this.taoyao);
        Toast.makeText(context, "连接信令", Toast.LENGTH_SHORT).show();
    }

    /**
     * 屏幕录制
     *
     * @param intent Intent
     */
    public void screenCapture(Intent intent) {
        final Intent activityIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, IdUtils.nextInt(), activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, TAOYAO)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher_foreground))
            .setContentTitle("录制屏幕")
            .setContentText("桃夭正在录制屏幕")
            .setContentIntent(pendingIntent);
        final Notification notification = notificationBuilder.build();
        this.startForeground((int) System.currentTimeMillis(), notification);
        MediaManager.getInstance().initScreen(intent.getParcelableExtra("data"));
    }

    /**
     * 关闭连接
     */
    private synchronized void close() {
        if (this.taoyao == null) {
            return;
        }
        Toast.makeText(this.getApplicationContext(), "关闭信令", Toast.LENGTH_SHORT).show();
        this.taoyao.close();
        this.taoyao = null;
    }

    /**
     * 创建目录
     *
     * @param path 路径
     * @param type 类型
     */
    private void mkdir(String path, String type) {
        final Path imagePath = Paths.get(
            Environment.getExternalStoragePublicDirectory(type).getAbsolutePath(),
            path
        );
        final File file = imagePath.toFile();
        if(file.exists()) {
            Log.d(MediaService.class.getSimpleName(), "目录已经存在：" + imagePath);
        } else if(file.mkdirs()) {
            Log.d(MediaService.class.getSimpleName(), "新建目录成功：" + imagePath);
        } else {
            Log.d(MediaService.class.getSimpleName(), "新建目录失败：" + imagePath);
        }
    }

    /**
     * 设置音频
     */
    private void settingAudio() {
        final AudioManager audioManager = this.getApplicationContext().getSystemService(AudioManager.class);
        Log.i(MediaService.class.getSimpleName(), "当前音频模式：" + audioManager.getMode());
        Log.i(MediaService.class.getSimpleName(), "当前音频音量：" + audioManager.getStreamVolume(audioManager.getMode()));
//      Log.i(MediaService.class.getSimpleName(), "当前蓝牙是否打开：" + audioManager.isBluetoothScoOn());
//      Log.i(MediaService.class.getSimpleName(), "当前耳机是否打开：" + audioManager.isWiredHeadsetOn());
//      Log.i(MediaService.class.getSimpleName(), "当前电话扬声器是否打开：" + audioManager.isSpeakerphoneOn());
//      audioManager.setStreamVolume(AudioManager.MODE_IN_COMMUNICATION, audioManager.getStreamMaxVolume(AudioManager.MODE_IN_COMMUNICATION), AudioManager.FLAG_PLAY_SOUND);
    }

    /**
     * 新建通知通道
     */
    private void buildNotificationChannel() {
        final NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
        final NotificationChannel notificationChannel = new NotificationChannel(TAOYAO, "桃夭通知", NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setShowBadge(false);
        notificationChannel.setDescription("桃夭通知");
        notificationManager.createNotificationChannel(notificationChannel);
    }

    /**
     * @param mainHandler MainHandler
     */
    public static final void setMainHandler(Handler mainHandler) {
        MediaService.mainHandler = mainHandler;
    }

}
