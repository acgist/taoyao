const os = require("os");

/**
 * 桃夭媒体服务地址
 */
const defaultTaoyaoHost = "192.168.1.110";

/**
 * 配置
 */
module.exports = {
  // 服务名称
  name: "taoyao-client-media",
  // 信令配置
  signal: {
    // 信令版本
    version   : "1.0.0",
    // 终端标识
    clientId  : "taoyao-client-media",
    // 终端类型
    clientType: "MEDIA",
    // 终端名称
    name      : "桃夭媒体服务",
    // 信令地址
    host      : "127.0.0.1",
    // host   : "192.168.1.100",
    // 信令端口
    port      : 8888,
    // 信令协议
    scheme    : "wss",
    // 信令帐号
    username  : "taoyao",
    // 信令密码
    password  : "taoyao",
  },
  // 录像配置
  record: {
    // 请求关键帧的最大次数
    requestKeyFrameMaxIndex: 16,
    // 请求关键帧的文件大小
    requestKeyFrameFileSize: 32 * 1024,
  },
  // Mediasoup
  mediasoup: {
    // Worker数量
    workerSize: Object.keys(os.cpus()).length,
    // Worker配置：https://mediasoup.org/documentation/v3/mediasoup/api/#WorkerSettings
    workerSettings: {
      // 日志标记
      logTags: [
        "bwe",
        "ice",
        "rtp",
        "rtx",
        "svc",
        "dtls",
        "info",
        "rtcp",
        "sctp",
        "srtp",
        "score",
        "message",
        "simulcast",
      ],
      // 日志级别：debug | warn | error | none
      logLevel: "warn",
      // RTP端口范围
      rtcMinPort: process.env.MEDIASOUP_MIN_PORT || 40000,
      rtcMaxPort: process.env.MEDIASOUP_MAX_PORT || 49999,
    },
    // Router配置：https://mediasoup.org/documentation/v3/mediasoup/api/#RouterOptions
    routerOptions: {
      mediaCodecs: [
        // OPUS PCMA PCMU G722
        {
          kind     : "audio",
          mimeType : "audio/opus",
          clockRate: 48000,
          channels : 2,
        },
        {
          kind      : "video",
          mimeType  : "video/VP8",
          clockRate : 90000,
          parameters: {
            "x-google-start-bitrate": 1000,
            // "x-google-min-bitrate": 800,
            // "x-google-max-bitrate": 1800,
          },
        },
        {
          kind      : "video",
          mimeType  : "video/VP9",
          clockRate : 90000,
          parameters: {
            "profile-id"            : 2,
            "x-google-start-bitrate": 1000,
            // "x-google-min-bitrate": 800,
            // "x-google-max-bitrate": 1800,
          },
        },
        {
          kind      : "video",
          mimeType  : "video/h264",
          clockRate : 90000,
          parameters: {
            "packetization-mode"     : 1,
            "profile-level-id"       : "4d0032",
            "level-asymmetry-allowed": 1,
            "x-google-start-bitrate" : 1000,
            // "x-google-min-bitrate": 800,
            // "x-google-max-bitrate": 1800,
          },
        },
        {
          kind      : "video",
          mimeType  : "video/h264",
          clockRate : 90000,
          parameters: {
            "packetization-mode"     : 1,
            "profile-level-id"       : "42e01f",
            "level-asymmetry-allowed": 1,
            "x-google-start-bitrate" : 1000,
            // "x-google-min-bitrate": 800,
            // "x-google-max-bitrate": 1800,
          },
        },
      ],
    },
    // WebRtcServer配置：https://mediasoup.org/documentation/v3/mediasoup/api/#WebRtcServerOptions
    webRtcServerOptions: {
      listenInfos: [
        {
          protocol   : "udp",
          ip         : process.env.MEDIASOUP_LISTEN_IP || "0.0.0.0",
          port       : 44444,
          announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || defaultTaoyaoHost || "127.0.0.1",
        },
        {
          protocol   : "tcp",
          ip         : process.env.MEDIASOUP_LISTEN_IP || "0.0.0.0",
          port       : 44444,
          announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || defaultTaoyaoHost || "127.0.0.1",
        },
      ],
    },
    // WebRtcTransport配置：https://mediasoup.org/documentation/v3/mediasoup/api/#WebRtcTransportOptions
    webRtcTransportOptions: {
      listenIps: [
        {
          ip         : process.env.MEDIASOUP_LISTEN_IP    || "0.0.0.0",
          announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || defaultTaoyaoHost || "127.0.0.1",
        },
      ],
      initialAvailableOutgoingBitrate: 1000000,
      minimumAvailableOutgoingBitrate: 800000,
      maxSctpMessageSize             : 262144,
      maxIncomingBitrate             : 1800000,
    },
    // PlainTransport配置：https://mediasoup.org/documentation/v3/mediasoup/api/#PlainTransportOptions
    plainTransportOptions: {
      listenIp: {
        ip         : process.env.MEDIASOUP_LISTEN_IP    || "0.0.0.0",
        announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || defaultTaoyaoHost || "127.0.0.1",
      },
      maxSctpMessageSize: 262144,
    },
  },
};

/**
 * PipeTransport  : RTP(router)
 * PlainTransport : RTP
 * DirectTransport: NodeJS
 * WebRtcTransport: WebRTC
 */
