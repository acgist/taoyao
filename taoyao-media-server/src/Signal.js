const Logger = require("./Logger");
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
        ...body
      },
    };
    return message;
  },
};

/**
 * Peer
 */
class Peer {

  peerId;
  device;
  displayName;
  rtpCapabilities;
  sctpCapabilities;
  transports = new Map();
  producers = new Map();
  consumers = new Map();
  dataProducers = new Map();
  dataConsumers = new Map();

}

/**
 * 房间
 */
class Room {

  // 是否关闭
  close = false;
  // 终端
  peers = new Map();
  // 房间ID
  roomId = null;
  // 网络节流
  networkThrottled = false;
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

  constructor({
    roomId,
    signal,
    webRtcServer,
    mediasoupRouter,
    audioLevelObserver,
    activeSpeakerObserver
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
      for(const value of volumes) {
        const { producer, volume } = value;
        this.signal.push(protocol.buildMessage(
          "audio::active::speaker",
          this.roomId,
          {
            peerId: producer.appData.peerId,
            volume: volume
          }
        ));
      }
    });
    // 静音
    this.audioLevelObserver.on("silence", () => {
      this.signal.push(protocol.buildMessage(
        "audio::active::speaker",
        this.roomId,
        {
          peerId: null
        }
      ));
    });
  }

  handleActiveSpeakerObserver() {
    this.activeSpeakerObserver.on("dominantspeaker", (dominantSpeaker) => {
      logger.debug("dominantspeaker", dominantSpeaker.producer.id);
    });
  }

  close() {
    this.close = true;
    if(this.mediasoupRouter) {
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
  // 日志
  logger = new Logger();
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
        this.logger.debug("授权成功", session._socket.remoteAddress);
        this.clients.push(session);
        session.authorize = true;
        message.code = "0000";
        message.message = "授权成功";
        message.body.username = null;
        message.body.password = null;
      } else {
        this.logger.warn("授权失败", session._socket.remoteAddress);
        message.code = "3401";
        message.message = "授权失败";
      }
      this.push(message, session);
      return;
    }
    // 处理信令
    switch (message.header.signal) {
      case "router::rtp::capabilities":
        this.routerRtpCapabilities(session, message);
        break;
      case "room::create":
        this.roomCreate(session, message, message.body);
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
    if(session) {
      try {
        session.send(JSON.stringify(message));
      } catch (error) {
        logger.error(
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
   * @param {*} message 消息
   * 
   * @returns 房间
   */
  selectRoom(message) {
    return this.rooms.get(message.body.roomId);
  }

  /**
   * 路由RTP能力信令
   * 
   * @param {*} session 信令通道
   * @param {*} message 消息
   */
  routerRtpCapabilities(session, message) {
    const room = this.selectRoom(message);
    message.body = room.mediasoupRouter.rtpCapabilities;
    this.push(message, session);
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
      interval: 2000
    });
    const activeSpeakerObserver = await mediasoupRouter.createActiveSpeakerObserver({
      interval: 500
    });
    room = new Room({
      roomId,
      webRtcServer: mediasoupWorker.appData.webRtcServer,
      mediasoupRouter,
      audioLevelObserver,
      activeSpeakerObserver
    });
    this.rooms.set(roomId, room);
    this.logger.info("创建房间", roomId, room);
    this.push(message, session);
    return room;
  }

}

module.exports = Signal;
