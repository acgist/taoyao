package com.acgist.taoyao.client.signal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.boot.utils.IdUtils;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.client.R;
import com.acgist.taoyao.media.MediaManager;
import com.acgist.taoyao.media.client.RecordClient;
import com.acgist.taoyao.media.client.Room;
import com.acgist.taoyao.media.client.SessionClient;
import com.acgist.taoyao.media.config.MediaAudioProperties;
import com.acgist.taoyao.media.config.MediaProperties;
import com.acgist.taoyao.media.config.MediaVideoProperties;
import com.acgist.taoyao.media.config.WebrtcProperties;
import com.acgist.taoyao.media.signal.ITaoyao;
import com.acgist.taoyao.media.signal.ITaoyaoListener;

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
     * 心跳时间
     */
    private static final long HEARTBEAT_DURATION = 30L * 1000;

    /**
     * 信令版本
     */
    private final String version;
    /**
     * 终端类型
     */
    private final String clientType;
    /**
     * 信令端口
     */
    private final int port;
    /**
     * 信令地址
     */
    private final String host;
    /**
     * 终端名称
     */
    private final String name;
    /**
     * 终端ID
     */
    private final String clientId;
    /**
     * 信令帐号
     */
    private final String username;
    /**
     * 信令密码
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
     * MainHandler
     */
    private final Handler mainHandler;
    /**
     * 上下文
     */
    private final Context context;
    /**
     * 信令监听
     */
    private final ITaoyaoListener taoyaoListener;
    /**
     * WIFI管理器
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
     * 消息Handler
     * 接收消息、重连信令
     */
    private final Handler messageHandler;
    /**
     * 消息线程
     */
    private final HandlerThread messageThread;
    /**
     * 执行Handler
     * 处理接收信令消息
     */
    private final Handler executeHandler;
    /**
     * 执行线程
     */
    private final HandlerThread executeThread;
    /**
     * 心跳Handler
     */
    private final Handler heartbeatHandler;
    /**
     * 心跳线程
     */
    private final HandlerThread heartbeatThread;
    /**
     * 媒体管理器
     */
    private final MediaManager mediaManager;
    /**
     * 请求消息：同步消息
     */
    private final Map<Long, Message> requestMessage;
    /**
     * 视频房间列表
     */
    private final Map<String, Room> rooms;
    /**
     * 视频会话列表
     */
    private final Map<String, SessionClient> sessions;
    /**
     * 全局静态变量
     */
    public static Taoyao taoyao;

    /**
     * @param version        信令版本
     * @param clientType     终端类型
     * @param port           信令端口
     * @param host           信令地址
     * @param name           终端名称
     * @param clientId       终端ID
     * @param username       信令帐号
     * @param password       信令密码
     * @param timeout        超时时间
     * @param algo           加密算法
     * @param secret         加密密钥
     * @param mainHandler    MainHandler
     * @param context        上下文
     * @param taoyaoListener 桃夭监听
     */
    public Taoyao(
        String version, String clientType, int port, String host,
        String name, String clientId, String username, String password,
        int timeout, String algo, String secret,
        Handler mainHandler, Context context, ITaoyaoListener taoyaoListener
    ) {
        this.close      = false;
        this.connect    = false;
        this.version    = version;
        this.clientType = clientType;
        this.port       = port;
        this.host       = host;
        this.name       = name;
        this.clientId   = clientId;
        this.username   = username;
        this.password   = password;
        this.timeout    = timeout;
        final boolean plaintext = algo == null || algo.isEmpty() || algo.equals("PLAINTEXT");
        this.encrypt    = plaintext ? null : this.buildCipher(Cipher.ENCRYPT_MODE, algo, secret);
        this.decrypt    = plaintext ? null : this.buildCipher(Cipher.DECRYPT_MODE, algo, secret);
        this.mainHandler      = mainHandler;
        this.context          = context;
        this.taoyaoListener   = taoyaoListener;
        this.wifiManager      = context.getSystemService(WifiManager.class);
        this.powerManager     = context.getSystemService(PowerManager.class);
        this.batteryManager   = context.getSystemService(BatteryManager.class);
        this.locationManager  = context.getSystemService(LocationManager.class);
        this.messageThread    = this.buildHandlerThread("MessageThread");
        this.messageHandler   = new Handler(this.messageThread.getLooper());
        this.executeThread    = this.buildHandlerThread("ExecuteThread");
        this.executeHandler   = new Handler(this.executeThread.getLooper());
        this.heartbeatThread  = this.buildHandlerThread("HeartbeatThread");
        this.heartbeatHandler = new Handler(this.heartbeatThread.getLooper());
        this.mediaManager     = MediaManager.getInstance();
        this.requestMessage   = new ConcurrentHashMap<>();
        this.rooms            = new ConcurrentHashMap<>();
        this.sessions         = new ConcurrentHashMap<>();
        Taoyao.taoyao         = this;
        // 开始连接
        this.messageHandler.post(this::connect);
        // 开始心跳
        this.heartbeatHandler.postDelayed(this::clientHeartbeat, HEARTBEAT_DURATION);
    }

    /**
     * @param port     信令端口
     * @param host     信令地址
     * @param name     终端名称
     * @param clientId 终端ID
     * @param username 信令帐号
     * @param password 信令密码
     *
     * @return 是否需要重连信令
     */
    public boolean needReconnect(int port, String host, String name, String clientId, String username, String password) {
        return !(
            port == this.port              &&
            host.equals(this.host)         &&
            name.equals(this.name)         &&
            clientId.equals(this.clientId) &&
            username.equals(this.username) &&
            password.equals(this.password)
        );
    }

    /**
     * @param mode   加密/解密
     * @param name   算法名称
     * @param secret 密钥
     *
     * @return 加密解密工具
     */
    private Cipher buildCipher(int mode, String name, String secret) {
        try {
            final String algo   = "DES".equals(name) ? "DES/ECB/PKCS5Padding" : "AES/ECB/PKCS5Padding";
            final Cipher cipher = Cipher.getInstance(algo);
            cipher.init(mode, new SecretKeySpec(Base64.getMimeDecoder().decode(secret), name));
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            Log.e(Taoyao.class.getSimpleName(), "创建加密解密工具异常", e);
        }
        return null;
    }

    /**
     * @param name 线程名称
     *
     * @return 线程
     */
    private HandlerThread buildHandlerThread(String name) {
        final HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.setDaemon(true);
        handlerThread.start();
        return handlerThread;
    }

    /**
     * 连接信令
     *
     * @returns 是否连接成功
     */
    public synchronized boolean connect() {
        if(this.close) {
            return false;
        }
        // 释放连接
        this.disconnect();
        // 开始连接
        Log.i(Taoyao.class.getSimpleName(), "连接信令：" + this.host + ":" + this.port);
        this.socket = new Socket();
        this.taoyaoListener.onConnect();
        try {
            // 设置读取超时时间：不要设置一直阻塞
//          socket.setSoTimeout(this.timeout);
            this.socket.connect(new InetSocketAddress(this.host, this.port), this.timeout);
            if (this.socket.isConnected()) {
                this.connect = true;
                this.input   = this.socket.getInputStream();
                this.output  = this.socket.getOutputStream();
                this.clientRegister();
                this.taoyaoListener.onConnected();
                this.messageHandler.post(this::pull);
            } else {
                this.connect = false;
                this.messageHandler.postDelayed(this::connect, this.timeout);
            }
        } catch (Exception e) {
            Log.e(Taoyao.class.getSimpleName(), "连接信令异常：" + this.host + ":" + this.port, e);
            this.connect = false;
            this.messageHandler.postDelayed(this::connect, this.timeout);
        }
        return this.connect;
    }

    /**
     * 循环读取信令消息
     */
    private void pull() {
        int length              = 0;
        short messageLength     = 0;
        final byte[] bytes      = new byte[1024];
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        try {
            while (!this.close && this.connect && (length = this.input.read(bytes)) >= 0) {
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
                                this.on(content);
                            } catch (Exception e) {
                                Log.e(Taoyao.class.getSimpleName(), "处理信令异常：" + content, e);
                                this.taoyaoListener.onError(e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Taoyao.class.getSimpleName(), "接收信令异常", e);
            this.disconnect();
        }
        if (!this.close) {
            this.messageHandler.post(this::connect);
        }
    }

    /**
     * @param message 原始消息
     *
     * @return 加密消息
     *
     * @throws BadPaddingException       错误填充
     * @throws IllegalBlockSizeException 错误大小
     */
    private byte[] encrypt(Message message) throws BadPaddingException, IllegalBlockSizeException {
        final byte[] bytes = message.toString().getBytes();
        if (this.encrypt == null) {
            return bytes;
        }
        final byte[] encryptBytes  = this.encrypt.doFinal(bytes);
        final ByteBuffer buffer    = ByteBuffer.allocateDirect(Short.BYTES + encryptBytes.length);
        final byte[] encodingBytes = new byte[buffer.capacity()];
        buffer.putShort((short) encryptBytes.length);
        buffer.put(encryptBytes);
        buffer.flip();
        buffer.get(encodingBytes);
        return encodingBytes;
    }

    /**
     * @param message 信令消息
     */
    @Override
    public void push(Message message) {
        if (this.close) {
            Log.w(Taoyao.class.getSimpleName(), "通道已经关闭：" + message);
            return;
        }
        if (!this.connect) {
            Log.w(Taoyao.class.getSimpleName(), "通道没有打开：" + message);
            return;
        }
        Log.i(Taoyao.class.getSimpleName(), "发送信令：" + message);
        try {
            this.output.write(this.encrypt(message));
        } catch (Exception e) {
            Log.e(Taoyao.class.getSimpleName(), "请求信令异常：" + message, e);
            this.disconnect();
        }
    }

    /**
     * @param request 信令请求消息
     *
     * @return 信令响应消息
     */
    @Override
    public Message request(Message request) {
        if(request == null) {
            Log.w(Taoyao.class.getSimpleName(), "信令消息错误：" + request);
            return null;
        }
        final Header header = request.getHeader();
        if(header == null) {
            Log.w(Taoyao.class.getSimpleName(), "信令消息错误：" + request);
            return null;
        }
        final Long id       = header.getId();
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
        Log.i(Taoyao.class.getSimpleName(), "释放信令：" + this.host + ":" + this.port);
        this.taoyaoListener.onDisconnect();
        this.connect = false;
        CloseableUtils.close(this.input);
        CloseableUtils.close(this.output);
        CloseableUtils.close(this.socket);
        this.input  = null;
        this.output = null;
        this.socket = null;
        this.closeRoomMedia();
        this.closeSessionMedia();
    }

    /**
     * 释放所有视频房间
     */
    private void closeRoomMedia() {
        Log.i(Taoyao.class.getSimpleName(), "释放所有视频房间");
        this.rooms.forEach((k, v) -> v.close());
        this.rooms.clear();
    }

    /**
     * 释放所有视频会话
     */
    private void closeSessionMedia() {
        Log.i(Taoyao.class.getSimpleName(), "释放所有视频会话");
        this.sessions.forEach((k, v) -> v.close());
        this.sessions.clear();
    }

    /**
     * 关闭信令
     */
    public synchronized void close() {
        if(this.close) {
            return;
        }
        Log.i(Taoyao.class.getSimpleName(), "关闭信令：" + this.host + ":" + this.port);
        this.close = true;
        this.disconnect();
        this.heartbeatThread.quitSafely();
        this.messageThread.quitSafely();
        this.executeThread.quitSafely();
    }

    /**
     * @param signal 信令
     * @param args   消息主体内容
     *
     * @return 消息
     */
    public Message buildMessage(String signal, Object ... args) {
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
            Log.w(Taoyao.class.getSimpleName(), "信令消息错误：" + content);
            return;
        }
        final Header header = message.getHeader();
        if (header == null) {
            Log.w(Taoyao.class.getSimpleName(), "信令消息错误：" + content);
            return;
        }
        final Long id = header.getId();
        final Message request = this.requestMessage.get(id);
        if (request != null) {
            Log.i(Taoyao.class.getSimpleName(), "处理信令响应：" + content);
            // 同步处理：重新设置响应消息
            this.requestMessage.put(id, message);
            // 唤醒等待线程
            synchronized (request) {
                request.notifyAll();
            }
        } else {
            Log.i(Taoyao.class.getSimpleName(), "处理信令异步：" + content);
            this.executeHandler.post(() -> {
                try {
                    this.dispatch(content, header, message);
                } catch (Exception e) {
                    Log.e(Taoyao.class.getSimpleName(), "处理信令异常：" + content, e);
                    this.taoyaoListener.onError(e);
                }
            });
        }
    }

    /**
     * @param content 信令原始消息
     * @param header  信令消息头部
     * @param message 信令消息
     */
    private void dispatch(final String content, final Header header, final Message message) {
        final boolean done = this.taoyaoListener.preOnMessage(message);
        if(done) {
            Log.d(Taoyao.class.getSimpleName(), "信令前置处理完成：" + message);
            return;
        }
        switch (header.getSignal()) {
            case "client::config"                    -> this.clientConfig(message, message.body());
            case "client::reboot"                    -> this.clientReboot(message, message.body());
            case "client::register"                  -> this.clientRegister(message, message.body());
            case "client::shutdown"                  -> this.clientShutdown(message, message.body());
            case "control::client::record"           -> this.controlClientRecord(message, message.body());
            case "control::config::audio"            -> this.controlConfigAudio(message, message.body());
            case "control::config::video"            -> this.controlConfigVideo(message, message.body());
            case "control::photograph"               -> this.controlPhotograph(message, message.body());
            case "media::audio::volume"              -> this.mediaAudioVolume(message, message.body());
            case "media::consume"                    -> this.mediaConsume(message, message.body());
            case "media::consumer::close"            -> this.mediaConsumerClose(message, message.body());
            case "media::consumer::pause"            -> this.mediaConsumerPause(message, message.body());
            case "media::consumer::resume"           -> this.mediaConsumerResume(message, message.body());
            case "media::consumer::status"           -> this.mediaConsumerStatus(message, message.body());
            case "media::producer::close"            -> this.mediaProducerClose(message, message.body());
            case "media::producer::pause"            -> this.mediaProducerPause(message, message.body());
            case "media::producer::resume"           -> this.mediaProducerResume(message, message.body());
            case "media::video::orientation::change" -> this.mediaVideoOrientationChange(message, message.body());
            case "room::client::list"                -> this.roomClientList(message, message.body());
            case "room::close"                       -> this.roomClose(message, message.body());
            case "room::enter"                       -> this.roomEnter(message, message.body());
            case "room::expel"                       -> this.roomExpel(message, message.body());
            case "room::invite"                      -> this.roomInivte(message, message.body());
            case "room::leave"                       -> this.roomLeave(message, message.body());
            case "session::call"                     -> this.sessionCall(message, message.body());
            case "session::close"                    -> this.sessionClose(message, message.body());
            case "session::exchange"                 -> this.sessionExchange(message, message.body());
            case "session::pause"                    -> this.sessionPause(message, message.body());
            case "session::resume"                   -> this.sessionResume(message, message.body());
            default                                  -> Log.d(Taoyao.class.getSimpleName(), "没有适配信令：" + content);
        }
        this.taoyaoListener.postOnMessage(message);
    }

    /**
     * 配置终端
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void clientConfig(Message message, Map<String, Object> body) {
        final MediaProperties mediaProperties   = JSONUtils.toJava(JSONUtils.toJSON(body.get("media")), MediaProperties.class);
        this.mediaManager.updateMediaConfig(mediaProperties);
        final WebrtcProperties webrtcProperties = JSONUtils.toJava(JSONUtils.toJSON(body.get("webrtc")), WebrtcProperties.class);
        this.mediaManager.updateWebrtcConfig(webrtcProperties);
    }

    /**
     * 终端心跳信令
     */
    private void clientHeartbeat() {
        this.heartbeatHandler.postDelayed(this::clientHeartbeat, HEARTBEAT_DURATION);
        if(this.close || !this.connect) {
            return;
        }
        final Location location = this.location();
        this.push(this.buildMessage(
            "client::heartbeat",
            "latitude",  location == null ? -1 : location.getLatitude(),
            "longitude", location == null ? -1 : location.getLongitude(),
            "signal",    this.signal(),
            "battery",   this.battery(),
            "charging",  this.charging(),
            "clientRecording", this.mediaManager.isRecording()
        ));
    }

    /**
     * 重启终端
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    @SuppressLint("MissingPermission")
    private void clientReboot(Message message, Map<String, Object> body) {
        Log.i(Taoyao.class.getSimpleName(), "系统重启");
        this.powerManager.reboot("系统重启");
        Process.killProcess(Process.myPid());
    }

    /**
     * 终端注册
     */
    private void clientRegister() {
        final Location location = this.location();
        this.push(this.buildMessage(
            "client::register",
            "name",       this.name,
            "clientId",   this.clientId,
            "clientType", this.clientType,
            "username",   this.username,
            "password",   this.password,
            "latitude",   location == null ? -1 : location.getLatitude(),
            "longitude",  location == null ? -1 : location.getLongitude(),
            "signal",     this.signal(),
            "battery",    this.battery(),
            "charging",   this.charging(),
            "clientRecording", this.mediaManager.isRecording()
        ));
    }

    /**
     * 终端注册
     *
     * @param message 消息
     * @param body    消息主体
     */
    private void clientRegister(Message message, Map<String, Object> body) {
        final Integer index = MapUtils.getInteger(body, "index");
        if (index == null) {
            return;
        }
        IdUtils.setClientIndex(index);
    }

    /**
     * 关闭终端
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    @SuppressLint("MissingPermission")
    private void clientShutdown(Message message, Map<String, Object> body) {
        Log.i(Taoyao.class.getSimpleName(), "系统关机");
        this.powerManager.reboot("系统关机");
        Process.killProcess(Process.myPid());
    }

    /**
     * 录像
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void controlClientRecord(Message message, Map<String, Object> body) {
        String filepath;
        final Boolean enabled = MapUtils.getBoolean(body, "enabled");
        if(Boolean.TRUE.equals(enabled)) {
            final RecordClient recordClient = this.mediaManager.startRecord();
            filepath = recordClient.getFilepath();
        } else {
            filepath = this.mediaManager.stopRecord();
        }
        body.put("enabled", enabled);
        body.put("filepath", filepath);
        this.push(message);
    }

    /**
     * 更新音频配置
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void controlConfigAudio(Message message, Map<String, Object> body) {
        final MediaAudioProperties mediaAudioProperties = JSONUtils.toJava(JSONUtils.toJSON(body), MediaAudioProperties.class);
        this.mediaManager.updateAudioConfig(mediaAudioProperties);
        this.push(message);
    }

    /**
     * 更新视频配置
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void controlConfigVideo(Message message, Map<String, Object> body) {
        final MediaVideoProperties mediaVideoProperties = JSONUtils.toJava(JSONUtils.toJSON(body), MediaVideoProperties.class);
        this.mediaManager.updateVideoConfig(mediaVideoProperties);
        this.push(message);
    }

    /**
     * 拍照
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void controlPhotograph(Message message, Map<String, Object> body) {
        final String filepath = this.mediaManager.photograph();
        body.put("filepath", filepath);
        this.push(message);
    }

    /**
     * 远程音量
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaAudioVolume(Message message, Map<String, Object> body) {
        // TODO：如果需要显示音量
    }

    /**
     * 消费媒体信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaConsume(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.mediaConsume(message, body);
    }

    /**
     * 关闭消费者信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaConsumerClose(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.mediaConsumerClose(body);
    }

    /**
     * 暂停消费者信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaConsumerPause(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.mediaConsumerPause(body);
    }

    /**
     * 恢复消费者信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaConsumerResume(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.mediaConsumerResume(body);
    }

    /**
     * 查询消费者状态信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaConsumerStatus(Message message, Map<String, Object> body) {

    }

    /**
     * 关闭生产者信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaProducerClose(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.mediaProducerClose(body);
    }

    /**
     * 暂停生产者信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaProducerPause(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.mediaProducerPause(body);
    }

    /**
     * 恢复生产者信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaProducerResume(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.mediaProducerResume(body);
    }

    /**
     * 视频方向变化信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void mediaVideoOrientationChange(Message message, Map<String, Object> body) {

    }

    /**
     * 房间终端列表信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void roomClientList(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.newRemoteClientFromRoomClientList(body);
    }

    /**
     * 关闭房间信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void roomClose(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.remove(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.close();
    }

    /**
     * 进入房间信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void roomEnter(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        final Room room     = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.newRemoteClientFromRoomEnter(body);
    }

    /**
     * 进入房间信令
     *
     * @param roomId   房间ID
     * @param password 房间密码
     *
     * @return 房间
     */
    public Room roomEnter(String roomId, String password) {
        final Resources resources = this.context.getResources();
        final Room room = this.rooms.computeIfAbsent(
            roomId,
            key -> new Room(
                roomId, this.name,
                password, this.clientId,
                this, this.mainHandler,
                resources.getBoolean(R.bool.preview),
                resources.getBoolean(R.bool.playAudio),
                resources.getBoolean(R.bool.playVideo),
                resources.getBoolean(R.bool.dataConsume),
                resources.getBoolean(R.bool.audioConsume),
                resources.getBoolean(R.bool.videoConsume),
                resources.getBoolean(R.bool.dataProduce),
                resources.getBoolean(R.bool.audioProduce),
                resources.getBoolean(R.bool.videoProduce),
                resources.getBoolean(R.bool.roomUseIceServer),
                this.mediaManager.getMediaProperties(),
                this.mediaManager.getWebrtcProperties()
            )
        );
        final boolean success = room.enter();
        if(success) {
            room.mediaProduce();
            return room;
        } else {
            Log.i(Taoyao.class.getSimpleName(), "进入房间失败：" + roomId);
            this.rooms.remove(roomId);
            return null;
        }
    }

    /**
     * 踢出房间信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void roomExpel(Message message, Map<String, Object> body) {
        final String roomId = MapUtils.get(body, "roomId");
        this.roomLeave(roomId);
    }

    /**
     * 邀请终端信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void roomInivte(Message message, Map<String, Object> body) {
        final String roomId   = MapUtils.get(body, "roomId");
        final String password = MapUtils.get(body, "password");
        this.roomEnter(roomId, password);
    }

    /**
     * 离开房间信令
     *
     * @param roomId 房间ID
     */
    public void roomLeave(String roomId) {
        final Room room = this.rooms.remove(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        this.push(this.buildMessage(
            "room::leave",
            "roomId", roomId
        ));
        room.close();
    }

    /**
     * 离开房间信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void roomLeave(Message message, Map<String, Object> body) {
        final String roomId   = MapUtils.get(body, "roomId");
        final String clientId = MapUtils.get(body, "clientId");
        final Room room       = this.rooms.get(roomId);
        if(room == null) {
            Log.w(Taoyao.class.getSimpleName(), "无效房间：" + roomId);
            return;
        }
        room.closeRemoteClient(clientId);
    }

    /**
     * 发起会话信令
     *
     * @param clientId 终端ID
     */
    public void sessionCall(String clientId) {
        this.requestFuture(
            this.buildMessage(
                "session::call",
                "clientId", clientId
            ),
            response -> {
                final Map<String, Object> body = response.body();
                final String name         = MapUtils.get(body, "name");
                final String sessionId    = MapUtils.get(body, "sessionId");
                final Resources resources = this.context.getResources();
                final SessionClient sessionClient = new SessionClient(
                    sessionId, name, clientId, this, this.mainHandler,
                    resources.getBoolean(R.bool.preview),
                    resources.getBoolean(R.bool.playAudio),
                    resources.getBoolean(R.bool.playVideo),
                    resources.getBoolean(R.bool.dataConsume),
                    resources.getBoolean(R.bool.audioConsume),
                    resources.getBoolean(R.bool.videoConsume),
                    resources.getBoolean(R.bool.dataProduce),
                    resources.getBoolean(R.bool.audioProduce),
                    resources.getBoolean(R.bool.videoProduce),
                    this.mediaManager.getMediaProperties(),
                    this.mediaManager.getWebrtcProperties()
                );
                this.sessions.put(sessionId, sessionClient);
            }
        );
    }

    /**
     * 发起会话信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void sessionCall(Message message, Map<String, Object> body) {
        final String name      = MapUtils.get(body, "name");
        final String clientId  = MapUtils.get(body, "clientId");
        final String sessionId = MapUtils.get(body, "sessionId");
        final Resources resources = this.context.getResources();
        final SessionClient sessionClient = new SessionClient(
            sessionId, name, clientId, this, this.mainHandler,
            resources.getBoolean(R.bool.preview),
            resources.getBoolean(R.bool.playAudio),
            resources.getBoolean(R.bool.playVideo),
            resources.getBoolean(R.bool.dataConsume),
            resources.getBoolean(R.bool.audioConsume),
            resources.getBoolean(R.bool.videoConsume),
            resources.getBoolean(R.bool.dataProduce),
            resources.getBoolean(R.bool.audioProduce),
            resources.getBoolean(R.bool.videoProduce),
            this.mediaManager.getMediaProperties(),
            this.mediaManager.getWebrtcProperties()
        );
        this.sessions.put(sessionId, sessionClient);
        sessionClient.offer();
    }

    /**
     * 关闭媒体信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void sessionClose(Message message, Map<String, Object> body) {
        final String sessionId            = MapUtils.get(body, "sessionId");
        final SessionClient sessionClient = this.sessions.remove(sessionId);
        if(sessionClient == null) {
            Log.w(Taoyao.class.getSimpleName(), "关闭媒体（无效会话）：" + sessionId);
            return;
        }
        sessionClient.close();
    }

    /**
     * 媒体交换信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void sessionExchange(Message message, Map<String, Object> body) {
        final String sessionId            = MapUtils.get(body, "sessionId");
        final SessionClient sessionClient = this.sessions.get(sessionId);
        if(sessionClient == null) {
            Log.w(Taoyao.class.getSimpleName(), "媒体交换（无效会话）：" + sessionId);
            return;
        }
        sessionClient.exchange(message, body);
    }

    /**
     * 暂停媒体信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void sessionPause(Message message, Map<String, Object> body) {
        final String sessionId            = MapUtils.get(body, "sessionId");
        final SessionClient sessionClient = this.sessions.get(sessionId);
        if(sessionClient == null) {
            Log.w(Taoyao.class.getSimpleName(), "暂停媒体（无效会话）：" + sessionId);
            return;
        }
        final String type = MapUtils.get(body, "type");
        sessionClient.pauseLocal(type);
    }

    /**
     * 恢复媒体信令
     *
     * @param message 信令消息
     * @param body    信令主体
     */
    private void sessionResume(Message message, Map<String, Object> body) {
        final String sessionId            = MapUtils.get(body, "sessionId");
        final SessionClient sessionClient = this.sessions.get(sessionId);
        if(sessionClient == null) {
            Log.w(Taoyao.class.getSimpleName(), "恢复媒体（无效会话）：" + sessionId);
            return;
        }
        final String type = MapUtils.get(body, "type");
        sessionClient.resumeLocal(type);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return this.wifiManager.calculateSignalLevel(wifiInfo.getRssi()) / this.wifiManager.getMaxSignalLevel() * 100;
        } else {
            // 0   ~ -50 : 优秀
            // -50 ~ -70 : 良好
            // -70 ~ -100: 较差
            final int rssi = wifiInfo.getRssi();
            return rssi <= -100 ? 0 : (100 + rssi);
        }
    }

    /**
     * @return 电量信息
     */
    private int battery() {
        return this.batteryManager == null ? -1 : this.batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    /**
     * @return 充电状态
     */
    private boolean charging() {
        return this.batteryManager == null ? false : this.batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    /**
     * @return 位置信息
     */
    @SuppressLint("MissingPermission")
    private Location location() {
        if (this.locationManager == null) {
            return null;
        }
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(false);
        criteria.setBearingRequired(false);
        criteria.setAltitudeRequired(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        final String provider = this.locationManager.getBestProvider(criteria, true);
        if (provider == null) {
            return null;
        }
        return this.locationManager.getLastKnownLocation(provider);
    }

}
