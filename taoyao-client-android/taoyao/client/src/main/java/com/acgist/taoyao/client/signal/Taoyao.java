package com.acgist.taoyao.client.signal;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.CloseableUtils;
import com.acgist.taoyao.boot.utils.JSONUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 桃夭信令
 *
 * @author acgist
 */
public class Taoyao {

//    private static final HiLogLabel label = new HiLogLabel(HiLog.LOG_APP, 0, "[信令]");

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(8);

    /**
     * 端口
     */
    private int port;
    /**
     * 地址
     */
    private String host;
    /**
     * Socket
     */
    private Socket socket;
    private InputStream input;
    private OutputStream output;
    private String username;
    private String password;
    private boolean close;
    /**
     * 是否连接
     */
    private boolean connect;
    private final Cipher encrypt;
    private final Cipher decrypt;

    public Taoyao(int port, String host, String algo, String secret) {
        this.port = port;
        this.host = host;
        this.close = false;
        this.connect = false;
        if(algo == null || algo.isEmpty() || algo.equals("PLAINTEXT")) {
            // 明文
            this.encrypt = null;
            this.decrypt = null;
        } else {
            this.encrypt = this.buildCipher(Cipher.ENCRYPT_MODE, algo, secret);
            this.decrypt = this.buildCipher(Cipher.DECRYPT_MODE, algo, secret);
        }
        EXECUTOR.submit(this::read);
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
//            socket.setSoTimeout(5000);
            this.socket.connect(new InetSocketAddress(this.host, this.port), 5000);
            if(this.socket.isConnected()) {
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

    private void read() {
        int length = 0;
        short messageLength = 0;
        final byte[] bytes = new byte[1024];
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while(!this.close) {
            try {
                while(this.input == null) {
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
                                EXECUTOR.submit(() -> {
                                    Taoyao.this.on(content);
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
     *
     * @return 加密消息
     */
    private byte[] encrypt(Message message) {
        final byte[] bytes = message.toString().getBytes();
        if(this.encrypt != null) {
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
        if(this.output == null) {
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

    private void register() {
        final Header header = new Header();
        this.push(this.buildMessage(
                "client::register",
                "clientId", "harmony",
                "name", "harmony",
                "clientType", "camera",
                "username", "taoyao",
                "password","taoyao"
        ));
    }

    private void close() {
        this.connect = false;
        CloseableUtils.close(this.input);
        CloseableUtils.close(this.output);
        CloseableUtils.close(this.socket);
        this.input = null;
        this.output = null;
        this.socket = null;
    }

    private void shutdown() {
        this.close();
        this.close = true;
        EXECUTOR.shutdownNow();
    }

    /**
     * 当前索引
     */
    private int index;
    /**
     * 当前终端索引
     */
    private int clientIndex = 99999;

    private static final int MAX_INDEX = 999;

    public long buildId() {
        int index;
        synchronized (this) {
            if (++this.index > MAX_INDEX) {
                this.index = 0;
            }
            index = this.index;
        }
        final LocalDateTime time = LocalDateTime.now();
        return
            100000000000000L * time.getDayOfMonth() +
            1000000000000L   * time.getHour()       +
            10000000000L     * time.getMinute()     +
            100000000L       * time.getSecond()     +
            1000000L         * this.clientIndex     +
            index;
    }

    private String version;

    public Message buildMessage(String signal, Object ... args) {
        final Map<Object, Object> map = new HashMap<>();
        if(args != null) {
            for (int index = 0; index < args.length; index+=2) {
                map.put(args[index], args[index + 1]);
            }
        }
        return this.buildMessage(signal, map);
    }

    public Message buildMessage(String signal, Object body) {
        final Header header = new Header();
        header.setV(this.version == null ? "1.0.0" : this.version);
        header.setId(this.buildId());
        header.setSignal(signal);
        final Message message = new Message();
        message.setHeader(header);
        message.setBody(body == null ? new HashMap<>() : body);
        return message;
    }

    private void on(String content) {
        // TODO：日志
//                                    log.debug("收到消息：{}", new String(this.decrypt.doFinal(message)));
        System.out.println(content);
        final Message message = JSONUtils.toJava(content, Message.class);
        if(message == null) {
            return;
        }
        final Header header = message.getHeader();
        if(header == null) {
            return;
        }
        final Map<String, Object> body = message.body();
        switch (header.getSignal()) {
            case "client::register":
                this.register(message, body);
                break;
            default:
                break;
        }
    }

    private void register(Message message, Map<String, Object> body) {
        final Integer index = (Integer) body.get("index");
        this.clientIndex = index;
        System.out.println(clientIndex);
    }

}
