package com.acgist.taoyao.client.signal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCode;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.media.MediaRecorder;
import com.acgist.taoyao.client.utils.IdUtils;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
public final class Taoyao {

    private static final long MAX_TIMEOUT = 60L * 1000;

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
     * 重试次数
     */
    private int connectRetryTimes;
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
    private final Handler handler;
    /**
     * 服务上下文
     */
    private final Context context;
    /**
     * Wifi管理器
     */
    private final WifiManager wifiManager;
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
    /**
     * 线程池
     */
    private final ExecutorService executor;
    /**
     * 定时任务线程池
     */
    private final ScheduledExecutorService scheduled;

    public Taoyao(
        int port, String host, String version,
        String name, String clientId, String clientType, String username, String password,
        int timeout, String algo, String secret,
        Handler handler, Context context,
        WifiManager wifiManager, BatteryManager batteryManager, LocationManager locationManager
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
        this.connectRetryTimes = 1;
        final boolean plaintext = algo == null || algo.isEmpty() || algo.equals("PLAINTEXT");
        this.encrypt = plaintext ? null : this.buildCipher(Cipher.ENCRYPT_MODE, algo, secret);
        this.decrypt = plaintext ? null : this.buildCipher(Cipher.DECRYPT_MODE, algo, secret);
        this.handler = handler;
        this.context = context;
        this.wifiManager = wifiManager;
        this.batteryManager = batteryManager;
        this.locationManager = locationManager;
        this.requestMessage = new ConcurrentHashMap<>();
        // 读取线程 + 两条处理线程
        this.executor = Executors.newFixedThreadPool(3);
        // 心跳线程
        this.scheduled = Executors.newScheduledThreadPool(1);
        this.executor.submit(this::loopMessage);
        this.scheduled.scheduleWithFixedDelay(this::heartbeat, 30, 30, TimeUnit.SECONDS);
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
                this.register();
                this.connect = true;
                this.connectRetryTimes = 1;
                synchronized (this) {
                    this.notifyAll();
                }
            } else {
                this.connect = false;
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
                    synchronized (this) {
                        try {
                            long timeout = this.timeout;
                            if(MAX_TIMEOUT > this.timeout * this.connectRetryTimes) {
                                timeout = this.timeout * this.connectRetryTimes++;
                            } else {
                                timeout = MAX_TIMEOUT;
                            }
                            this.wait(timeout);
                        } catch (InterruptedException e) {
                            Log.d(Taoyao.class.getSimpleName(), "信令等待异常", e);
                        }
                    }
                }
                // 读取
                while ((length = this.input.read(bytes)) >= 0) {
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
                                Log.d(Taoyao.class.getSimpleName(), "处理信令：" + content);
                                executor.submit(() -> {
                                    try {
                                        Taoyao.this.on(content);
                                    } catch (Exception e) {
                                        Log.e(Taoyao.class.getSimpleName(), "处理信令异常：" + content, e);
                                    }
                                });
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
            throw MessageCodeException.of(MessageCode.CODE_2001, "请求信令没有响应");
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
        this.executor.shutdown();
        this.scheduled.shutdown();
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
        Log.d(Taoyao.class.getSimpleName(), "收到消息：" + content);
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
            final Map<String, Object> body = message.body();
            switch (header.getSignal()) {
                case "client::register" -> this.register(message, body);
                default -> Log.i(Taoyao.class.getSimpleName(), "没有适配信令：" + content);
            }
        }
    }

    /**
     * 注册
     */
    private void register() {
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
            "signal", this.wifiSignal(),
            "battery", this.battery(),
            "charging", this.charging(),
            "recording", MediaRecorder.getInstance().isActive()
        ));
    }

    /**
     * @param message 消息
     * @param body    消息主体
     */
    private void register(Message message, Map<String, Object> body) {
        final Integer index = (Integer) body.get("index");
        if (index == null) {
            return;
        }
        IdUtils.setClientIndex(index);
    }

    /**
     * 心跳
     */
    private void heartbeat() {
        if(this.close || !this.connect) {
            return;
        }
        final Location location = this.location();
        this.push(this.buildMessage(
            "client::heartbeat",
            "latitude", location == null ? -1 : location.getLatitude(),
            "longitude", location == null ? -1 : location.getLongitude(),
            "signal", this.wifiSignal(),
            "battery", this.battery(),
            "charging", this.charging(),
            "recording", MediaRecorder.getInstance().isActive()
        ));
    }

    /**
     * @return 电量百分比
     */
    private int battery() {
        return this.batteryManager == null ? -1 : this.batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
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
    private int wifiSignal() {
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
        // 功耗
        criteria.setCostAllowed(false);
        // 不要海拔
        criteria.setAltitudeRequired(false);
        // 不要方位
        criteria.setBearingRequired(false);
        // 精度
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 功耗
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        // 最佳提供者
        final String provider = this.locationManager.getBestProvider(criteria, true);
        if (provider == null) {
            return null;
        }
        return this.locationManager.getLastKnownLocation(provider);
    }

}
