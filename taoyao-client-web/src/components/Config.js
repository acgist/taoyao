/**
 * 信令
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
  buildId() {
    const self = this;
    if (self.index++ >= self.maxIndex) {
      self.index = self.minIndex;
    }
    return Date.now() + "" + self.index;
  },
  /**
   * @param {*} signal 信令标识
   * @param {*} body 消息主体
   * @param {*} id 消息标识
   * @param {*} v 消息版本
   *
   * @returns 信令消息
   */
  buildMessage(signal, body = {}, id, v) {
    if (!signal) {
      throw new Error("信令标识缺失");
    }
    const message = {
      header: {
        v: v || "1.0.0",
        id: id || this.buildId(),
        signal: signal,
      },
      body: body,
    };
    return message;
  },
};

/**
 * 音频默认配置
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
  sampleRate: 48000,
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
 * 视频默认配置
 */
const defaultVideoConfig = {
  // 设备
  // deviceId: '',
  // 宽度
  width: { min: 720, ideal: 1280, max: 4096 },
  // 高度
  height: { min: 480, ideal: 720, max: 2160 },
  // 帧率
  frameRate: 24,
  // 选摄像头：user|left|right|environment
  facingMode: "environment",
};

/**
 * RTCPeerConnection默认配置
 */
const defaultRTCPeerConnectionConfig = {
  // ICE代理的服务器
  iceServers: [],
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
  protocol,
  defaultAudioConfig,
  defaultVideoConfig,
  defaultRTCPeerConnectionConfig,
};
