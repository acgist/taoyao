package com.acgist.taoyao.client.signal;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.util.Log;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.client.media.Recorder;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /**
     * 端口
     */
    private final int port;
    /**
     * 地址
     */
    private final String host;
    /**
     * 终端ID
     */
    private final String clientId;
    private final String clientType = "camera";
    /**
     * 终端名称
     */
    private final String name;
    /**
     * 桃夭帐号
     */
    private final String username;
    /**
     * 桃夭密码
     */
    private final String password;
    private String version;
    /**
     * Socket
     */
    private Socket socket;
    private InputStream input;
    private OutputStream output;
    private boolean close;
    /**
     * 是否连接
     */
    private boolean connect;
    private final Cipher encrypt;
    private final Cipher decrypt;
    private final WifiManager wifiManager;
    private final BatteryManager batteryManager;
    private final LocationManager locationManager;
    // 线程池
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    // 定时任务线程池
    private final ExecutorService scheduled = Executors.newScheduledThreadPool(2);

    public Taoyao(
        int port, String host, String algo, String secret, String clientId, String name, String username, String password,
        WifiManager wifiManager, BatteryManager batteryManager, LocationManager locationManager
    ) {
        this.port = port;
        this.host = host;
        this.close = false;
        this.connect = false;
        if (algo == null || algo.isEmpty() || algo.equals("PLAINTEXT")) {
            // 明文
            this.encrypt = null;
            this.decrypt = null;
        } else {
            this.encrypt = this.buildCipher(Cipher.ENCRYPT_MODE, algo, secret);
            this.decrypt = this.buildCipher(Cipher.DECRYPT_MODE, algo, secret);
        }
        this.clientId = clientId;
        this.name = name;
        this.username = username;
        this.password = password;
        this.wifiManager = wifiManager;
        this.batteryManager = batteryManager;
        this.locationManager = locationManager;
        executor.submit(this::read);
        scheduled.submit(this::heartbeat);
    }

    private Cipher buildCipher(int mode, String name, String secret) {
        try {
            final String algo = name.equals("DES") ? "DES/ECB/PKCS5Padding" : "AES/ECB/PKCS5Padding";
            final Cipher cipher = Cipher.getInstance(algo);
            cipher.init(mode, new SecretKeySpec(Base64.getMimeDecoder().decode(secret), name));
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            // TODO：日志
        }
        return null;
    }

    /**
     * 连接信令
     */
    public void connect() {
        this.close();
//        HiLog.debug(this.label, "连接信令：%s:%d", this.host, this.port);
        this.socket = new Socket();
        try {
//          socket.setSoTimeout(5000);
            this.socket.connect(new InetSocketAddress(this.host, this.port), 5000);
            if (this.socket.isConnected()) {
                this.input = this.socket.getInputStream();
                this.output = this.socket.getOutputStream();
                this.register();
                synchronized (this) {
                    this.notifyAll();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
//            HiLog.error(this.label, "连接信令异常：%s:%d", this.host, this.port);
        }
    }

    private void heartbeat() {

    }

    private void read() {
        int length = 0;
        short messageLength = 0;
        final byte[] bytes = new byte[1024];
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while (!this.close) {
            try {
                while (this.input == null) {
                    this.connect();
                    synchronized (this) {
                        this.wait(5000);
                    }
                }
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
                e.printStackTrace();
                this.connect();
                // TODO：日志
//                    log.error("读取异常", e);
            }
        }
    }

    /**
     * @param message 消息
     * @return 加密消息
     */
    private byte[] encrypt(Message message) {
        final byte[] bytes = message.toString().getBytes();
        if (this.encrypt != null) {
            try {
                // 加密
                final byte[] encryptBytes = this.encrypt.doFinal(bytes);
                // 发送
                final ByteBuffer buffer = ByteBuffer.allocateDirect(Short.BYTES + encryptBytes.length);
                buffer.putShort((short) encryptBytes.length);
                buffer.put(encryptBytes);
                buffer.flip();
                final byte[] sendBytes = new byte[buffer.capacity()];
                buffer.get(sendBytes);
                return sendBytes;
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
//                log.error("加密异常：{}", message);
            }
        }
        return bytes;
    }

    public void push(Message message) {
        if (this.output == null) {
            return;
        }
        try {
            this.output.write(this.encrypt(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Message request(Message request) {
        return null;
    }

    /**
     * 释放连接
     */
    private void close() {
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
    private void shutdown() {
        this.close();
        this.close = true;
        executor.shutdownNow();
        scheduled.shutdownNow();
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
        final Map<String, Object> body = message.body();
        switch (header.getSignal()) {
            case "client::register" -> this.register(message, body);
            default -> Log.i(Taoyao.class.getSimpleName(), "没有适配信令：" + content);
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
            "signal", this.wifiManager == null ? -1 : this.wifiManager.getMaxSignalLevel(),
            "batter", this.batteryManager == null ? -1 : this.batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            "charging", this.batteryManager == null ? -1 : this.batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS),
            "recording", Recorder.getInstance().isActive()
        ));
    }

    /**
     * @param message 消息
     * @param body    消息主体
     */
    private void register(Message message, Map<String, Object> body) {
        final Integer index = (Integer) body.get("index");
        IdUtils.setClientIndex(index);
    }

    /**
     * @return 位置
     */
    private Location location() {
        if(this.locationManager == null) {
            return null;
        }
        final Criteria criteria = new Criteria();
        // 耗电
        criteria.setCostAllowed(false);
        // 不要海拔
        criteria.setAltitudeRequired(false);
        // 不要方位
        criteria.setBearingRequired(false);
        // 精度
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 低功耗
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        final String provider = locationManager.getBestProvider(criteria, true);
        return this.locationManager.getLastKnownLocation(provider);
    }

}
