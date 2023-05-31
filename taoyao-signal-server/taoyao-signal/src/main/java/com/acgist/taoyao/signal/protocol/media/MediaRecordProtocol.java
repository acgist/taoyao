package com.acgist.taoyao.signal.protocol.media;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.MapUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
import com.acgist.taoyao.signal.party.media.Kind;
import com.acgist.taoyao.signal.party.media.Recorder;
import com.acgist.taoyao.signal.party.media.Room;
import com.acgist.taoyao.signal.protocol.ProtocolRoomAdapter;

/**
 * 媒体录像
 * 
 * @author acgist
 */
@Protocol
@Description(
    body = {
        """
        {
            "clientId": "目标终端ID",
            "enabled": 是否录像（true|false）
        }
        """,
        """
        {
            "enabled": 是否录像（true|false）,
            "filepath": "视频文件路径"
        }
        """
    },
    flow = "终端=>信令服务->终端"
)
public class MediaRecordProtocol extends ProtocolRoomAdapter {

    private final FfmpegProperties ffmpegProperties;
    
    public MediaRecordProtocol(FfmpegProperties ffmpegProperties) {
        super("媒体录像", "media::record");
        this.ffmpegProperties = ffmpegProperties;
    }

    @Override
    public void execute(String clientId, ClientType clientType, Room room, Client client, Client mediaClient, Message message, Map<String, Object> body) {
        final Boolean enabled = MapUtils.get(body, Constant.ENABLED, Boolean.TRUE);
        String filepath;
        if(enabled) {
            filepath = this.start(room, client, mediaClient);
        } else {
            filepath = this.stop(room, client, mediaClient);
        }
        body.put(Constant.FILEPATH, filepath);
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
        final Room room = this.roomManager.room(roomId);
        final Client client = this.clientManager.clients(clientId);
        final Client mediaClient = room.getMediaClient();
        String filepath;
        if(enabled) {
            filepath = this.start(room, client, mediaClient);
        } else {
            filepath = this.stop(room, client, mediaClient);
        }
        return Message.success(Map.of(
            Constant.ENABLED,  enabled,
            Constant.FILEPATH, filepath
        ));
    }
    
    /**
     * 开始录制
     * 
     * @param room        房间
     * @param client      终端
     * @param mediaClient 媒体终端
     * 
     * @return 文件地址
     */
    private String start(Room room, Client client, Client mediaClient) {
        final ClientWrapper clientWrapper = room.clientWrapper(client);
        synchronized (clientWrapper) {
            final Recorder recorder = clientWrapper.getRecorder();
            if(recorder != null) {
                return recorder.getFilepath();
            }
        }
        // 打开录制线程
        final Recorder recorder = new Recorder(UUID.randomUUID().toString(), this.ffmpegProperties);
        recorder.start();
        clientWrapper.setRecorder(recorder);
        // 打开媒体录制
        final Message message = this.build();
        final Map<String, Object> body = new HashMap<>();
        body.put("audioPort", recorder.getAudioPort());
        body.put("videoPort", recorder.getVideoPort());
        body.put(Constant.HOST, this.ffmpegProperties.getHost());
        body.put(Constant.ROOM_ID, room.getRoomId());
        body.put(Constant.ENABLED, true);
        body.put(Constant.CLIENT_ID, client.clientId());
        body.put(Constant.RTP_CAPABILITIES, clientWrapper.getRtpCapabilities());
        clientWrapper.getProducers().values().forEach(producer -> {
            if(producer.getKind() == Kind.AUDIO) {
                recorder.setAudioStreamId(Constant.STREAM_ID_CONSUMER.apply(producer.getStreamId(), client.clientId()));
                body.put("audioStreamId", recorder.getAudioStreamId());
                body.put("audioProducerId", producer.getProducerId());
            } else if(producer.getKind() == Kind.VIDEO) {
                recorder.setAudioStreamId(Constant.STREAM_ID_CONSUMER.apply(producer.getStreamId(), client.clientId()));
                body.put("videoStreamId", recorder.getVideoStreamId());
                body.put("videoProducerId", producer.getProducerId());
            } else {
                // 忽略
            }
        });
        message.setBody(body);
        mediaClient.request(message);
        return recorder.getFilepath();
    }

    /**
     * 关闭录像
     * 
     * @param room        房间
     * @param client      终端
     * @param mediaClient 媒体终端
     * 
     * @return 文件地址
     */
    private String stop(Room room, Client client, Client mediaClient) {
        final Recorder recorder;
        final ClientWrapper clientWrapper = room.clientWrapper(client);
        synchronized (clientWrapper) {
            recorder = clientWrapper.getRecorder();
            if(recorder == null) {
                return null;
            }
        }
        // 关闭录制线程
        recorder.stop();
        clientWrapper.setRecorder(null);
        // 关闭媒体录制
        final Message message = this.build();
        final Map<String, Object> body = new HashMap<>();
        body.put("audioStreamId", recorder.getAudioStreamId());
        body.put("videoStreamId", recorder.getVideoStreamId());
        body.put("audioConsumerId", recorder.getAudioConsumerId());
        body.put("videoConsumerId", recorder.getVideoConsumerId());
        body.put("audioTransportId", recorder.getAudioTransportId());
        body.put("videoTransportId", recorder.getVideoConsumerId());
        body.put(Constant.ROOM_ID, room.getRoomId());
        body.put(Constant.ENABLED, false);
        message.setBody(body);
        mediaClient.request(message);
        return recorder.getFilepath();
    }
    
}
