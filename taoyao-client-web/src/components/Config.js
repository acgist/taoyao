/**
 * 配置
 */
const config = {
  // 媒体配置
  media: {
    // 是否预览共享文件
    filePreview: true
  },
  // 信令配置
  signal: {
    // 信令版本
    version   : "1.0.0",
    // 终端类型
    clientType: "WEB",
  },
};

/**
 * 音频默认配置
 * 配置：{ min: 8000, exact: 32000, ideal: 32000, max: 48000 }
 * 
 * https://developer.mozilla.org/en-US/docs/Web/API/MediaTrackSettings
 */
const defaultAudioConfig = {
  // 指定设备
  // deviceId     : '',
  // 标识会话
  // groupId      : '',
  // 音量（废弃）：0.0~1.0
  // volume       : 1.0,
  // 延迟时间（单位：秒）：500毫秒以内较好
  // latency      : 0.4,
  // 采样位数：8|16|32
  sampleSize      : { min: 8,    ideal: 16,    max: 32    },
  // 采样率：8000|16000|32000|48000
  sampleRate      : { min: 8000, ideal: 32000, max: 48000 },
  // 声道数量：1|2
  channelCount    : 1,
  // 是否开启自动增益：true|false
  autoGainControl : true,
  // 是否开启降噪功能：true|false
  noiseSuppression: true,
  // 是否开启回音消除：true|false
  echoCancellation: true,
};

/**
 * 视频默认配置
 * 配置：{ min: 8000, exact: 32000, ideal: 32000, max: 48000 }
 * 
 * https://developer.mozilla.org/en-US/docs/Web/API/MediaTrackSettings
 */
const defaultVideoConfig = {
  // 指定设备
  // deviceId   : '',
  // 标识会话
  // groupId    : '',
  // 宽度
  width         : { min: 720, ideal: 1280, max: 4096 },
  // 高度
  height        : { min: 480, ideal: 720,  max: 2160 },
  // 帧率
  frameRate     : { min: 15,  ideal: 24,   max: 45   },
  // 摄像头：user|left|right|environment
  facingMode    : "environment",
  // 裁剪
  // resizeMode : null,
  // 宽高比
  // aspectRatio: 1.7777777778,
};

/**
 * 共享屏幕默认配置
 * 
 * https://developer.mozilla.org/en-US/docs/Web/API/MediaTrackSettings
 */
const defaultShareScreenConfig = {
  // 显示鼠标：always|motion|never
  cursor        : "always",
  // 逻辑窗口捕获（没有完全显示）
  logicalSurface: true,
  // 视频来源：window|monitor|browser|application
  displaySurface: "monitor",
};

/**
 * SVC默认配置
 * 支持编码：VP9
 * 
 * https://w3c.github.io/webrtc-svc/
 * https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities/
 * https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities/#SVC
 */
const defaultSvcEncodings = [
  {
    dtx            : true,
    maxBitrate     : 5000000,
    scalabilityMode: 'L3T3_KEY'
  }
];

/**
 * Simulcast默认配置
 * 支持编码：VP8 H264
 * 可以根据数量减少配置数量
 * dtx：屏幕贡献开启效果显著
 * 
 * https://w3c.github.io/webrtc-svc/
 * https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities/
 * https://mediasoup.org/documentation/v3/mediasoup/rtp-parameters-and-capabilities/#Simulcast
 */
const defaultSimulcastEncodings = [
  {
    dtx                  : true,
    maxBitrate           : 5000000,
    scalabilityMode      : 'L1T3',
    scaleResolutionDownBy: 1,
  },
  {
    dtx                  : true,
    maxBitrate           : 1000000,
    scalabilityMode      : 'L1T3',
    scaleResolutionDownBy: 2,
  },
  {
    dtx                  : true,
    maxBitrate           : 500000,
    scalabilityMode      : 'L1T3',
    scaleResolutionDownBy: 4,
  },
];

/**
 * RTCPeerConnection默认配置
 * 
 * https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/RTCPeerConnection
 */
const defaultRTCPeerConnectionConfig = {
  // ICE代理服务器
  iceServers          : [
    {
      // { "urls": "stun:stun1.l.google.com:19302" },
      urls: [
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302",
        "stun:stun3.l.google.com:19302",
        "stun:stun4.l.google.com:19302"
      ]
    }
  ],
  // 目标对等身份
  // peerIdentity     : null,
  // 传输通道绑定策略：balanced|max-compat|max-bundle
  bundlePolicy        : "balanced",
  // RTCP多路复用策略：require|negotiate
  rtcpMuxPolicy       : "require",
  // 连接证书
  // certificates     : null,
  // ICE传输策略：all|relay
  iceTransportPolicy  : "all",
  // ICE候选个数
  iceCandidatePoolSize: 8,
};

export {
  config,
  defaultAudioConfig,
  defaultVideoConfig,
  defaultShareScreenConfig,
  defaultSvcEncodings,
  defaultSimulcastEncodings,
  defaultRTCPeerConnectionConfig,
};
