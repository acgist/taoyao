package com.acgist.taoyao.boot.config;

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
	 * 状态
	 */
	String STATUS = "status";
	/**
	 * 配置
	 */
	String CONFIG = "config";
	/**
	 * 纬度
	 */
	String LATITUDE = "latitude";
	/**
	 * 经度
	 */
	String LONGITUDE = "longitude";
	/**
	 * 湿度
	 */
	String HUMIDITY = "humidity";
	/**
	 * 温度
	 */
	String TEMPERATURE = "temperature";
	/**
	 * 信号强度（0~100）
	 */
	String SIGNAL = "signal";
	/**
	 * 电池电量（0~100）
	 */
	String BATTERY = "battery";
	/**
	 * 是否正在运行
	 */
	String RUNNING = "running";
	/**
	 * 是否正在充电
	 */
	String CHARGING = "charging";
	/**
	 * 是否正在录像
	 */
	String RECORDING = "recording";
	/**
	 * 地址
	 */
	String URLS = "urls";
	/**
	 * 凭证
	 */
	String CREDENTIAL = "credential";
	/**
	 * 最小
	 */
	String MIN = "min";
	/**
	 * 最大
	 */
	String MAX = "max";
	/**
	 * 建议
	 */
	String IDEAL = "ideal";
	/**
	 * 最小宽度
	 */
	Integer MIN_WIDTH = 720;
	/**
	 * 最大宽度
	 */
	Integer MAX_WIDTH = 4096;
	/**
	 * 最小高度
	 */
	Integer MIN_HEIGHT = 480;
	/**
	 * 最大高度
	 */
	Integer MAX_HEIGHT = 2160;
	/**
	 * 名称
	 */
	String NAME = "name";
	/**
	 * 类型
	 */
	String CLIENT_TYPE = "clientType";
	/**
	 * 脚本
	 */
	String SCRIPT = "script";
	/**
	 * 结果
	 */
	String RESULT = "result";
	/**
	 * 消息
	 */
	String MESSAGE = "message";
	/**
	 * 请求
	 */
	String REQUEST = "request";
	/**
	 * 帐号
	 */
	String USERNAME = "username";
	/**
	 * 密码
	 */
	String PASSWORD = "password";
	/**
	 * 媒体类型
	 */
	String KIND = "kind";
	/**
	 * 媒体
	 */
	String MEDIA = "media";
	/**
	 * WebRTC
	 */
	String WEBRTC = "webrtc";
	/**
	 * 日期时间
	 */
	String DATETIME = "datetime";
	/**
	 * 房间ID
	 */
	String ROOM_ID = "roomId";
	/**
	 * 媒体服务ID
	 */
	String MEDIA_ID = "mediaId";
	/**
	 * 媒体流ID
	 */
	String STREAM_ID = "streamId";
	/**
	 * 终端ID
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
	/**
	 * ICE服务：P2P直连使用
	 */
	String ICE_SERVERS = "iceServers";
	/**
	 * ICE候选
	 */
	String ICE_CANDIDATES = "iceCandidates";
	/**
	 * ICE参数
	 */
	String ICE_PARAMETERS = "iceParameters";
	/**
	 * DTLS参数
	 */
	String DTLS_PARAMETERS = "dtlsParameters";
	/**
	 * SCTP参数
	 */
	String SCTP_PARAMETERS = "sctpParameters";
	/**
	 * RTP能力
	 */
	String RTP_CAPABILITIES = "rtpCapabilities";
	/**
	 * SCTP能力
	 */
	String SCTP_CAPABILITIES = "sctpCapabilities";
	/**
	 * 生产者
	 */
    String CONSUMING = "consuming";
    /**
     * 消费者
     */
    String PRODUCING = "producing";

}
