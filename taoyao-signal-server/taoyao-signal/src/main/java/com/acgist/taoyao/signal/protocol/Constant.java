package com.acgist.taoyao.signal.protocol;

/**
 * 字符常量
 * 
 * @author acgist
 */
public interface Constant {

    /**
     * 换行
     */
    String LINE = "\n";
	/**
	 * IP
	 */
	String IP = "ip";
	/**
	 * 接收方
	 */
	String TO = "to";
	/**
	 * 发送方
	 */
	String FROM = "from";
	/**
	 * 信号强度（0~100）
	 */
	String SIGNAL = "signal";
	/**
	 * 电池电量（0~100）
	 */
	String BATTERY = "battery";
	/**
	 * 是否充电
	 */
	String CHARGING = "charging";
	/**
	 * 名称
	 */
	String NAME = "name";
	/**
	 * 脚本
	 */
	String SCRIPT = "script";
	/**
	 * 结果
	 */
	String RESULT = "result";
	/**
	 * 帐号
	 */
	String USERNAME = "username";
	/**
	 * 密码
	 */
	String PASSWORD = "password";
	/**
	 * 时间
	 */
	String TIME = "time";
	/**
	 * 媒体
	 */
	String MEDIA = "media";
	/**
	 * WebRTC
	 */
	String WEBRTC = "webrtc";
	/**
	 * PeerId
	 * 
	 * @see #CLIENT_ID
	 */
	String PEER_ID = "peerId";
	/**
	 * 房间ID
	 */
	String ROOM_ID = "roomId";
	/**
	 * 媒体服务ID
	 */
	String MEDIA_ID = "mediaId";
	/**
	 * 终端ID
	 * 
	 * @see #PEER_ID
	 */
	String CLIENT_ID = "clientId";
	/**
	 * 路由ID
	 */
	String ROUTER_ID = "routerId";
	/**
	 * 传输通道ID
	 */
	String TRANSPORT_ID = "transportId";
	/**
	 * 生产者ID
	 */
	String PRODUCER_ID = "producerId";
	/**
	 * 消费者ID
	 */
	String CONSUMER_ID = "consumerId";
	/**
	 * 数据生产者ID
	 */
	String DATA_PRODUCER_ID = "dataProducerId";
	/**
	 * 数据消费者ID
	 */
	String DATA_CONSUMER_ID = "dataConsumerId";

}
