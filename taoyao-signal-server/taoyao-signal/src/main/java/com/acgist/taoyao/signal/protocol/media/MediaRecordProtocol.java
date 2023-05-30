package com.acgist.taoyao.signal.protocol.media;

import java.util.Map;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.config.FfmpegProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.party.media.ClientWrapper;
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
        if(enabled) {
            this.record(room, client);
        }
        return null;
    }
    
    /**
     * 开始录制
     */
    private void record(Room room, Client client) {
        final ClientWrapper clientWrapper = room.clientWrapper(client);
        synchronized (clientWrapper) {
            if(clientWrapper.getRecorder() != null) {
                return;
            }
            final Recorder recorder = new Recorder(this.ffmpegProperties);
            recorder.start();
            clientWrapper.setRecorder(recorder);
        }
    }
    
}
