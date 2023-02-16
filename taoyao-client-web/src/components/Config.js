/**
 * 桃夭配置
 */

/**
 * 信令配置
 * TODO：合并到taoyao
 */
const config = {
  // 终端标识
  clientId: "taoyao",
  // 信令服务地址
  host: "localhost",
  port: "8888",
  // 终端名称
  name: "taoyao-client-web",
  // 终端版本
  version: "1.0.0",
  // 日志级别
  logLevel: "DEBUG",
  // 帐号密码
  username: "taoyao",
  password: "taoyao",
  signal: function () {
    return `wss://${this.host}:${this.port}/websocket.signal`;
  },
  // 媒体配置
  audio: {},
  video: {},
  // WebRTC配置
  webrtc: {},
  // 媒体服务配置
  mediaServerList: [],
};

/**
 * 信令协议
 */
const protocol = {
  // 当前索引
  index: 100000,
  // 最小索引
  minIndex: 100000,
  // 最大索引
  maxIndex: 999999,
  /**
   * @returns 索引
   */
  buildId: function () {
    if (this.index++ >= this.maxIndex) {
      this.index = this.minIndex;
    }
    return Date.now() + "" + this.index;
  },
  /**
   * 生成信令消息
   *
   * @param {*} signal 信令标识
   * @param {*} body 信令消息
   * @param {*} id ID
   *
   * @returns 信令消息
   */
  buildMessage: function (signal, body = {}, id) {
    let message = {
      header: {
        v: config.version,
        id: id || this.buildId(),
        signal: signal,
      },
      body: body,
    };
    return message;
  },
};

/**
 * 默认音频配置
 */
const defaultAudioConfig = {
  // 设备
  // deviceId : '',
  // 音量：0~1
  volume: 0.5,
  // 延迟大小（单位毫秒）：500毫秒以内较好
  latency: 0.4,
  // 采样数：16
  sampleSize: 16,
  // 采样率：8000|16000|32000|48000
  sampleRate: 32000,
  // 声道数量：1|2
  channelCount: 1,
  // 是否开启自动增益：true|false
  autoGainControl: false,
  // 是否开启降噪功能：true|false
  noiseSuppression: true,
  // 是否开启回音消除：true|false
  echoCancellation: true,
  // 消除回音方式：system|browser
  echoCancellationType: "system",
};

/**
 * 默认视频配置
 */
const defaultVideoConfig = {
  // 设备
  // deviceId: '',
  // 宽度
  width: 1280,
  // 高度
  height: 720,
  // 帧率
  frameRate: 24,
  // 选摄像头：user|left|right|environment
  facingMode: "environment",
};

/**
 * 默认RTCPeerConnection配置
 */
const defaultRTCPeerConnectionConfig = {
  // ICE代理的服务器
  iceServers: null,
  // 传输通道绑定策略：balanced|max-compat|max-bundle
  bundlePolicy: "balanced",
  // RTCP多路复用策略：require|negotiate
  rtcpMuxPolicy: "require",
  // ICE传输策略：all|relay
  iceTransportPolicy: "all",
  // ICE候选个数
  iceCandidatePoolSize: 8,
};

export {
  config,
  protocol,
  defaultAudioConfig,
  defaultVideoConfig,
  defaultRTCPeerConnectionConfig,
};
