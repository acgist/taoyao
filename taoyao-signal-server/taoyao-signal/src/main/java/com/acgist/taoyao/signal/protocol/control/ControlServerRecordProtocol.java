package com.acgist.taoyao.signal.protocol.control;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationListener;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.event.room.RecorderCloseEvent;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Kind;
import com.acgist.taoyao.signal.party.media.Recorder;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolControlAdapter;

/**
 * 服务端录像信令
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "to"     : "目标终端ID",
            "roomId" : "房间ID",
            "enabled": 是否录像（true|false）
        }
        """,
        """
        {
            "roomId"  : "房间ID",
            "enabled" : 是否录像（true|false）,
            "filepath": "视频文件路径",
            "clientId": "录像终端ID"
        }
        """
    },
    flow = "终端=>信令服务->终端"
)
public class ControlServerRecordProtocol extends ProtocolControlAdapter implements ApplicationListener<RecorderCloseEvent> {

    public static final String SIGNAL = "control::server::record";
    
    private final FfmpegProperties ffmpegProperties;
    
    public ControlServerRecordProtocol(FfmpegProperties ffmpegProperties) {
        super("服务端录像信令", SIGNAL);
        this.ffmpegProperties = ffmpegProperties;
    }
    
    @Override
    public void onApplicationEvent(RecorderCloseEvent event) {
        final Recorder recorder = event.getRecorder();
        this.stop(recorder.getRoom(), recorder.getClientWrapper());
    }

    @Override
    public void execute(String clientId, ClientType clientType, Client client, Client targetClient, Message message, Map<String, Object> body) {
        String filepath;
        final String roomId   = MapUtils.get(body, Constant.ROOM_ID);
        final Boolean enabled = MapUtils.get(body, Constant.ENABLED, Boolean.TRUE);
        final Room room = this.roomManager.room(roomId);
        if(enabled) {
            filepath = this.start(room, room.clientWrapper(client));
        } else {
            filepath = this.stop(room, room.clientWrapper(client));
        }
        body.put(Constant.FILEPATH, filepath);
        body.put(Constant.CLIENT_ID, clientId);
        client.push(message);
    }

    /**
     * @param roomId   房间ID
     * @param clientId 终端ID
     * @param enabled  状态
     * 
     * @return 执行结果
     */
    public Message execute(String roomId, String clientId, Boolean enabled) {
        String filepath;
        final Room room     = this.roomManager.room(roomId);
        final Client client = this.clientManager.clients(clientId);
        if(enabled) {
            filepath = this.start(room, room.clientWrapper(client));
        } else {
            filepath = this.stop(room, room.clientWrapper(client));
        }
        return Message.success(Map.of(
            Constant.ROOM_ID,   roomId,
            Constant.ENABLED,   enabled,
            Constant.FILEPATH,  filepath,
            Constant.CLIENT_ID, clientId
        ));
    }
    
    /**
     * 开始录像
     * 
     * @param room          房间
     * @param clientWrapper 终端
     * @param mediaClient   媒体终端
     * 
     * @return 文件地址
     */
    private String start(Room room, ClientWrapper clientWrapper) {
        synchronized (clientWrapper) {
            final Recorder recorder = clientWrapper.getRecorder();
            if(recorder != null) {
                return recorder.getFilepath();
            }
        }
        final String name = UUID.randomUUID().toString();
        // 打开录像线程
        final Recorder recorder = new Recorder(name, room, clientWrapper, this.ffmpegProperties);
        recorder.start();
        clientWrapper.setRecorder(recorder);
        // 打开媒体录像
        final Message message = this.build();
        final Map<String, Object> body = new HashMap<>();
        body.put(Constant.HOST, this.ffmpegProperties.getHost());
        body.put(Constant.ROOM_ID, room.getRoomId());
        body.put(Constant.ENABLED, true);
        body.put(Constant.FILEPATH, recorder.getFilepath());
        body.put(Constant.CLIENT_ID, clientWrapper.getClientId());
        body.put(Constant.AUDIO_PORT, recorder.getAudioPort());
        body.put(Constant.VIDEO_PORT, recorder.getVideoPort());
        body.put(Constant.AUDIO_RTCP_PORT, recorder.getAudioRtcpPort());
        body.put(Constant.VIDEO_RTCP_PORT, recorder.getVideoRtcpPort());
        body.put(Constant.RTP_CAPABILITIES, clientWrapper.getRtpCapabilities());
        clientWrapper.getProducers().values().forEach(producer -> {
            if(producer.getKind() == Kind.AUDIO) {
                recorder.setAudioStreamId(Constant.STREAM_ID_CONSUMER.apply(producer.getStreamId(), clientWrapper.getClientId()));
                body.put(Constant.AUDIO_STREAM_ID, recorder.getAudioStreamId());
                body.put(Constant.AUDIO_PRODUCER_ID, producer.getProducerId());
            } else if(producer.getKind() == Kind.VIDEO) {
                recorder.setAudioStreamId(Constant.STREAM_ID_CONSUMER.apply(producer.getStreamId(), clientWrapper.getClientId()));
                body.put(Constant.VIDEO_STREAM_ID, recorder.getVideoStreamId());
                body.put(Constant.VIDEO_PRODUCER_ID, producer.getProducerId());
            } else {
                // 忽略
            }
        });
        message.setBody(body);
        final Client mediaClient = room.getMediaClient();
        final Message response = mediaClient.request(message);
        final Map<String, String> responseBody = response.body();
        recorder.setAudioConsumerId(responseBody.get(Constant.AUDIO_CONSUMER_ID));
        recorder.setVideoConsumerId(responseBody.get(Constant.VIDEO_CONSUMER_ID));
        recorder.setAudioTransportId(responseBody.get(Constant.AUDIO_TRANSPORT_ID));
        recorder.setVideoTransportId(responseBody.get(Constant.VIDEO_TRANSPORT_ID));
        return recorder.getFilepath();
    }

    /**
     * 关闭录像
     * 
     * @param room          房间
     * @param clientWrapper 终端
     * 
     * @return 文件地址
     */
    private String stop(Room room, ClientWrapper clientWrapper) {
        final Recorder recorder;
        synchronized (clientWrapper) {
            recorder = clientWrapper.getRecorder();
            if(recorder == null) {
                return null;
            }
        }
        // 关闭录像线程
        recorder.stop();
        clientWrapper.setRecorder(null);
        // 关闭媒体录像
        final Message message = this.build();
        final Map<String, Object> body = new HashMap<>();
        body.put(Constant.ROOM_ID, room.getRoomId());
        body.put(Constant.ENABLED, false);
        body.put(Constant.AUDIO_STREAM_ID, recorder.getAudioStreamId());
        body.put(Constant.VIDEO_STREAM_ID, recorder.getVideoStreamId());
        body.put(Constant.AUDIO_CONSUMER_ID, recorder.getAudioConsumerId());
        body.put(Constant.VIDEO_CONSUMER_ID, recorder.getVideoConsumerId());
        body.put(Constant.AUDIO_TRANSPORT_ID, recorder.getAudioTransportId());
        body.put(Constant.VIDEO_TRANSPORT_ID, recorder.getVideoTransportId());
        message.setBody(body);
        final Client mediaClient = room.getMediaClient();
        mediaClient.request(message);
        return recorder.getFilepath();
    }
    
}
