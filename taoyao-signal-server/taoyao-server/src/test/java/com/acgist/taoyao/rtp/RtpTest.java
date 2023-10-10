package com.acgist.taoyao.rtp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.SocketProperties.Encrypt;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.boot.utils.JSONUtils;
import com.acgist.taoyao.boot.utils.ScriptUtils;
import com.acgist.taoyao.signal.utils.CipherUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * TODO：验证不能生产两个媒体
 */
@Slf4j
public class RtpTest {

    private String roomId = "79371adf-0f05-4852-a664-f00b1b77662d";
    private Map<String, Message> response = new HashMap<>();
    
    @Test
    void testSocket() throws Exception {
        final Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 9999));
        final InputStream  inputStream  = socket.getInputStream();
        final OutputStream outputStream = socket.getOutputStream();
        // 随机密码：https://localhost:8888/config/socket
        final String secret  = "2SPWy+TF1zM=".strip();
        final Cipher encrypt = CipherUtils.buildCipher(Cipher.ENCRYPT_MODE, Encrypt.DES, secret);
        final Cipher decrypt = CipherUtils.buildCipher(Cipher.DECRYPT_MODE, Encrypt.DES, secret);
        // 接收
        new Thread(() -> {
            int length = 0;
            short messageLength = 0;
            final byte[] bytes = new byte[1024];
            final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
            try {
                while((length = inputStream.read(bytes)) >= 0) {
                    buffer.put(bytes, 0, length);
                    while(buffer.position() > 0) {
                        if(messageLength <= 0) {
                            if(buffer.position() < Short.BYTES) {
                                // 不够消息长度
                                break;
                            } else {
                                buffer.flip();
                                messageLength = buffer.getShort();
                                buffer.compact();
                                if(messageLength > 16 * 1024) {
                                    throw MessageCodeException.of("超过最大数据大小：" + messageLength);
                                }
                            }
                        } else {
                            if(buffer.position() < messageLength) {
                                // 不够消息长度
                                break;
                            } else {
                                final byte[] message = new byte[messageLength];
                                messageLength = 0;
                                buffer.flip();
                                buffer.get(message);
                                buffer.compact();
                                final String value     = new String(decrypt.doFinal(message));
                                final Message response = JSONUtils.toJava(value, Message.class);
                                final String signal    = response.getHeader().getSignal();
                                if("media::audio::volume".equals(signal)) {
                                    log.debug("收到消息：{}", value);
                                } else {
                                    log.info("收到消息：{}", value);
                                }
                                this.response.put(signal, response);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("读取异常", e);
            }
        }).start();
        // ffmpeg不支持rtcpMux
        // 循环推流：-stream_loop 4 | -stream_loop -1
//      ffmpeg -re -i video.mp4 -c:v vp8    -map 0:0 -f tee "[select=v:f=rtp:ssrc=123456:payload_type=102]rtp://192.168.1.110:40793?rtcpport=47218"
//      ffmpeg -re -i video.mp4 -c:v libvpx -map 0:0 -f tee "[select=v:f=rtp:ssrc=123456:payload_type=102]rtp://192.168.1.110:40793?rtcpport=47218"
        // 音频视频同时传输
//      ffmpeg -re -i video.mp4 -c:a libopus -vn -f rtp rtp://192.168.1.110:8888 -c:v libx264 -an -f rtp rtp://192.168.1.110:9999 -sdp_file taoyao.sdp
//      ffplay -protocol_whitelist "file,rtp,udp" -i taoyao.sdp
//      ffmpeg -protocol_whitelist "file,rtp,udp" -i taoyao.sdp taoyao.mp4
        // 发送命令：register/enter/create/audio/video
        String command = "register";
        final Scanner scanner = new Scanner(System.in);
        do {
            if(StringUtils.isEmpty(command)) {
                break;
            }
            try {
                final byte[] bytes        = this.request(command).getBytes();
                final byte[] encryptBytes = encrypt.doFinal(bytes);
                final ByteBuffer buffer   = ByteBuffer.allocateDirect(Short.BYTES + encryptBytes.length);
                buffer.putShort((short) encryptBytes.length);
                buffer.put(encryptBytes);
                buffer.flip();
                final byte[] message = new byte[buffer.capacity()];
                buffer.get(message);
                outputStream.write(message);
            } catch (Exception e) {
                log.error("发送异常", e);
            }
        } while((command = scanner.next()) != null);
        socket.close();
        scanner.close();
    }
    
    private String request(String command) {
        return switch (command) {
        case "register"  -> this.register();
        case "enter"     -> this.enter();
        case "transport" -> this.transport();
        case "audio"     -> this.produceAudio();
        case "video"     -> this.produceVideo();
        default          -> null;
        };
    }
    
    private String register() {
      return """
      {
          "header": {
              "v"     : "1.0.0",
              "id"    : 1,
              "signal": "client::register"
          },
          "body"  : {
              "clientId"  : "ffmpeg",
              "name"      : "ffmpeg",
              "clientType": "WEB",
              "battery"   : 100,
              "charging"  : true,
              "username"  : "taoyao",
              "password"  : "taoyao"
          }
      }
      """;
    }
    
    private String enter() {
        return String.format("""
        {
            "header": {
                "v"     : "1.0.0",
                "id"    : 2,
                "signal": "room::enter"
            },
            "body"  : {
                "roomId": "%s"
            }
        }
        """, this.roomId);
    }
    
    private String transport() {
        return String.format("""
        {
            "header": {
                "v"     : "1.0.0",
                "id"    : 3,
                "signal": "media::transport::plain::create"},
            "body"  : {
                "roomId" : "%s",
                "rtcpMux": false,
                "comedia": true
            }
        }
        """, this.roomId);
    }
    
    private String produceAudio() {
        final Message message = this.response.get("media::transport::plain::create");
        final Map<String, String> body = message.body();
        final String transportId = body.get("transportId");
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                final Message produce = RtpTest.this.response.get("media::transport::plain::create");
                final Map<String, Object> map = produce.body();
                final String ip = map.get("ip").toString();
                final String port = map.get("port").toString();
                final String rtcpPort = map.get("rtcpPort").toString();
                final String command = String.format("ffmpeg -re -i D:\\tmp\\video.mp4 -c:a libopus -map 0:1 -f tee \"[select=a:f=rtp:ssrc=123456:payload_type=100]rtp://%s:%s?rtcpport=%s\"", ip, port, rtcpPort);
                log.info("执行命令：{}", command);
                ScriptUtils.execute(command);
            }
        }, 1000);
        return String.format("""
        {
            "header": {
                "v"     : "1.0.0",
                "id"    : 4,
                "signal": "media::produce"
            },
            "body"  : {
                "kind"       : "audio",
                "roomId"     : "%s",
                "transportId": "%s",
                "appData"    : {},
                "rtpParameters":{
                    "codecs"   : [{
                        "mimeType"   : "audio/opus",
                        "channels"   : 2,
                        "clockRate"  : 48000,
                        "payloadType": 100
                    }],
                    "encodings": [{
                        "ssrc": 123456
                    }]
                }
            }
        }
        """, this.roomId, transportId);
    }
    
    private String produceVideo() {
        final Message message = this.response.get("media::transport::plain::create");
        final Map<String, String> body = message.body();
        final String transportId = body.get("transportId");
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                final Message produce = RtpTest.this.response.get("media::transport::plain::create");
                final Map<String, Object> map = produce.body();
                final String ip = map.get("ip").toString();
                final String port = map.get("port").toString();
                final String rtcpPort = map.get("rtcpPort").toString();
                final String command = String.format("ffmpeg -re -i D:\\tmp\\video.mp4 -c:v libvpx -map 0:0 -f tee \"[select=v:f=rtp:ssrc=654321:payload_type=102]rtp://%s:%s?rtcpport=%s\"", ip, port, rtcpPort);
                log.info("执行命令：{}", command);
                ScriptUtils.execute(command);
            }
        }, 1000);
        return String.format("""
        {
            "header": {
                "v"     : "1.0.0",
                "id"    : 5,
                "signal": "media::produce"
            },
            "body"  : {
                "kind"       : "video",
                "roomId"     : "%s",
                "transportId": "%s",
                "appData"    : {},
                "rtpParameters":{
                    "codecs"   : [{
                        "mimeType"    : "video/vp8",
                        "clockRate"   : 90000,
                        "payloadType" : 102,
                        "rtcpFeedback":[]
                    }],
                    "encodings": [{
                        "ssrc": 654321
                    }]
                }
            }
        }
        """, this.roomId, transportId);
    }
    
}
