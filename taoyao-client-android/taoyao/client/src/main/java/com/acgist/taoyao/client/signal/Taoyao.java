package com.acgist.taoyao.client.signal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.boot.utils.IdUtils;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.client.R;
import com.acgist.taoyao.media.config.MediaProperties;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.client.Room;
import com.acgist.taoyao.media.client.SessionClient;
import com.acgist.taoyao.media.signal.ITaoyao;

import org.apache.commons.lang3.ArrayUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * 桃夭信令
 *
 * @author acgist
 */
public final class Taoyao implements ITaoyao {

    /**
     * 端口
     */
    private final int port;
    /**
     * 地址
     */
    private final String host;
    /**
     * 信令版本
     */
    private final String version;
    /**
     * 终端名称
     */
    private final String name;
    /**
     * 终端ID
     */
    private final String clientId;
    /**
     * 终端类型
     */
    private final String clientType;
    /**
     * 桃夭帐号
     */
    private final String username;
    /**
     * 桃夭密码
     */
    private final String password;
    /**
     * 是否关闭
     */
    private volatile boolean close;
    /**
     * 是否连接
     */
    private volatile boolean connect;
    /**
     * 超时时间
     */
    private final int timeout;
    /**
     * Socket
     */
    private Socket socket;
    /**
     * 信令输入
     */
    private InputStream input;
    /**
     * 信令输出
     */
    private OutputStream output;
    /**
     * 加密工具
     */
    private final Cipher encrypt;
    /**
     * 解密工具
     */
    private final Cipher decrypt;
    /**
     * Handler
     */
    private final Handler mainHandler;
    /**
     * 服务上下文
     */
    private final Context context;
    /**
     * Wifi管理器
     */
    private final WifiManager wifiManager;
    /**
     * 电源管理
     */
    private final PowerManager powerManager;
    /**
     * 电池管理器
     */
    private final BatteryManager batteryManager;
    /**
     * 位置管理器
     */
    private final LocationManager locationManager;
    /**
     * 请求消息：同步消息
     */
    private final Map<Long, Message> requestMessage;
    private final Handler loopMessageHandler;
    private final HandlerThread loopMessageThread;
    private final Handler heartbeatHandler;
    private final HandlerThread heartbeatThread;
    private final Handler executeMessageHandler;
    private final HandlerThread executeMessageThread;
    private final MediaManager mediaManager;
    /**
     * 媒体配置
     */
    private MediaProperties mediaProperties;
    /**
     * 房间列表
     */
    private final Map<String, Room> rooms;
    /**
     * 会话终端列表
     */
    private final Map<String, SessionClient> sessionClients;
    public static Taoyao taoyao;

    public Taoyao(
        int port, String host, String version,
        String name, String clientId, String clientType, String username, String password,
        int timeout, String algo, String secret,
        Handler mainHandler, Context context
    ) {
        this.close = false;
        this.connect = false;
        this.port = port;
        this.host = host;
        this.version = version;
        this.name = name;
        this.clientId = clientId;
        this.clientType = clientType;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        final boolean plaintext = algo == null || algo.isEmpty() || algo.equals("PLAINTEXT");
        this.encrypt = plaintext ? null : this.buildCipher(Cipher.ENCRYPT_MODE, algo, secret);
        this.decrypt = plaintext ? null : this.buildCipher(Cipher.DECRYPT_MODE, algo, secret);
        this.mainHandler = mainHandler;
        this.context = context;
        this.wifiManager = context.getSystemService(WifiManager.class);
        this.powerManager = context.getSystemService(PowerManager.class);
        this.batteryManager = context.getSystemService(BatteryManager.class);
        this.locationManager = context.getSystemService(LocationManager.class);
        this.requestMessage = new ConcurrentHashMap<>();
        this.loopMessageThread = new HandlerThread("TaoyaoLoopMessageThread");
        this.loopMessageThread.start();
        this.loopMessageHandler = new Handler(this.loopMessageThread.getLooper());
        this.loopMessageHandler.post(this::loopMessage);
        this.heartbeatThread = new HandlerThread("TaoyaoHeartbeatThread");
        this.heartbeatThread.start();
        this.heartbeatHandler = new Handler(this.heartbeatThread.getLooper());
        this.heartbeatHandler.postDelayed(this::heartbeat, 30L * 1000);
        this.executeMessageThread = new HandlerThread("TaoyaoExecuteMessageThread");
        this.executeMessageThread.start();
        this.executeMessageHandler = new Handler(this.executeMessageThread.getLooper());
        this.mediaManager = MediaManager.getInstance();
        this.rooms = new ConcurrentHashMap<>();
        this.sessionClients = new ConcurrentHashMap<>();
        Taoyao.taoyao = this;
    }

