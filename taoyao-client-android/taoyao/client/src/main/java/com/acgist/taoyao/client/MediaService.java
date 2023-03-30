package com.acgist.taoyao.client;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.acgist.taoyao.client.signal.Taoyao;

/**
 * 媒体服务
 *
 * @author acgist
 */
public class MediaService extends Service {

    static {
        System.loadLibrary("taoyao");
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
        RECONNECT;

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
            if (this.taoyao == null) {
                Log.d(MediaService.class.getSimpleName(), "打开信令连接");
                this.mainHandler = (Handler) intent.getSerializableExtra("mainHandler");
                this.connect();
            } else {
                Log.d(MediaService.class.getSimpleName(), "信令已经连接");
            }
        } else if (Action.RECONNECT.name().equals(intent.getAction())) {
            Log.d(MediaService.class.getSimpleName(), "重新连接信令");
            this.connect();
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

}