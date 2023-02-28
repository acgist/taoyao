const os = require("os");

/**
 * 配置
 */
module.exports = {
  // 服务名称
  name: "taoyao-client-media",
  // 保存目录
  storagePath: "/data/storage",
  // 图片目录
  storageImagePath: "/data/storage/image",
  // 视频目录
  storageVideoPath: "/data/storage/video",
  // 信令配置
  signal: {
    // 服务版本
    version: "1.0.0",
    // 终端标识
    clientId: "taoyao-client-media",
    // 地址
    host: "127.0.0.1",
    // 端口
    port: 8888,
    // 协议
    scheme: "wss",
    // 信令用户
    username: "taoyao",
    // 信令密码
    password: "taoyao",
  },
  // 水印
  watermark: {
    enabled: false,
    text: "taoyao",
    posx: 0,
    posy: 0,
    opacity: 1,
  },
  // Mediasoup
  mediasoup: {
    // 配置Worker进程数量
    workerSize: Object.keys(os.cpus()).length,
    // Worker：https://mediasoup.org/documentation/v3/mediasoup/api/#WorkerSettings
    workerSettings: {
      // 记录标记
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
      // 级别：debug | warn | error | none
      logLevel: "warn",
      rtcMinPort: process.env.MEDIASOUP_MIN_PORT || 40000,
      rtcMaxPort: process.env.MEDIASOUP_MAX_PORT || 49999,
    },
    // Router：https://mediasoup.org/documentation/v3/mediasoup/api/#RouterOptions
    routerOptions: {
      mediaCodecs: [
        {
          kind: "audio",
          mimeType: "audio/opus",
          clockRate: 48000,
          channels: 2,
        },
        {
          kind: "video",
          mimeType: "video/VP8",
          clockRate: 90000,
          parameters: {
            "x-google-start-bitrate": 1000,
          },
        },
        {
          kind: "video",
          mimeType: "video/VP9",
          clockRate: 90000,
          parameters: {
            "profile-id": 2,
            "x-google-start-bitrate": 1000,
          },
        },
        {
          kind: "video",
          mimeType: "video/h264",
          clockRate: 90000,
          parameters: {
            "packetization-mode": 1,
            "profile-level-id": "4d0032",
            "level-asymmetry-allowed": 1,
            "x-google-start-bitrate": 1000,
          },
        },
        {
          kind: "video",
          mimeType: "video/h264",
          clockRate: 90000,
          parameters: {
            "packetization-mode": 1,
            "profile-level-id": "42e01f",
            "level-asymmetry-allowed": 1,
            "x-google-start-bitrate": 1000,
          },
        },
      ],
    },
    // WebRtcServer：https://mediasoup.org/documentation/v3/mediasoup/api/#WebRtcServerOptions
    webRtcServerOptions: {
      listenInfos: [
        // UDP
        {
          protocol: "udp",
          ip: process.env.MEDIASOUP_LISTEN_IP || "0.0.0.0",
          port: 44444,
          announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || "192.168.1.110",
        },
        // TCP
        // {
        //   protocol: "tcp",
        //   ip: process.env.MEDIASOUP_LISTEN_IP || "0.0.0.0",
        //   port: 44444,
        //   announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || "192.168.1.110",
        // },
      ],
    },
    // WebRtcTransport：https://mediasoup.org/documentation/v3/mediasoup/api/#WebRtcTransportOptions
    webRtcTransportOptions: {
      listenIps: [
        {
          ip: process.env.MEDIASOUP_LISTEN_IP || "0.0.0.0",
          announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || "192.168.1.110",
        },
      ],
      initialAvailableOutgoingBitrate: 1000000,
      minimumAvailableOutgoingBitrate: 600000,
      maxSctpMessageSize: 262144,
      maxIncomingBitrate: 1500000,
    },
    // PlainTransport：https://mediasoup.org/documentation/v3/mediasoup/api/#PlainTransportOptions
    plainTransportOptions: {
      listenIp: {
        ip: process.env.MEDIASOUP_LISTEN_IP || "0.0.0.0",
        announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || "192.168.1.110",
      },
      maxSctpMessageSize: 262144,
    },
  },
};