    /**
     * @param mode   加密/解密
     * @param name   算法名称
     * @param secret 密钥
     *
     * @return 加解密工具
     */
    private Cipher buildCipher(int mode, String name, String secret) {
        try {
            final String algo = name.equals("DES") ? "DES/ECB/PKCS5Padding" : "AES/ECB/PKCS5Padding";
            final Cipher cipher = Cipher.getInstance(algo);
            cipher.init(mode, new SecretKeySpec(Base64.getMimeDecoder().decode(secret), name));
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            Log.e(Taoyao.class.getSimpleName(), "创建加解密工具异常", e);
        }
        return null;
    }

    /**
     * 连接信令
     */
    public synchronized boolean connect() {
        if(this.close) {
            return false;
        }
        // 释放连接
        this.disconnect();
        // 开始连接
        Log.d(Taoyao.class.getSimpleName(), "连接信令：" + this.host + ":" + this.port);
        this.socket = new Socket();
        try {
            // 设置读取超时时间：不要设置一直阻塞
//          socket.setSoTimeout(this.timeout);
            this.socket.connect(new InetSocketAddress(this.host, this.port), this.timeout);
            if (this.socket.isConnected()) {
                this.input = this.socket.getInputStream();
                this.output = this.socket.getOutputStream();
                this.clientRegister();
                this.connect = true;
                synchronized (this) {
                    this.notifyAll();
                }
            } else {
                this.connect = false;
                synchronized (this) {
                    try {
                        this.wait(this.timeout);
                    } catch (InterruptedException e) {
                        Log.d(Taoyao.class.getSimpleName(), "信令等待异常", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Taoyao.class.getSimpleName(), "连接信令异常：" + this.host + ":" + this.port, e);
        }
        return this.connect;
    }

    /**
     * 循环读取信令消息
     */
    private void loopMessage() {
        int length = 0;
        short messageLength = 0;
        final byte[] bytes = new byte[1024];
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while (!this.close) {
            try {
                // 重连
                while (!this.close && !this.connect) {
                    this.connect();
                }
                // 读取
                while (!this.close && (length = this.input.read(bytes)) >= 0) {
                    buffer.put(bytes, 0, length);
                    while (buffer.position() > 0) {
                        if (messageLength <= 0) {
                            if (buffer.position() < Short.BYTES) {
                                // 不够消息长度
                                break;
                            } else {
                                buffer.flip();
                                messageLength = buffer.getShort();
                                buffer.compact();
                                if (messageLength > 16 * 1024) {
                                    throw new RuntimeException("超过最大数据大小：" + messageLength);
                                }
                            }
                        } else {
                            if (buffer.position() < messageLength) {
                                // 不够消息长度
                                break;
                            } else {
                                final byte[] message = new byte[messageLength];
                                messageLength = 0;
                                buffer.flip();
                                buffer.get(message);
                                buffer.compact();
                                final String content = new String(this.decrypt.doFinal(message));
                                try {
                                    Log.d(Taoyao.class.getSimpleName(), "处理信令：" + content);
                                    Taoyao.this.on(content);
                                } catch (Exception e) {
                                    Log.e(Taoyao.class.getSimpleName(), "处理信令异常：" + content, e);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(Taoyao.class.getSimpleName(), "接收信令异常", e);
                this.disconnect();
            }
        }
    }

    /**
     * @param message 原始消息
     *
     * @return 加密消息
     */
    private byte[] encrypt(Message message) {
        final byte[] bytes = message.toString().getBytes();
        if (this.encrypt != null) {
            try {
                // 加密
                final byte[] encryptBytes = this.encrypt.doFinal(bytes);
                // 编码
                final ByteBuffer buffer = ByteBuffer.allocateDirect(Short.BYTES + encryptBytes.length);
                buffer.putShort((short) encryptBytes.length);
                buffer.put(encryptBytes);
                buffer.flip();
                // 编码
                final byte[] encodingBytes = new byte[buffer.capacity()];
                buffer.get(encodingBytes);
                return encodingBytes;
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                Log.e(Taoyao.class.getSimpleName(), "加密异常：" + message, e);
            }
        }
        return bytes;
    }

    /**
     * @param message 信令消息
     */
    public void push(Message message) {
        if (this.output == null) {
            Log.w(Taoyao.class.getSimpleName(), "通道没有打开：" + message);
            return;
        }
        Log.i(Taoyao.class.getSimpleName(), "发送信令：" + message);
        try {
            this.output.write(this.encrypt(message));
        } catch (Exception e) {
            Log.e(Taoyao.class.getSimpleName(), "请求信令异常：" + message, e);
        }
    }

    /**
     * @param request 信令请求消息
     *
     * @return 信令响应消息
     */
    public Message request(Message request) {
        final Header header = request.getHeader();
        final Long id = header.getId();
        this.requestMessage.put(id, request);
        synchronized (request) {
            this.push(request);
            try {
                request.wait(this.timeout);
            } catch (InterruptedException e) {
                Log.e(Taoyao.class.getSimpleName(), "请求信令等待异常：" + request, e);
            }
        }
        final Message response = this.requestMessage.remove(id);
        if (response == null || request.equals(response)) {
            Log.w(Taoyao.class.getSimpleName(), "请求信令没有响应：" + request);
            return null;
        }
        return response;
    }

    /**
     * 释放连接
     */
    private synchronized void disconnect() {
        if(!this.connect) {
            return;
        }
        Log.d(Taoyao.class.getSimpleName(), "释放信令：" + this.host + ":" + this.port);
        this.connect = false;
        CloseableUtils.close(this.input);
        CloseableUtils.close(this.output);
        CloseableUtils.close(this.socket);
        this.input = null;
        this.output = null;
        this.socket = null;
    }

    /**
     * 关闭信令
     */
    public synchronized void close() {
        if(this.close) {
            return;
        }
        Log.d(Taoyao.class.getSimpleName(), "关闭信令：" + this.host + ":" + this.port);
        this.close = true;
        this.disconnect();
        this.heartbeatThread.quitSafely();
        this.loopMessageThread.quitSafely();
        this.executeMessageThread.quitSafely();
        this.rooms.values().forEach(Room::close);
        this.sessionClients.values().forEach(SessionClient::close);
    }

    /**
     * @param signal 信令
     * @param args   消息主体内容
     *
     * @return 消息
     */
    public Message buildMessage(String signal, Object... args) {
        final Map<Object, Object> map = new HashMap<>();
        if (ArrayUtils.isNotEmpty(args)) {
            for (int index = 0; index < args.length; index += 2) {
                if(args[index] == null || args[index + 1] == null) {
                    continue;
                }
                map.put(args[index], args[index + 1]);
            }
        }
        return this.buildMessage(signal, map);
    }

    /**
     * @param signal 信令
     * @param body   消息主体
     *
     * @return 消息
     */
    public Message buildMessage(String signal, Object body) {
        final Header header = new Header();
        header.setV(this.version == null ? "1.0.0" : this.version);
        header.setId(IdUtils.buildId());
        header.setSignal(signal);
        final Message message = new Message();
        message.setHeader(header);
        message.setBody(body == null ? Map.of() : body);
        return message;
    }

    /**
     * @param content 信令消息
     */
    private void on(String content) {
        final Message message = JSONUtils.toJava(content, Message.class);
        if (message == null) {
            return;
        }
        final Header header = message.getHeader();
        if (header == null) {
            return;
        }
        final Long id = header.getId();
        final Message request = this.requestMessage.get(id);
        if (request != null) {
            // 同步处理：重新设置响应消息
            this.requestMessage.put(id, message);
            // 唤醒等待线程
            synchronized (request) {
                request.notifyAll();
            }
        } else {
            this.executeMessageHandler.post(() -> this.dispatch(content, header, message));
        }
    }

    private void dispatch(final String content, final Header header, final Message message) {
        final Map<String, Object> body = message.body();
        switch (header.getSignal()) {
            case "client::config"    -> this.clientConfig(message, body);
            case "client::register"  -> this.clientRegister(message, body);
            case "client::reboot"    -> this.clientReboot(message, body);
            case "client::shutdown"  -> this.clientShutdown(message, body);
            case "room::close"       -> this.roomClose(message, body);
            case "room::enter"       -> this.roomEnter(message, body);
//            case "room::expel"       -> this.roomExpel(message, body);
            case "room::invite"      -> this.roomInivte(message, body);
//            case "room::leave"       -> this.roomLeave(message, body);
            case "session::call"     -> this.sessionCall(message, body);
            case "session::close"    -> this.sessionClose(message, body);
            case "session::exchange" -> this.sessionExchange(message, body);
            default                  -> Log.d(Taoyao.class.getSimpleName(), "没有适配信令：" + content);
        }
    }

    /**
     * 注册
     */
    private void clientRegister() {
        final Location location = this.location();
        this.push(this.buildMessage(
            "client::register",
            "username", this.username,
            "password", this.password,
            "name", this.name,
            "clientId", this.clientId,
            "clientType", this.clientType,
            "latitude", location == null ? -1 : location.getLatitude(),
            "longitude", location == null ? -1 : location.getLongitude(),
            "signal", this.signal(),
            "battery", this.battery(),
            "charging", this.charging(),
            "recording", this.mediaManager.isRecording()
        ));
    }

    /**
     * @param message 消息
     * @param body    消息主体
     */
    private void clientConfig(Message message, Map<String, Object> body) {
        this.mediaProperties = JSONUtils.toJava(JSONUtils.toJSON(body), MediaProperties.class);
    }

    /**
     * @param message 消息
     * @param body    消息主体
     */
    private void clientRegister(Message message, Map<String, Object> body) {
        final Integer index = (Integer) body.get("index");
        if (index == null) {
            return;
        }
        IdUtils.setClientIndex(index);
    }

    private void clientReboot(Message message, Map<String, Object> body) {
        Log.i(Taoyao.class.getSimpleName(), "系统重启");
//      this.powerManager.reboot("系统重启");
        Process.killProcess(Process.myPid());
    }

    private void clientShutdown(Message message, Map<String, Object> body) {
        Log.i(Taoyao.class.getSimpleName(), "系统关机");
        // 自行实现
//      this.powerManager.reboot("系统关机");
        Process.killProcess(Process.myPid());
    }

    private void roomClose(Message message, Map<String, Object> body) {
        final String roomId   = MapUtils.get(body, "roomId");
        final Room room = this.rooms.remove(roomId);
        if(room == null) {
            return;
        }
        room.close();
    }

    private void roomEnter(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room = this.rooms.get(roomId);
        if(room == null) {
            return;
        }
        room.newRemoteClient(body);
    }

    private void roomInivte(Message message, Map<String, Object> body) {
        final String roomId   = MapUtils.get(body, "roomId");
        final String password = MapUtils.get(body, "password");
        this.roomEnter(roomId, password);
    }

    public Room roomEnter(String roomId, String password) {
        final Resources resources = this.context.getResources();
        final Room room = this.rooms.computeIfAbsent(
            roomId,
            key -> new Room(
                this.name, this.clientId,
                key, password,
                this.mainHandler, this,
                resources.getBoolean(R.bool.dataConsume),
                resources.getBoolean(R.bool.audioConsume),
                resources.getBoolean(R.bool.videoConsume),
                resources.getBoolean(R.bool.audioProduce),
                resources.getBoolean(R.bool.dataProduce),
                resources.getBoolean(R.bool.videoProduce)
            )
        );
        room.enter();
        room.produceMedia();
        return room;
    }

    private void sessionCall(Message message, Map<String, Object> body) {
        final String name      = MapUtils.get(body, "name");
        final String clientId  = MapUtils.get(body, "clientId");
        final String sessionId = MapUtils.get(body, "sessionId");
        final SessionClient sessionClient = new SessionClient(sessionId, name, clientId, this.mainHandler, this);
        this.sessionClients.put(sessionId, sessionClient);
        sessionClient.init();
        sessionClient.offer();
    }

    private void sessionClose(Message message, Map<String, Object> body) {
    }

    private void sessionExchange(Message message, Map<String, Object> body) {
        final String sessionId            = MapUtils.get(body, "sessionId");
        final SessionClient sessionClient = this.sessionClients.get(sessionId);
        if(sessionClient == null) {
            Log.w(Taoyao.class.getSimpleName(), "会话交换无效会话：" + sessionId);
            return;
        }
        sessionClient.exchange(message, body);
    }

    /**
     * 心跳
     */
    private void heartbeat() {
        this.heartbeatHandler.postDelayed(this::heartbeat, 30L * 1000);
        if(this.close || !this.connect) {
            return;
        }
        final Location location = this.location();
        this.push(this.buildMessage(
            "client::heartbeat",
            "latitude", location == null ? -1 : location.getLatitude(),
            "longitude", location == null ? -1 : location.getLongitude(),
            "signal", this.signal(),
            "battery", this.battery(),
            "charging", this.charging(),
            "recording", this.mediaManager.isRecording()
        ));
    }

    /**
     * @return 电量百分比
     */
    private int battery() {
        return
            this.batteryManager == null ?
            -1 :
            this.batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    /**
     * @return 充电状态
     */
    private boolean charging() {
        return
            this.batteryManager == null ?
            false :
            this.batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    /**
     * @return WIFI信号强度
     */
    private int signal() {
        if(this.wifiManager == null) {
            return -1;
        }
        final WifiInfo wifiInfo = this.wifiManager.getConnectionInfo();
        if(wifiInfo == null) {
            return -1;
        }
        final int signal = this.wifiManager.calculateSignalLevel(wifiInfo.getRssi());
        return signal / this.wifiManager.getMaxSignalLevel() * 100;
    }

    /**
     * @return 位置
     */
    private Location location() {
        if (this.locationManager == null) {
            return null;
        }
        if (
            ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION)   != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return null;
        }
        final Criteria criteria = new Criteria();
        criteria.setCostAllowed(false);
        criteria.setBearingRequired(false);
        criteria.setAltitudeRequired(false);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        final String provider = this.locationManager.getBestProvider(criteria, true);
        if (provider == null) {
            return null;
        }
        return this.locationManager.getLastKnownLocation(provider);
    }

}
