package com.acgist.taoyao.rtp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

import javax.crypto.Cipher;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.acgist.taoyao.boot.config.SocketProperties.Encrypt;
import com.acgist.taoyao.boot.model.MessageCodeException;
import com.acgist.taoyao.signal.utils.CipherUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RtpTest {

    @Test
    void testSocket() throws Exception {
        final Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 9999));
        final InputStream inputStream = socket.getInputStream();
        final OutputStream outputStream = socket.getOutputStream();
        // 随机密码：https://localhost:8888/config/socket
        final String secret = "TSFXzB7hcfE=".strip();
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
                                log.debug("收到消息：{}", new String(decrypt.doFinal(message)));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("读取异常", e);
            }
        }).start();
        // 发送
        String line = """
        {
            "header":{"v":"1.0.0","id":1215293599999001,"signal":"client::register"},
            "body":{"clientId":"ffmpeg","name":"ffmpeg","clientType":"WEB","battery":100,"charging":true,"username":"taoyao","password":"taoyao"}
        }
        """;
        // {"header":{"v":"1.0.0","id":1215310510002009,"signal":"room::enter"},"body":{"roomId":"8260e615-3081-4bfc-96a8-574f4dd780d9"}}
        // {"header":{"v":"1.0.0","id":1215310510002010,"signal":"media::transport::plain"},"body":{"roomId":"8260e615-3081-4bfc-96a8-574f4dd780d9","rtcpMux":false,"comedia":true}}
        // {"header":{"v":"1.0.0","id":1215375110006012,"signal":"media::produce"},"body":{"kind":"video","roomId":"8260e615-3081-4bfc-96a8-574f4dd780d9","transportId":"14dc9307-bf9c-4442-a9ad-ce6a97623ef4","appData":{},"rtpParameters":{"codecs":[{"mimeType":"video/vp8","clockRate":90000,"payloadType":102,"rtcpFeedback":[]}],"encodings":[{"ssrc":123123}]}}}
        // 音频转为PCM
//      ffmpeg.exe -i .\a.m4a -f s16le a.pcm
//      ffmpeg.exe -i .\a.m4a -f s16le -ac 2 -ar 8000 a.pcm
//      ffplay.exe -ar 48000 -ac 2 -f s16le -i a.pcm
        // ffmpeg不支持rtcpMux
//      ffmpeg -re -i video.mp4 -c:v vp8    -map 0:0 -f tee "[select=v:f=rtp:ssrc=123123:payload_type=102]rtp://192.168.1.110:40793?rtcpport=47218"
//      ffmpeg -re -i video.mp4 -c:v libvpx -map 0:0 -f tee "[select=v:f=rtp:ssrc=123123:payload_type=102]rtp://192.168.1.110:40793?rtcpport=47218"
        // 音频视频同时传输
//      ffmpeg -re -i video.mp4 -c:a libopus -vn -f rtp rtp://192.168.1.110:8888 -c:v libx264 -an -f rtp rtp://192.168.1.110:9999 -sdp_file taoyao.sdp
//      ffplay -protocol_whitelist "file,rtp,udp" -i taoyao.sdp
//      ffmpeg -protocol_whitelist "file,rtp,udp" -i taoyao.sdp taoyao.mp4
        final Scanner scanner = new Scanner(System.in);
        do {
            if(StringUtils.isEmpty(line)) {
                break;
            }
            try {
                final byte[] bytes = line.getBytes();
                final byte[] encryptBytes = encrypt.doFinal(bytes);
                final ByteBuffer buffer = ByteBuffer.allocateDirect(Short.BYTES + encryptBytes.length);
                buffer.putShort((short) encryptBytes.length);
                buffer.put(encryptBytes);
                buffer.flip();
                final byte[] message = new byte[buffer.capacity()];
                buffer.get(message);
                outputStream.write(message);
            } catch (Exception e) {
                log.error("发送异常", e);
            }
        } while((line = scanner.next()) != null);
        socket.close();
        scanner.close();
    }
    
}
