const config = require("./Config");

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
   * @param {*} roomId 房间标识
   * @param {*} body 信令消息
   * @param {*} id ID
   *
   * @returns 信令消息
   */
  buildMessage: function (signal, roomId, body = {}, id) {
    const message = {
      header: {
        v: config.version,
        id: id || this.buildId(),
        signal: signal,
      },
      body: {
        roomId,
        ...body,
      },
    };
    return message;
  },
};

/**
 * 房间
 */
class Room {
  // 是否关闭
  close = false;
  // 房间ID
  roomId = null;
  // 信令
  signal = null;
  // WebRTCServer
  webRtcServer = null;
  // 路由
  mediasoupRouter = null;
  // 音频监控
  audioLevelObserver = null;
  // 音频监控
  activeSpeakerObserver = null;
  // 通道
  transports = new Map();
  // 生产者
  producers = new Map();
  // 消费者
  consumers = new Map();
  // 数据通道生产者
  dataProducers = new Map();
  // 数据通道消费者
  dataConsumers = new Map();

  constructor({
    roomId,
    signal,
    webRtcServer,
    mediasoupRouter,
    audioLevelObserver,
    activeSpeakerObserver,
  }) {
    this.close = false;
    this.roomId = roomId;
    this.networkThrottled = false;
    this.signal = signal;
    this.webRtcServer = webRtcServer;
    this.mediasoupRouter = mediasoupRouter;
    this.audioLevelObserver = audioLevelObserver;
    this.activeSpeakerObserver = activeSpeakerObserver;
    this.handleAudioLevelObserver();
    this.handleActiveSpeakerObserver();
  }

  handleAudioLevelObserver() {
    // 声音
    this.audioLevelObserver.on("volumes", (volumes) => {
      for (const value of volumes) {
        const { producer, volume } = value;
        this.signal.push(
          protocol.buildMessage("audio::active::speaker", this.roomId, {
            peerId: producer.appData.peerId,
            volume: volume,
          })
        );
      }
    });
    // 静音
    this.audioLevelObserver.on("silence", () => {
      this.signal.push(
        protocol.buildMessage("audio::active::speaker", this.roomId, {
          peerId: null,
        })
      );
    });
  }

  handleActiveSpeakerObserver() {
    this.activeSpeakerObserver.on("dominantspeaker", (dominantSpeaker) => {
      console.debug("dominantspeaker", dominantSpeaker.producer.id);
    });
  }

  usage() {
    console.info("房间标识", this.roomId);
    console.info("房间通道数量", this.transports.size);
    console.info("房间生产者数量", this.producers.size);
    console.info("房间消费者数量", this.consumers.size);
    console.info("房间数据生产者数量", this.dataProducers.size);
    console.info("房间数据消费者数量", this.dataConsumers.size);
  }

  close() {
    this.close = true;
    if (this.mediasoupRouter) {
      this.mediasoupRouter.close();
    }
  }
}

/**
 * 信令
 */
class Signal {
  // 房间列表
  rooms = new Map();
  // 信令终端列表
  clients = [];
  // Worker列表
  mediasoupWorkers = [];
  // Worker索引
  nextMediasoupWorkerIndex = 0;

  constructor(mediasoupWorkers) {
    this.mediasoupWorkers = mediasoupWorkers;
  }

  /**
   * 处理事件
   *
   * @param {*} message 消息
   * @param {*} session websocket
   */
  on(message, session) {
    // 授权验证
    if (!session.authorize) {
      if (
        message?.header?.signal === "media::register" &&
        message?.body?.username === config.https.username &&
        message?.body?.password === config.https.password
      ) {
        console.debug("授权成功", session._socket.remoteAddress);
        this.clients.push(session);
        session.authorize = true;
        message.code = "0000";
        message.message = "授权成功";
        message.body.username = undefined;
        message.body.password = undefined;
      } else {
        console.warn("授权失败", session._socket.remoteAddress);
        message.code = "3401";
        message.message = "授权失败";
      }
      this.push(message, session);
      return;
    }
    // 处理信令
    const body = message.body;
    switch (message.header.signal) {
      case "media::router::rtp::capabilities":
        this.mediaRouterRtpCapabilities(session, message, body);
        break;
      case "media::transport::webrtc::connect":
        this.mediaTransportWebrtcConnect(session, message, body);
        break;
      case "media::transport::webrtc::create":
        this.mediaTransportWebrtcCreate(session, message, body);
        break;
      case "room::create":
        this.roomCreate(session, message, body);
        break;
    }
  }

  /**
   * 通知信令
   *
   * @param {*} message 消息
   * @param {*} session 信令通道
   */
  push(message, session) {
    if (session) {
      try {
        session.send(JSON.stringify(message));
      } catch (error) {
        console.error(
          "通知信令失败",
          session._socket.remoteAddress,
          message,
          error
        );
      }
    } else {
      this.clients.forEach((session) => this.push(message, session));
    }
  }

