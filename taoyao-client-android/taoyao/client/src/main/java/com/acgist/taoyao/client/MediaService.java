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
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.acgist.taoyao.client.signal.Taoyao;
import com.acgist.taoyao.media.MediaManager;
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
        System.loadLibrary("taoyao");
        System.loadLibrary("jingle_peerconnection_so");
    }

    /**
     * 动作类型
     *
     * @author acgist
     */
    public enum Action {

        // 启动
        LAUNCH,
        // 连接
        CONNECT,
        // 重连
        RECONNECT,
        // 屏幕录制
        SCREEN_RECORD;

    }

    public static Handler mainHandler;

    private Taoyao taoyao;
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
        this.cleanAllNotification();
        if (Action.LAUNCH.name().equals(intent.getAction())) {
            this.launch(intent);
            this.openConnect(intent);
        } else if (Action.CONNECT.name().equals(intent.getAction())) {
            this.openConnect(intent);
        } else if (Action.RECONNECT.name().equals(intent.getAction())) {
            this.reconnect();
        } else if (Action.SCREEN_RECORD.name().equals(intent.getAction())) {
            this.screenRecord(intent);
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

    private void launch(Intent intent) {
        final Intent notificationIntent = new Intent(this, MediaService.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "TAOYAO")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher_foreground))
            .setContentTitle("桃夭后台")
            .setContentText("桃夭正在后台运行")
            .setContentIntent(pendingIntent);
        final Notification notification = notificationBuilder.build();
        this.startForeground((int) System.currentTimeMillis(), notification);
    }

    private void openConnect(Intent intent) {
        if (this.taoyao == null) {
            Log.d(MediaService.class.getSimpleName(), "打开信令连接");
            this.connect();
        } else {
            Log.d(MediaService.class.getSimpleName(), "信令已经连接");
        }
    }

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
        final int port = sharedPreferences.getInt("settings.port", 9999);
        final String host = sharedPreferences.getString("settings.host", "192.168.1.100");
        final String name = sharedPreferences.getString("settings.name", "移动端");
        final String clientId = sharedPreferences.getString("settings.clientId", "mobile");
        final String username = sharedPreferences.getString("settings.username", "taoyao");
        final String password = sharedPreferences.getString("settings.password", "taoyao");
        final Resources resources = this.getResources();
        // 系统服务
        final Context context = this.getApplicationContext();
        this.close();
        // 连接信令
        this.taoyao = new Taoyao(
            port, host, resources.getString(R.string.version),
            name, clientId, resources.getString(R.string.clientType), username, password,
            resources.getInteger(R.integer.timeout), resources.getString(R.string.encrypt), resources.getString(R.string.encryptSecret),
            this.mainHandler, context, this.taoyaoListener
        );
        MediaManager.getInstance().initTaoyao(this.taoyao);
        Toast.makeText(this.getApplicationContext(), "连接信令", Toast.LENGTH_SHORT).show();
    }

    private synchronized void close() {
        if (this.taoyao == null) {
            return;
        }
        Toast.makeText(this.getApplicationContext(), "关闭信令", Toast.LENGTH_SHORT).show();
        this.taoyao.close();
        this.taoyao = null;
    }

    public void screenRecord(Intent intent) {
        final Intent notificationIntent = new Intent(this, MediaService.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "TAOYAO")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher_foreground))
            // 自动清除
//          .setAutoCancel(true)
            .setContentTitle("录制屏幕")
            .setContentText("桃夭正在录制屏幕")
            .setContentIntent(pendingIntent);
        final Notification notification = notificationBuilder.build();
        this.startForeground((int) System.currentTimeMillis(), notification);
        MediaManager.getInstance().initScreen(intent.getParcelableExtra("data"));
    }

    private void mkdir(String path, String type) {
        final Path imagePath = Paths.get(
            Environment.getExternalStoragePublicDirectory(type).getAbsolutePath(),
            path
        );
        final File file = imagePath.toFile();
        if(file.exists()) {
            Log.d(MediaService.class.getSimpleName(), "目录已经存在：" + imagePath);
        } else {
            Log.d(MediaService.class.getSimpleName(), "新建文件目录：" + imagePath);
            file.mkdirs();
        }
    }

    private void buildNotificationChannel() {
        final NotificationChannel channel = new NotificationChannel("TAOYAO", "桃夭通知", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setShowBadge(false);
        channel.setDescription("桃夭系统通知");
        final NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void cleanAllNotification() {
        final NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
        notificationManager.cancelAll();
    }

}