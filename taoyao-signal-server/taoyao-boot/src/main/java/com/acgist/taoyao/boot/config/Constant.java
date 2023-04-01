package com.acgist.taoyao.boot.config;

import java.util.function.BiFunction;

/**
 * 常量
 * 
 * @author acgist
 */
public interface Constant {

    /**
     * 接收方的终端标识
     */
    String TO = "to";
    /**
     * IP
     */
    String IP = "ip";
    /**
     * 状态对象
     */
    String STATUS = "status";
    /**
     * 配置对象
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
     * 是否发生告警
     */
    String ALARMING = "alarming";
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
     * 响应
     */
    String RESPONSE = "response";
    /**
     * 帐号
     */
    String USERNAME = "username";
    /**
     * 密码
     */
    String PASSWORD = "password";
    /**
     * 数据
     */
    String DATA = "data";
    /**
     * 名称
     */
    String NAME = "name";
    /**
     * 媒体类型
     */
    String KIND = "kind";
    /**
     * 索引
     */
    String INDEX = "index";
    /**
     * 媒体
     */
    String MEDIA = "media";
    /**
     * WebRTC
     */
    String WEBRTC = "webrtc";
    /**
     * 音量
     */
    String VOLUMES = "volumes";
    /**
     * 日期时间
     */
    String DATETIME = "datetime";
    /**
     * 终端类型
     */
    String CLIENT_TYPE = "clientType";
    /**
     * 房间ID
     */
    String ROOM_ID = "roomId";
    /**
     * 媒体流ID
     */
    String STREAM_ID = "streamId";
    /**
     * 终端ID
     */
    String CLIENT_ID = "clientId";
    /**
     * 来源终端ID
     */
    String SOURCE_ID = "sourceId";
    /**
     * 会话ID
     */
    String SESSION_ID = "sessionId";
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
     * 媒体服务ID
     */
    String MEDIA_CLIENT_ID = "mediaClientId";
    /**
     * ICE服务
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
     * RTP参数
     */
    String RTP_PARAMETERS = "rtpParameters";
    /**
     * RTP协商
     */
    String RTP_CAPABILITIES = "rtpCapabilities";
    /**
     * DTLS参数
     */
    String DTLS_PARAMETERS = "dtlsParameters";
    /**
     * SCTP参数
     */
    String SCTP_PARAMETERS = "sctpParameters";
    /**
     * SCTP协商
     */
    String SCTP_CAPABILITIES = "sctpCapabilities";
    /**
     * 状态
     */
    String ENABLED = "enabled";
    /**
     * 是否是消费者
     */
    String CONSUMING = "consuming";
    /**
     * 是否是生产者
     */
    String PRODUCING = "producing";
    /**
     * 媒体订阅类型
     */
    String SUBSCRIBE_TYPE = "subscribeType";

    /**
     * 生产者ID生成器
     */
    public static final BiFunction<String, String, String> STREAM_ID_PRODUCER = (type, producerId) -> type + "::" + producerId;
    
    /**
     * 消费者ID生成器
     */
    public static final BiFunction<String, String, String> STREAM_ID_CONSUMER = (producerStreamId, consumerId) -> producerStreamId + "->" + consumerId;
    
}
