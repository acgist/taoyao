/**
 * 音频默认配置
 * TODO：MediaStreamTrack.applyConstraints()
 * TODO：播放音量（audio标签配置）、采集音量
 * 支持属性：navigator.mediaDevices.getSupportedConstraints()
 * https://developer.mozilla.org/en-US/docs/Web/API/MediaTrackSettings
 */
const defaultAudioConfig = {
  // 设备
  // deviceId : '',
  // 音量（废弃）：0.0~1.0
  // volume: 1.0,
  // 延迟时间（单位：秒）：500毫秒以内较好
  // latency: 0.4,
  // 采样位数：8|16|32
  sampleSize: { min: 8, ideal: 16, max: 32 },
  // 采样率：8000|16000|32000|48000
  sampleRate: { min: 8000, ideal: 32000, max: 48000 },
  // 声道数量：1|2
  channelCount: 1,
  // 是否开启自动增益：true|false
  autoGainControl: true,
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
  frameRate: { min: 15, ideal: 24, max: 45 },
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
