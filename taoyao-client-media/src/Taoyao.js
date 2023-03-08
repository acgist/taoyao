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
    return (
      100000000000000 * date.getDate() +
      1000000000000 * date.getHours() +
      10000000000 * date.getMinutes() +
      100000000 * date.getSeconds() +
      1000 * this.clientIndex +
      this.index
    );
  },
  /**
   * @param {*} signal 信令标识
   * @param {*} body 消息主体
   * @param {*} id 消息ID
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
  // 桃夭
  taoyao: null,
  // 通道
  channel: null,
  // 地址
  address: null,
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
  reconnectionTimeout: 5 * 1000,
  // 最小重连时间
  minReconnectionDelay: 5 * 1000,
  // 最大重连时间
  maxReconnectionDelay: 60 * 1000,
  /**
   * 心跳
   */
  heartbeat() {
    const me = this;
    if (me.heartbeatTimer) {
      clearTimeout(me.heartbeatTimer);
    }
    me.heartbeatTimer = setTimeout(async function () {
      if (me.channel && me.channel.readyState === WebSocket.OPEN) {
        me.push(
          // TODO：电池信息
          protocol.buildMessage("client::heartbeat", {
            battery: 100,
            charging: true,
          })
        );
        me.heartbeat();
      } else {
        console.warn("心跳失败：", me.address);
      }
    }, me.heartbeatTime);
  },
  /**
   * 连接
   *
   * @param {*} address 地址
   * @param {*} reconnection 是否重连
   *
   * @returns Promise
   */
  async connect(address, reconnection = true) {
    const me = this;
    if (me.channel && me.channel.readyState === WebSocket.OPEN) {
      return new Promise((resolve, reject) => {
        resolve(me.channel);
      });
    }
    me.address = address;
    me.reconnection = reconnection;
    return new Promise((resolve, reject) => {
      console.debug("连接信令通道：", me.address);
      me.channel = new WebSocket(me.address, { handshakeTimeout: 5000 });
      me.channel.on("open", async function () {
        console.info("打开信令通道：", me.address);
        // TODO：电池信息
        me.push(
          protocol.buildMessage("client::register", {
            clientId: config.signal.clientId,
            name: config.signal.name,
            clientType: "MEDIA",
            battery: 100,
            charging: true,
            username: config.signal.username,
            password: config.signal.password,
          })
        );
        me.reconnectionTimeout = me.minReconnectionDelay;
        me.taoyao.connect = true;
        me.heartbeat();
        resolve(me.channel);
      });
      me.channel.on("close", async function () {
        console.warn("信令通道关闭：", me.address);
        me.taoyao.connect = false;
        if (me.reconnection) {
          me.reconnect();
        }
        // 不要失败回调
      });
      me.channel.on("error", async function (e) {
        console.error("信令通道异常：", me.address, e);
        me.taoyao.connect = false;
        if (me.reconnection) {
          me.reconnect();
        }
        // 不要失败回调
      });
      me.channel.on("message", async function (data) {
        try {
          const content = data.toString();
          console.debug("信令通道消息：", content);
          me.taoyao.on(JSON.parse(content));
        } catch (error) {
          console.error("处理信令消息异常：", data.toString(), error);
        }
      });
    });
  },
  /**
   * 重连
   */
  reconnect() {
    const me = this;
    if (
      me.lockReconnect ||
      (me.channel && me.channel.readyState === WebSocket.OPEN)
    ) {
      return;
    }
    me.lockReconnect = true;
    if (me.reconnectTimer) {
      clearTimeout(me.reconnectTimer);
    }
    // 定时重连
    me.reconnectTimer = setTimeout(function () {
      console.info("重连信令通道：", me.address);
      me.connect(me.address, me.reconnection);
      me.lockReconnect = false;
    }, me.reconnectionTimeout);
    me.reconnectionTimeout = Math.min(
      me.reconnectionTimeout + me.minReconnectionDelay,
      me.maxReconnectionDelay
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
    const me = this;
    clearTimeout(me.heartbeatTimer);
    clearTimeout(me.reconnectTimer);
    me.reconnection = false;
    me.channel.close();
    me.taoyao.connect = false;
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
  // 桃夭
  taoyao = null;
  // WebRTCServer
  webRtcServer = null;
  // 路由
  mediasoupRouter = null;
  // 音量监控
  audioLevelObserver = null;
  // 采样监控
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
    taoyao,
    webRtcServer,
    mediasoupRouter,
    audioLevelObserver,
    activeSpeakerObserver,
  }) {
    this.close = false;
    this.roomId = roomId;
    this.networkThrottled = false;
    this.taoyao = taoyao;
    this.webRtcServer = webRtcServer;
    this.mediasoupRouter = mediasoupRouter;
    this.audioLevelObserver = audioLevelObserver;
    this.activeSpeakerObserver = activeSpeakerObserver;
    this.handleAudioLevelObserver();
    this.handleActiveSpeakerObserver();
  }
  /**
   * 音量监控
   */
  handleAudioLevelObserver() {
    const me = this;
    // 静音
    me.audioLevelObserver.on("silence", () => {
      signalChannel.push(
        protocol.buildMessage("media::audio::volume", {
          roomId: me.roomId,
        })
      );
    });
    // 音量
    me.audioLevelObserver.on("volumes", (volumes) => {
      const volumeArray = [];
      for (const value of volumes) {
        const { producer, volume } = value;
        volumeArray.push({ volume: volume, clientId: producer.clientId });
      }
      signalChannel.push(
        protocol.buildMessage("media::audio::volume", {
          roomId: me.roomId,
          volumes: volumeArray
        })
      );
    });
  }
  /**
   * 采样监控
   */
  handleActiveSpeakerObserver() {
    const me = this;
    me.activeSpeakerObserver.on("dominantspeaker", (dominantSpeaker) => {
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
  closeAll() {
    const me = this;
    if (me.close) {
      return;
    }
    me.close = true;
    // me.producers.forEach(v => v.close());
    // me.consumers.forEach(v => v.close());
    // me.dataProducers.forEach(v => v.close());
    // me.dataConsumers.forEach(v => v.close());
    me.audioLevelObserver.close();
    me.activeSpeakerObserver.close();
    me.transports.forEach(v => v.close());
    me.mediasoupRouter.close();
  }
}

/**
 * 桃夭
 */
class Taoyao {
  // 是否连接
  connect = false;
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
      case "media::consumer::close":
        me.mediaConsumerClose(message, body);
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
      case "platform::error":
        this.platformError(message, body);
        break;
      case "room::create":
        this.roomCreate(message, body);
        break;
      case "room::close":
        this.roomClose(message, body);
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
    console.info("工作线程数量：", this.mediasoupWorkers.length);
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
    const {
      roomId,
      clientId,
      sourceId,
      streamId,
      producerId,
      transportId,
      rtpCapabilities,
    } = body;
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
            console.info("通道关闭同时关闭消费者：", consumer.id);
            // 信令服务统一调度关闭
            // consumer.close();
            // room.consumers.delete(consumer.id);
          });
          consumer.on("producerclose", () => {
            console.info("生产者关闭同时关闭消费者：", consumer.id);
            // 信令服务统一调度关闭
            // consumer.close();
            // room.consumers.delete(consumer.id);
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
                score: score,
                roomId: roomId,
                consumerId: consumer.id,
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
          consumer.observer.on("close", () => {
            this.push(
              protocol.buildMessage("media::consumer::close", {
                roomId: roomId,
                consumerId: consumer.id
              })
            );
          });
          // TODO：改为同步
          //await this.request("media::consume", {
          this.push(
            protocol.buildMessage("media::consume", {
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
            })
          );
          await consumer.resume();
          this.push(
            protocol.buildMessage("media::consumer::score", {
              score: consumer.score,
              roomId: roomId,
              consumerId: consumer.id,
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
   * 关闭消费者信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  mediaConsumerClose(message, body) {
    const { roomId, consumerId } = body;
    const room = this.rooms.get(roomId);
    const consumer = room.consumers.get(consumerId);
    if(consumer) {
      console.info("关闭消费者：", consumerId);
      consumer.close();
      room.consumers.delete(consumerId);
    } else {
      console.debug("关闭消费者无效：", consumerId);
    }
  }

  /**
   * 路由RTP协商信令
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
    // await transport.enableTraceEvent([ 'probation', 'bwe' ]);
    transport.on("trace", (trace) => {
      console.debug("transport trace event：", transport.id, trace.type, trace);
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
   * 平台异常信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  platformError(message, body) {
    const { code } = message;
    if(code === "3401") {
      signalChannel.close();
    }
  }
  /**
   * 关闭房间信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async roomClose(message, body) {
    const roomId = body.roomId;
    const room = this.rooms.get(roomId);
    if(!room) {
      console.warn("房间无效：", roomId);
      return;
    }
    console.info("关闭房间：", roomId);
    room.closeAll();
    this.rooms.delete(roomId);
  }

  /**
   * 创建房间信令
   *
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async roomCreate(message, body) {
    const me = this;
    const roomId = body.roomId;
    let room = me.rooms.get(roomId);
    if (room) {
      console.debug("创建房间已经存在：", room);
      me.push(message);
      return;
    }
    const mediasoupWorker = me.nextMediasoupWorker();
    const { mediaCodecs } = config.mediasoup.routerOptions;
    const mediasoupRouter = await mediasoupWorker.createRouter({ mediaCodecs });
    // 音量监控
    const audioLevelObserver = await mediasoupRouter.createAudioLevelObserver({
      interval: 2000,
      // 范围：-127~0
      threshold: -80,
      // 采样数量
      maxEntries: 2,
    });
    // 采样监控
    const activeSpeakerObserver = await mediasoupRouter.createActiveSpeakerObserver({
      interval: 500,
    });
    room = new Room({
      roomId,
      webRtcServer: mediasoupWorker.appData.webRtcServer,
      mediasoupRouter,
      audioLevelObserver,
      activeSpeakerObserver,
    });
    me.rooms.set(roomId, room);
    console.info("创建房间", roomId);
    me.push(message);
    // 监听事件
    mediasoupRouter.observer.on("close", () => {
      console.info("房间路由关闭：", roomId, mediasoupRouter);
      room.closeAll();
      me.rooms.delete(roomId);
      me.push(
        protocol.buildMessage("room::close", {
          roomId: roomId
        })
      );
    });
    // mediasoupRouter.on("workerclose", () => {});
    // mediasoupRouter.observer.on("newtransport", fn(transport));
  }
}

module.exports = { Taoyao, signalChannel };
