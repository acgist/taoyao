package com.acgist.taoyao.signal.event.room;

import com.acgist.taoyao.signal.event.RoomEventAdapter;
import com.acgist.taoyao.signal.party.media.Recorder;

import lombok.Getter;
import lombok.Setter;

/**
 * 服务端录像关闭事件
 * 
 * @author acgist
 */
@Getter
@Setter
public class RecorderCloseEvent extends RoomEventAdapter {

    private static final long serialVersionUID = 1L;
    
    /**
     * 媒体录像机
     */
    private final Recorder recorder;
    
    public RecorderCloseEvent(Recorder recorder) {
        super(recorder.getRoom());
        this.recorder = recorder;
    }
    
}
