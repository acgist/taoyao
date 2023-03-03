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
 * VP9默认配置
 */
const defaultKsvcEncodings = [{ scalabilityMode: "S3T3_KEY" }];

/**
 * simulcast默认配置
 */
const defaultSimulcastEncodings = [
  { scaleResolutionDownBy: 4, maxBitrate: 500000, scalabilityMode: "S1T2" },
  { scaleResolutionDownBy: 2, maxBitrate: 1000000, scalabilityMode: "S1T2" },
  { scaleResolutionDownBy: 1, maxBitrate: 5000000, scalabilityMode: "S1T2" },
];

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
  defaultAudioConfig,
  defaultVideoConfig,
  defaultKsvcEncodings,
  defaultSimulcastEncodings,
  defaultRTCPeerConnectionConfig,
};