  /**
   * 打印日志
   */
  async usage() {
    for (const worker of this.mediasoupWorkers) {
      const usage = await worker.getResourceUsage();
      console.info("Worker使用情况", worker.pid, usage);
    }
    console.info("路由数量", this.mediasoupWorkers.length);
    console.info("房间数量", this.rooms.size);
    Array.from(this.rooms.values()).forEach(room => room.usage());
  }

  /**
   * @returns 下个Meidasoup Worker
   */
  nextMediasoupWorker() {
    const worker = this.mediasoupWorkers[this.nextMediasoupWorkerIndex];
    if (++this.nextMediasoupWorkerIndex === this.mediasoupWorkers.length) {
      this.nextMediasoupWorkerIndex = 0;
    }
    return worker;
  }

  /**
   * 路由RTP能力信令
   *
   * @param {*} session 信令通道
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  mediaRouterRtpCapabilities(session, message, body) {
    const { roomId } = body;
    const room = this.rooms.get(roomId);
    message.body = room.mediasoupRouter.rtpCapabilities;
    this.push(message, session);
  }

  async mediaTransportWebrtcConnect(session, message, body) {
    const { roomId, transportId, dtlsParameters } = body;
    const room = this.rooms.get(roomId);
    const transport = room.transports.get(transportId);
    if (!transport) {
      throw new Error(`transport with id "${transportId}" not found`);
    }
    await transport.connect({ dtlsParameters });
    message.body = { roomId };
    this.push(message, session);
  }

  /**
   * @param {*} session 信令通道
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaTransportWebrtcCreate(session, message, body) {
    const self = this;
    const { roomId, forceTcp, producing, consuming, sctpCapabilities } = body;
    const webRtcTransportOptions = {
      ...config.mediasoup.webRtcTransportOptions,
      appData: { producing, consuming },
      enableSctp: Boolean(sctpCapabilities),
      numSctpStreams: (sctpCapabilities || {}).numStreams,
    };
    if (forceTcp) {
      webRtcTransportOptions.enableUdp = false;
      webRtcTransportOptions.enableTcp = true;
    }
    const room = this.rooms.get(roomId);
    const transport = await room.mediasoupRouter.createWebRtcTransport({
      ...webRtcTransportOptions,
      webRtcServer: room.webRtcServer,
    });
    transport.on("dtlsstatechange", (dtlsState) => {
      console.debug(
        'WebRtcTransport dtlsstatechange event',
        dtlsState
      );
    });
    transport.on("sctpstatechange", (sctpState) => {
      console.debug(
        'WebRtcTransport sctpstatechange event',
        sctpState
      );
    });
    // await transport.enableTraceEvent([ 'probation', 'bwe' ]);
    await transport.enableTraceEvent(["bwe"]);
    transport.on("trace", (trace) => {
      console.debug(
        'transport trace event',
        trace,
        trace.type,
        transport.id,
      );
    });
    // Store the WebRtcTransport into the protoo Peer data Object.
    room.transports.set(transport.id, transport);
    message.body = {
      transportId: transport.id,
      iceCandidates: transport.iceCandidates,
      iceParameters: transport.iceParameters,
      dtlsParameters: transport.dtlsParameters,
      sctpParameters: transport.sctpParameters,
    };
    self.push(
      message,
      session
    );
    const { maxIncomingBitrate } = config.mediasoup.webRtcTransportOptions;
    // If set, apply max incoming bitrate limit.
    if (maxIncomingBitrate) {
      try {
        await transport.setMaxIncomingBitrate(maxIncomingBitrate);
      } catch (error) {}
    }
  }

  /**
   * 创建房间信令
   *
   * @param {*} session 信令通道
   * @param {*} message 消息
   * @param {*} body 消息主体
   *
   * @returns 房间
   */
  async roomCreate(session, message, body) {
    const roomId = body.roomId;
    let room = this.rooms.get(roomId);
    if (room) {
      this.push(message, session);
      return room;
    }
    const mediasoupWorker = this.nextMediasoupWorker();
    const { mediaCodecs } = config.mediasoup.routerOptions;
    const mediasoupRouter = await mediasoupWorker.createRouter({ mediaCodecs });
    mediasoupRouter.on("workerclose", () => {
      // TODO：通知房间关闭
    });
    mediasoupRouter.observer.on("close", () => {
      // TODO：通知房间关闭
    });
    // TODO：下面两个监控改为配置启用
    const audioLevelObserver = await mediasoupRouter.createAudioLevelObserver({
      maxEntries: 1,
      threshold: -80,
      interval: 2000,
    });
    const activeSpeakerObserver =
      await mediasoupRouter.createActiveSpeakerObserver({
        interval: 500,
      });
    room = new Room({
      roomId,
      webRtcServer: mediasoupWorker.appData.webRtcServer,
      mediasoupRouter,
      audioLevelObserver,
      activeSpeakerObserver,
    });
    this.rooms.set(roomId, room);
    console.info("创建房间", roomId);
    this.push(message, session);
    return room;
  }
}

module.exports = Signal;
