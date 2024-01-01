const os = require("os");
const dotenv = require("dotenv");

if(process.env.NODE_ENV) {
  dotenv.config({ path: `.env.${process.env.NODE_ENV}` });
} else {
  dotenv.config({ path: `.env` });
}

/**
 * 配置
 */
module.exports = {
  // 服务名称
  name: process.env.NAME || "taoyao-client-media",
  // 信令配置
  signal: {
    // 信令版本
    version   : process.env.SIGNAL_VERSION || "1.0.0",
    // 终端ID
    clientId  : process.env.CLIENT_ID      || "taoyao-client-media",
    // 终端类型
    clientType: "MEDIA",
    // 终端名称
    name      : process.env.CLIENT_NAME || "桃夭媒体服务",
    // 信令地址
    host      : process.env.SIGNAL_HOST || "127.0.0.1",
    // 信令端口
    port      : process.env.SIGNAL_PORT || 8888,
    // 信令协议
    scheme    : "wss",
    // 信令帐号
    username  : process.env.SIGNAL_USERNAME || "taoyao",
    // 信令密码
    password  : process.env.SIGNAL_PASSWORD || "taoyao",
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
    workerSize: process.env.MEDIASOUP_WORKER_SIZE || Object.keys(os.cpus()).length,
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
      logLevel  : "warn",
      // RTP端口范围（最小）
      rtcMinPort: process.env.MEDIASOUP_MIN_PORT || 40000,
      // RTP端口范围（最大）
      rtcMaxPort: process.env.MEDIASOUP_MAX_PORT || 49999,
    },
    // Router配置：https://mediasoup.org/documentation/v3/mediasoup/api/#RouterOptions
    routerOptions: {
      mediaCodecs: [
        {
          kind     : "audio",
          mimeType : "audio/opus",
          clockRate: 48000,
          channels : 2,
        },
        {
          kind     : "audio",
          mimeType : "audio/pcmu",
          clockRate: 8000,
          channels : 1,
        },
        {
          kind     : "audio",
          mimeType : "audio/pcma",
          clockRate: 8000,
          channels : 1,
        },
        {
          kind      : "video",
          mimeType  : "video/VP8",
          clockRate : 90000,
          parameters: {
            "x-google-start-bitrate": 400,
            // "x-google-min-bitrate": 800,
            // "x-google-max-bitrate": 1600,
          },
        },
        {
          kind      : "video",
          mimeType  : "video/VP9",
          clockRate : 90000,
          parameters: {
            "profile-id"            : 2,
            "x-google-start-bitrate": 400,
            // "x-google-min-bitrate": 800,
            // "x-google-max-bitrate": 1600,
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
            "x-google-start-bitrate" : 400,
            // "x-google-min-bitrate": 800,
            // "x-google-max-bitrate": 1600,
          },
        },
        // 安卓H5失败
        {
          kind      : "video",
          mimeType  : "video/h264",
          clockRate : 90000,
          parameters: {
            "packetization-mode"     : 1,
            "profile-level-id"       : "4d0032",
            "level-asymmetry-allowed": 1,
            "x-google-start-bitrate" : 400,
            // "x-google-min-bitrate": 800,
            // "x-google-max-bitrate": 1600,
          },
        },
      ],
    },
    // WebRtcServer配置：https://mediasoup.org/documentation/v3/mediasoup/api/#WebRtcServerOptions
    webRtcServerOptions: {
      listenInfos: [
        {
          protocol   : "udp",
          ip         : process.env.MEDIASOUP_LISTEN_IP    || "0.0.0.0",
          port       : process.env.MEDIASOUP_LISTEN_PORT  || 44444,
          announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || "127.0.0.1",
        },
        {
          protocol   : "tcp",
          ip         : process.env.MEDIASOUP_LISTEN_IP    || "0.0.0.0",
          port       : process.env.MEDIASOUP_LISTEN_PORT  || 44444,
          announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || "127.0.0.1",
        },
      ],
    },
    // WebRtcTransport配置：https://mediasoup.org/documentation/v3/mediasoup/api/#WebRtcTransportOptions
    webRtcTransportOptions: {
      listenIps: [
        {
          ip         : process.env.MEDIASOUP_LISTEN_IP    || "0.0.0.0",
          announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || "127.0.0.1",
        },
      ],
      initialAvailableOutgoingBitrate: 400000,
      minimumAvailableOutgoingBitrate: 800000,
      maxSctpMessageSize             : 262144,
      maxIncomingBitrate             : 1600000,
    },
    // PlainTransport配置：https://mediasoup.org/documentation/v3/mediasoup/api/#PlainTransportOptions
    plainTransportOptions: {
      listenIp: {
        ip         : process.env.MEDIASOUP_LISTEN_IP    || "0.0.0.0",
        announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || "127.0.0.1",
      },
      maxSctpMessageSize: 262144,
    },
  },
};
