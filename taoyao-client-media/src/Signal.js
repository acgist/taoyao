const config = require("./Config");
const process = require("child_process");
const WebSocket = require("ws");

/**
 * 信令协议
 */
const protocol = {
  // 当前索引
  index: 0,
  // 最大索引
  maxIndex: 999,
  // 终端索引
  clientIndex: 99999,
  /**
   * @returns 索引
   */
  buildId() {
    if (++this.index > this.maxIndex) {
      this.index = 0;
    }
    const date = new Date();
    return 100000000000000 * date.getDate() +
    1000000000000 * date.getHours() +
    10000000000 * date.getMinutes() +
    100000000 * date.getSeconds() +
    1000 * this.clientIndex +
    this.index;
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
  buildMessage(signal, body = {}, id) {
    const message = {
      header: {
        v: config.signal.version,
        id: id || this.buildId(),
        signal: signal,
      },
      body: body,
    };
    return message;
  },
};

/**
 * 信令通道
 */
const signalChannel = {
  // 通道
  channel: null,
  // 地址
  address: null,
  // 回调
  callback: null,
  // 心跳时间
  heartbeatTime: 30 * 1000,
  // 心跳定时器
  heartbeatTimer: null,
  // 是否重连
  reconnection: true,
  // 重连定时器
  reconnectTimer: null,
  // 防止重复重连
  lockReconnect: false,
  // 当前重连时间
  connectionTimeout: 5 * 1000,
  // 最小重连时间
  minReconnectionDelay: 5 * 1000,
  // 最大重连时间
  maxReconnectionDelay: 60 * 1000,
  /**
   * 心跳
   */
  heartbeat() {
    const self = this;
    if (self.heartbeatTimer) {
      clearTimeout(self.heartbeatTimer);
    }
    self.heartbeatTimer = setTimeout(async function () {
      if (self.channel && self.channel.readyState === WebSocket.OPEN) {
        // TODO：信号强度、电池信息
        self.push(
          protocol.buildMessage("client::heartbeat", {
            signal: 100,
            battery: 100,
            charging: true,
          })
        );
        self.heartbeat();
      } else {
        console.warn("发送心跳失败：", self.address);
      }
    }, self.heartbeatTime);
  },
  /**
   * 连接
   *
   * @param {*} address 地址
   * @param {*} callback 回调
   * @param {*} reconnection 是否重连
   *
   * @returns Promise
   */
  async connect(address, callback, reconnection = true) {
    const self = this;
    if (self.channel && self.channel.readyState === WebSocket.OPEN) {
      return new Promise((resolve, reject) => {
        resolve(self.channel);
      });
    }
    self.address = address;
    self.callback = callback;
    self.reconnection = reconnection;
    return new Promise((resolve, reject) => {
      console.debug("连接信令通道：", self.address);
      self.channel = new WebSocket(self.address, { handshakeTimeout: 5000 });
      self.channel.on("open", async function () {
        console.info("打开信令通道：", self.address);
        // 注册终端
        // TODO：信号强度、电池信息
        self.push(
          protocol.buildMessage("client::register", {
            name: "桃夭媒体服务",
            clientId: config.signal.clientId,
            clientType: "MEDIA",
            signal: 100,
            battery: 100,
            charging: true,
            username: config.signal.username,
            password: config.signal.password,
          })
        );
        // 重置时间
        self.connectionTimeout = self.minReconnectionDelay;
        // 开始心跳
        self.heartbeat();
        // 成功回调
        resolve(self.channel);
      });
      self.channel.on("close", async function () {
        console.warn("信令通道关闭：", self.address);
        if (self.reconnection) {
          self.reconnect();
        }
        // 不要失败回调
      });
      self.channel.on("error", async function (e) {
        console.error("信令通道异常：", self.address, e);
        if (self.reconnection) {
          self.reconnect();
        }
        // 不要失败回调
      });
      self.channel.on("message", async function (data) {
        try {
          const content = data.toString();
          console.debug("信令通道消息：", content);
          self.callback(JSON.parse(content));
        } catch (error) {
          console.error("处理信令消息异常：", content, error);
        }
      });
    });
  },
  /**
   * 重连
   */
  reconnect() {
    const self = this;
    if (
      self.lockReconnect ||
      (self.channel && self.channel.readyState === WebSocket.OPEN)
    ) {
      return;
    }
    self.lockReconnect = true;
    if (self.reconnectTimer) {
      clearTimeout(self.reconnectTimer);
    }
    // 定时重连
    self.reconnectTimer = setTimeout(function () {
      console.info("信令通道重连：", self.address);
      self.connect(self.address, self.callback, self.reconnection);
      self.lockReconnect = false;
    }, self.connectionTimeout);
    self.connectionTimeout = Math.min(
      self.connectionTimeout + self.minReconnectionDelay,
      self.maxReconnectionDelay
    );
  },
  /**
   * 异步请求
   *
   * @param {*} message 消息
   */
   push(message) {
    try {
      this.channel.send(JSON.stringify(message));
    } catch (error) {
      console.error("异步请求异常：", message, error);
    }
  },
  /**
   * 关闭通道
   */
  close() {
    const self = this;
    self.reconnection = false;
    self.channel.close();
    clearTimeout(self.heartbeatTimer);
    clearTimeout(self.reconnectTimer);
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
  // 消费者复制数量
  consumerReplicas = 0;
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

  /**
   * 声音监控
   */
  handleAudioLevelObserver() {
    const self = this;
    // 声音
    self.audioLevelObserver.on("volumes", (volumes) => {
      for (const value of volumes) {
        const { producer, volume } = value;
        signalChannel.push(
          protocol.buildMessage("media::audio::active::speaker", {
            roomId: self.roomId,
            clientId: producer.clientId,
            volume: volume,
          })
        );
      }
    });
    // 静音
    self.audioLevelObserver.on("silence", () => {
      signalChannel.push(
        protocol.buildMessage("media::audio::active::speaker", {
          roomId: self.roomId,
        })
      );
    });
  }

  /**
   * 说话监控
   */
  handleActiveSpeakerObserver() {
    const self = this;
    self.activeSpeakerObserver.on("dominantspeaker", (dominantSpeaker) => {
      console.debug(
        "dominantspeaker：",
        dominantSpeaker.producer.id,
        dominantSpeaker.producer.clientId
      );
    });
  }

  /**
   * 使用情况
   */
  usage() {
    console.info("房间标识：", this.roomId);
    console.info("房间通道数量：", this.transports.size);
    console.info("房间生产者数量：", this.producers.size);
    console.info("房间消费者数量：", this.consumers.size);
    console.info("房间数据生产者数量：", this.dataProducers.size);
    console.info("房间数据消费者数量：", this.dataConsumers.size);
  }

  /**
   * 关闭资源
   */
  close() {
    const self = this;
    if (self.close) {
      return;
    }
    self.close = true;
    if (self.mediasoupRouter) {
      self.mediasoupRouter.close();
    }
  }
}

/**
 * 信令服务
 */
class Signal {
  // 房间列表
  rooms = new Map();
  // 回调事件
  callbackMapping = new Map();
  // Worker列表
  mediasoupWorkers = [];
  // Worker索引
  nextMediasoupWorkerIndex = 0;

  constructor(mediasoupWorkers) {
    this.mediasoupWorkers = mediasoupWorkers;
    // 定时打印使用情况
    setInterval(async () => {
      this.usage();
    }, 300 * 1000);
  }

  /**
   * 处理信令消息
   *
   * @param {*} message 消息
   */
  on(message) {
    // 请求回调
    if (this.callbackMapping.has(message.header.id)) {
      try {
        this.callbackMapping.get(message.header.id)(message);
      } finally {
        this.callbackMapping.delete(message.header.id);
      }
      return;
    }
    const body = message.body;
    switch (message.header.signal) {
      case "client::reboot":
        this.clientReboot(message, body);
        break;
      case "client::shutdown":
        this.clientShutdown(message, body);
        break;
      case "client::register":
        protocol.clientIndex = body.index;
        break;
      case "media::ice::restart":
        this.mediaIceRestart(message, body);
        break;
      case "media::consume":
        this.mediaConsume(message, body);
        break;
      case "media::produce":
        this.mediaProduce(message, body);
        break;
      case "media::router::rtp::capabilities":
        this.mediaRouterRtpCapabilities(message, body);
        break;
      case "media::transport::webrtc::connect":
        this.mediaTransportWebrtcConnect(message, body);
        break;
      case "media::transport::webrtc::create":
        this.mediaTransportWebrtcCreate(message, body);
        break;
      case "room::create":
        this.roomCreate(message, body);
        break;
    }
  }

  /**
   * 异步请求
   *
   * @param {*} message 消息
   */
  push(message) {
    try {
      signalChannel.channel.send(JSON.stringify(message));
    } catch (error) {
      console.error("异步请求异常：", message, error);
    }
  }

  /**
   * 同步请求
   *
   * @param {*} message 消息
   *
   * @returns Promise
   */
  async request(message) {
    const me = this;
    return new Promise((resolve, reject) => {
      let done = false;
      // 注册回调
      me.callbackMapping.set(message.header.id, (response) => {
        resolve(response);
        done = true;
      });
      // 发送消息
      try {
        signalChannel.channel.send(JSON.stringify(message));
      } catch (error) {
        reject("同步请求异常", error);
      }
      // 设置超时
      setTimeout(() => {
        if (!done) {
          me.callbackMapping.delete(message.header.id);
          reject("请求超时", message);
        }
      }, 5000);
    });
  }

  /**
   * 打印日志
   */
  async usage() {
    for (const worker of this.mediasoupWorkers) {
      const usage = await worker.getResourceUsage();
      console.info("Worker使用情况：", worker.pid, usage);
    }
    console.info("路由数量：", this.mediasoupWorkers.length);
    console.info("房间数量：", this.rooms.size);
    Array.from(this.rooms.values()).forEach((room) => room.usage());
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
   * 重启终端信令
   *
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  clientReboot(message, body) {
    process.exec(
      `pm2 restart ${config.signal.clientId}`,
      function (error, stdout, stderr) {
        console.info("重启媒体服务：", error, stdout, stderr);
      }
    );
    // this.push(message);
  }

  /**
   * 关闭终端信令
   *
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  clientShutdown(message, body) {
    process.exec(
      `pm2 stop ${config.signal.clientId}`,
      function (error, stdout, stderr) {
        console.info("关闭媒体服务：", error, stdout, stderr);
      }
    );
    // this.push(message);
  }

  /**
   * 媒体重启ICE信令
   *
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaIceRestart(message, body) {
    const { roomId, transportId } = body;
    const room = this.rooms.get(roomId);
    const transport = room.transports.get(transportId);
    const iceParameters = await transport.restartIce();
    message.body.iceParameters = iceParameters;
    this.push(message);
  }

  async mediaProduce(message, body) {
    const self = this;
    const {
      kind,
      roomId,
      clientId,
      streamId,
      appData,
      transportId,
      rtpParameters,
    } = body;
    const room = self.rooms.get(roomId);
    const transport = room.transports.get(transportId);
    const producer = await transport.produce({
      kind,
      appData,
      rtpParameters,
      // keyFrameRequestDelay: 5000
    });
    producer.clientId = clientId;
    producer.streamId = streamId;
    room.producers.set(producer.id, producer);
    // 打分
    producer.on("score", (score) => {
      self.push(
        protocol.buildMessage("media::producer::score", {
          roomId: roomId,
          producerId: producer.id,
          score,
        })
      );
    });

    producer.on("videoorientationchange", (videoOrientation) => {
      logger.debug(
        'producer "videoorientationchange" event [producerId:%s, videoOrientation:%o]',
        producer.id,
        videoOrientation
      );
    });

    producer.on("trace", (trace) => {
      logger.debug(
        'producer "trace" event [producerId:%s, trace.type:%s, trace:%o]',
        producer.id,
        trace.type,
        trace
      );
    });
    message.body = { kind: kind, producerId: producer.id };
    this.push(message);
    if (producer.kind === "audio") {
      room.audioLevelObserver
        .addProducer({ producerId: producer.id })
        .catch(() => {});
      room.activeSpeakerObserver
        .addProducer({ producerId: producer.id })
        .catch(() => {});
    }
  }

  async mediaConsume(message, body) {
    const { roomId, clientId, sourceId, streamId, producerId, transportId, rtpCapabilities } = body;
    const room = this.rooms.get(roomId);
    const producer = room.producers.get(producerId);
    const transport = room.transports.get(transportId);
    if (
      !room ||
      !producer ||
      !transport ||
      !rtpCapabilities ||
      !room.mediasoupRouter.canConsume({
        producerId: producerId,
        rtpCapabilities: rtpCapabilities,
      })
    ) {
      console.warn(
        "不能消费媒体：",
        roomId,
        clientId,
        producerId,
        transportId,
        rtpCapabilities
      );
      return;
    }
    const promises = [];
    const consumerCount = 1 + room.consumerReplicas;
    for (let i = 0; i < consumerCount; i++) {
      promises.push(
        (async () => {
          let consumer;
          try {
            consumer = await transport.consume({
              producerId: producerId,
              rtpCapabilities: rtpCapabilities,
              // 暂停
              paused: true,
            });
          } catch (error) {
            console.error(
              "创建消费者异常：",
              roomId,
              clientId,
              producerId,
              transportId,
              rtpCapabilities,
              error
            );
            return;
          }
          consumer.clientId = clientId;
          consumer.streamId = streamId;
          room.consumers.set(consumer.id, consumer);
          consumer.on("transportclose", () => {
            room.consumers.delete(consumer.id);
          });
          consumer.on("producerclose", () => {
            room.consumers.delete(consumer.id);
            this.push(
              protocol.buildMessage("media::consumer::close", {
                consumerId: consumer.id,
              })
            );
          });
          consumer.on("producerpause", () => {
            this.push(
              protocol.buildMessage("media::consumer::pause", {
                consumerId: consumer.id,
              })
            );
          });
          consumer.on("producerresume", () => {
            this.push(
              protocol.buildMessage("media::consumer::resume", {
                consumerId: consumer.id,
              })
            );
          });
          consumer.on("score", (score) => {
            this.push(
              protocol.buildMessage("media::consumer::score", {
                consumerId: consumer.id,
                score,
              })
            );
          });
          consumer.on("layerschange", (layers) => {
            this.push(
              protocol.buildMessage("media::consumer::layers::change", {
                consumerId: consumer.id,
                spatialLayer: layers ? layers.spatialLayer : null,
                temporalLayer: layers ? layers.temporalLayer : null,
              })
            );
          });
          consumer.on("trace", (trace) => {
            logger.debug(
              'consumer "trace" event [producerId:%s, trace.type:%s, trace:%o]',
              consumer.id,
              trace.type,
              trace
            );
          });
          // TODO：改为同步
          this.push(protocol.buildMessage("media::consume", {
          //await this.request("media::consume", {
            kind: consumer.kind,
            type: consumer.type,
            roomId: roomId,
            clientId: clientId,
            sourceId: sourceId,
            streamId: streamId,
            producerId: producerId,
            consumerId: consumer.id,
            rtpParameters: consumer.rtpParameters,
            appData: producer.appData,
            producerPaused: consumer.producerPaused,
          }));
          await consumer.resume();
          this.push(
            protocol.buildMessage("media::consumer::score", {
              consumerId: consumer.id,
              score: consumer.score,
            })
          );
        })()
      );
    }

    try {
      await Promise.all(promises);
    } catch (error) {
      console.warn("_createConsumer() | failed:%o", error);
    }
  }

  /**
   * 路由RTP能力信令
   *
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  mediaRouterRtpCapabilities(message, body) {
    const { roomId } = body;
    const room = this.rooms.get(roomId);
    message.body.rtpCapabilities = room.mediasoupRouter.rtpCapabilities;
    this.push(message);
  }

  /**
   * 连接WebRTC通道信令
   *
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaTransportWebrtcConnect(message, body) {
    const { roomId, transportId, dtlsParameters } = body;
    const room = this.rooms.get(roomId);
    const transport = room.transports.get(transportId);
    await transport.connect({ dtlsParameters });
    message.body = { roomId: roomId, transportId: transport.id };
    this.push(message);
  }

  /**
   * 创建WebRTC通道信令
   *
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaTransportWebrtcCreate(message, body) {
    const self = this;
    const {
      roomId,
      clientId,
      forceTcp,
      producing,
      consuming,
      sctpCapabilities,
    } = body;
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
    transport.clientId = clientId;
    transport.on("icestatechange", (iceState) => {
      console.debug(
        "WebRtcTransport icestatechange event：",
        iceState,
        transport.id
      );
    });
    transport.on("dtlsstatechange", (dtlsState) => {
      console.debug(
        "WebRtcTransport dtlsstatechange event：",
        dtlsState,
        transport.id
      );
    });
    transport.on("sctpstatechange", (sctpState) => {
      console.debug(
        "WebRtcTransport sctpstatechange event：",
        sctpState,
        transport.id
      );
    });
    await transport.enableTraceEvent(["bwe"]);
    transport.on("trace", (trace) => {
      console.debug("transport trace event：", trace, trace.type, transport.id);
    });
    // 可配置的事件
    // transport.on("routerclose", fn());
    // transport.on("listenserverclose", fn());
    // transport.observer.on("close", fn());
    // transport.observer.on("newproducer", fn(producer));
    // transport.observer.on("newconsumer", fn(consumer));
    // transport.observer.on("newdataproducer", fn(dataProducer));
    // transport.observer.on("newdataconsumer", fn(dataConsumer));
    // transport.observer.on("trace", fn(trace));
    room.transports.set(transport.id, transport);
    message.body = {
      transportId: transport.id,
      iceCandidates: transport.iceCandidates,
      iceParameters: transport.iceParameters,
      dtlsParameters: transport.dtlsParameters,
      sctpParameters: transport.sctpParameters,
    };
    self.push(message);
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
   * @param {*} message 消息
   * @param {*} body 消息主体
   *
   * @returns 房间
   */
  async roomCreate(message, body) {
    const roomId = body.roomId;
    let room = this.rooms.get(roomId);
    if (room) {
      this.push(message);
      return room;
    }
    const mediasoupWorker = this.nextMediasoupWorker();
    const { mediaCodecs } = config.mediasoup.routerOptions;
    const mediasoupRouter = await mediasoupWorker.createRouter({ mediaCodecs });
    mediasoupRouter.observer.on("close", () => {
      // TODO：通知房间关闭
    });
    // 可配置的事件
    // mediasoupRouter.on("workerclose", () => {});
    // mediasoupRouter.observer.on("newtransport", fn(transport));
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
    this.push(message);
    return room;
  }
}

module.exports = { Signal, signalChannel };
