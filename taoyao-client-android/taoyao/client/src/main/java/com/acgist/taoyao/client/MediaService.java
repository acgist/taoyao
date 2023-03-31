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
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.acgist.taoyao.client.signal.Taoyao;
import com.acgist.taoyao.media.MediaManager;

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

        // 连接
        CONNECT,
        // 重连
        RECONNECT,
        // 屏幕录制
        SCREEN_RECORD;

    }

    private Taoyao taoyao;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        Log.i(MediaService.class.getSimpleName(), "onCreate");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(MediaService.class.getSimpleName(), "onBind");
        return new Binder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(MediaService.class.getSimpleName(), "onStartCommand");
        if (Action.CONNECT.name().equals(intent.getAction())) {
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

    private void openConnect(Intent intent) {
        if (this.taoyao == null) {
            Log.d(MediaService.class.getSimpleName(), "打开信令连接");
            this.mainHandler = (Handler) intent.getSerializableExtra("mainHandler");
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
        final WifiManager wifiManager = context.getSystemService(WifiManager.class);
        final BatteryManager batteryManager = context.getSystemService(BatteryManager.class);
        final LocationManager locationManager = context.getSystemService(LocationManager.class);
        this.close();
        // 连接信令
        this.taoyao = new Taoyao(
            port, host, resources.getString(R.string.version),
            name, clientId, resources.getString(R.string.clientType), username, password,
            resources.getInteger(R.integer.timeout), resources.getString(R.string.encrypt), resources.getString(R.string.encryptSecret),
            this.mainHandler, context,
            wifiManager, batteryManager, locationManager
        );
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
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "NOTIFICATION_CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher_foreground))
            .setTicker("NOTIFICATION_TICKER")
            .setContentTitle("屏幕录制")
            .setContentText("屏幕录制共享")
            .setContentIntent(pendingIntent);
        final Notification notification = notificationBuilder.build();
        final NotificationChannel channel = new NotificationChannel("NOTIFICATION_CHANNEL_ID", "NOTIFICATION_CHANNEL_NAME", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("NOTIFICATION_CHANNEL_DESC");
        final NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        this.startForeground((int) System.currentTimeMillis(), notification);
        MediaManager.getInstance().screenRecord(intent.getParcelableExtra("data"));
    }

}