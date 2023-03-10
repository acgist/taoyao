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
    // me.audioLevelObserver.observer.on("silence", fn());
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
    // me.audioLevelObserver.observer.on("volumes", fn(volumes));
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
    // me.activeSpeakerObserver.observer.on("dominantspeaker", fn(dominantSpeaker));
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
    // TODO：测试是否需要这里释放
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
    const me = this;
    // 请求回调
    if (me.callbackMapping.has(message.header.id)) {
      try {
        me.callbackMapping.get(message.header.id)(message);
      } finally {
        me.callbackMapping.delete(message.header.id);
      }
      return;
    }
    // 执行信令
    const body = message.body;
    switch (message.header.signal) {
      case "client::reboot":
        me.clientReboot(message, body);
        break;
      case "client::shutdown":
        me.clientShutdown(message, body);
        break;
      case "client::register":
        protocol.clientIndex = body.index;
        break;
      case "media::ice::restart":
        me.mediaIceRestart(message, body);
        break;
      case "media::consume":
        me.mediaConsume(message, body);
        break;
      case "media::consumer::close":
        me.mediaConsumerClose(message, body);
        break;
      case "media::produce":
        me.mediaProduce(message, body);
        break;
      case "media::router::rtp::capabilities":
        me.mediaRouterRtpCapabilities(message, body);
        break;
      case "media::transport::webrtc::connect":
        me.mediaTransportWebrtcConnect(message, body);
        break;
      case "media::transport::webrtc::create":
        me.mediaTransportWebrtcCreate(message, body);
        break;
      case "platform::error":
        me.platformError(message, body);
        break;
      case "room::create":
        me.roomCreate(message, body);
        break;
      case "room::close":
        me.roomClose(message, body);
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
    producer.on("transportclose", () => {
      console.info("producer transportclose：", producer.id);
      producer.close();
    });
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
      console.info("producer videoorientationchange：", producer.id, videoOrientation);
    });
    producer.on("trace", (trace) => {
      console.info("producer trace：", producer.id, trace);
    });
    producer.observer.on("close", () => {
      if(me.producers.delete(producer.id)) {
        console.info("producer close：", producer.id);
        this.push(
          protocol.buildMessage("media::producer::close", {
            roomId: roomId,
            producerId: producer.id
          })
        );
      } else {
        console.info("producer close non：", producer.id);
      }
    });
    producer.observer.on("pause", () => {
      console.info("producer pause：", producer.id);
    });
    producer.observer.on("resume", () => {
      console.info("producer resume：", producer.id);
    });
    // producer.observer.on("score", fn(score));
    // producer.observer.on("videoorientationchange", fn(videoOrientation));
    // producer.observer.on("trace", fn(trace));
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
            console.info("consumer transportclose：", consumer.id);
            // 信令服务统一调度关闭
            // consumer.close();
            // room.consumers.delete(consumer.id);
          });
          consumer.on("producerclose", () => {
            console.info("consumer producerclose：", consumer.id);
            // 信令服务统一调度关闭
            // consumer.close();
            // room.consumers.delete(consumer.id);
          });
          consumer.on("producerpause", () => {
            console.info("consumer producerpause：", consumer.id);
            this.push(
              protocol.buildMessage("media::consumer::pause", {
                consumerId: consumer.id,
              })
            );
          });
          consumer.on("producerresume", () => {
            console.info("consumer producerresume：", consumer.id);
            this.push(
              protocol.buildMessage("media::consumer::resume", {
                consumerId: consumer.id,
              })
            );
          });
          consumer.on("score", (score) => {
            console.info("consumer score：", consumer.id, score);
            this.push(
              protocol.buildMessage("media::consumer::score", {
                score: score,
                roomId: roomId,
                consumerId: consumer.id,
              })
            );
          });
          consumer.on("layerschange", (layers) => {
            console.info("consumer layerschange：", consumer.id, layers);
            this.push(
              protocol.buildMessage("media::consumer::layers::change", {
                consumerId: consumer.id,
                spatialLayer: layers ? layers.spatialLayer : null,
                temporalLayer: layers ? layers.temporalLayer : null,
              })
            );
          });
          consumer.on("trace", (trace) => {
            console.info("consumer trace：", consumer.id, trace);
          });
          // consumer.on("rtp", (rtpPacket) => {
          //   console.info("consumer rtp：", consumer.id, rtpPacket);
          // });
          consumer.observer.on("close", () => {
            if(room.consumers.delete(consumer.id)) {
              console.debug("consumer close：", consumer.id);
              this.push(
                protocol.buildMessage("media::consumer::close", {
                  roomId: roomId,
                  consumerId: consumer.id
                })
              );
            } else {
              console.debug("consumer close non：", consumer.id);
            }
          });
          consumer.observer.on("pause", () => {
            this.push(
              protocol.buildMessage("media::consumer::pause", {
                roomId: roomId,
                consumerId: consumer.id
              })
            );
          });
          consumer.observer.on("resume", () => {
            this.push(
              protocol.buildMessage("media::consumer::resume", {
                roomId: roomId,
                consumerId: consumer.id
              })
            );
          });
          // consumer.observer.on("score", fn(score));
          // consumer.observer.on("layerschange", fn(layers));
          // consumer.observer.on("trace", fn(trace));
          // 等待终端准备就绪
          this.request(
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
    // 通用事件
    transport.on("routerclose", () => {
      console.info("transport routerclose：", transport.id);
      transport.close();
    });
    transport.on("listenserverclose", () => {
      console.info("transport listenserverclose：", transport.id);
      transport.close();
    });
    await transport.enableTraceEvent(["bwe"]);
    // await transport.enableTraceEvent([ 'probation', 'bwe' ]);
    transport.on("trace", (trace) => {
      console.debug("transport trace：", transport.id, trace);
    });
    transport.observer.on("close", () => {
      console.info("transport close：", transport.id);
    });
    transport.observer.on("newproducer", (producer) => {
      console.info("transport newproducer：", transport.id, producer.id);
    });
    transport.observer.on("newconsumer", (consumer) => {
      console.info("transport newconsumer：", transport.id, consumer.id);
    });
    transport.observer.on("newdataproducer", (dataProducer) => {
      console.info("transport newdataproducer：", transport.id, dataProducer.id);
    });
    transport.observer.on("newdataconsumer", (dataConsumer) => {
      console.info("transport newdataconsumer：", transport.id, dataProducer.id);
    });
    // transport.observer.on("trace", fn(trace));
    /********************* webRtcTransport通道事件 *********************/
    // transport.on("icestatechange", (iceState) => {
    //   console.info("transport icestatechange：", transport.id, iceState);
    // });
    // transport.on("iceselectedtuplechange", (iceSelectedTuple) => {
    //   console.info("transport iceselectedtuplechange：", transport.id, iceSelectedTuple);
    // });
    // transport.on("dtlsstatechange", (dtlsState) => {
    //   console.info("transport dtlsstatechange：", transport.id, dtlsState);
    // });
    // transport.on("sctpstatechange", (sctpState) => {
    //   console.info("transport sctpstatechange：", transport.id, sctpState);
    // });
    // transport.observer.on("icestatechange", fn(iceState));
    // transport.observer.on("iceselectedtuplechange", fn(iceSelectedTuple));
    // transport.observer.on("dtlsstatechange", fn(dtlsState));
    // transport.observer.on("sctpstatechange", fn(sctpState));
    /********************* plainTransport通道事件 *********************/
    // transport.on("tuple", fn(tuple));
    // transport.on("rtcptuple", fn(rtcpTuple));
    // transport.on("sctpstatechange", fn(sctpState));
    // transport.observer.on("tuple", fn(tuple));
    // transport.observer.on("rtcptuple", fn(rtcpTuple));
    // transport.observer.on("sctpstatechange", fn(sctpState));
    /********************* pipeTransport通道事件 *********************/
    // transport.on("sctpstatechange", fn(sctpState));
    // transport.observer.on("sctpstatechange", fn(sctpState));
    /********************* directTransport通道事件 *********************/
    // transport.on("rtcp", fn(rtcpPacket));
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
    console.info("roomCreate：", roomId, mediasoupRouter.id);
    me.push(message);
    mediasoupRouter.on("workerclose", () => {
      console.info("mediasoupRouter workerclose：", roomId, mediasoupRouter.id);
      room.closeAll();
    });
    mediasoupRouter.observer.on("close", () => {
      if(me.rooms.delete(roomId)) {
        console.info("mediasoupRouter close：", roomId, mediasoupRouter.id);
        me.push(
          protocol.buildMessage("room::close", {
            roomId: roomId
          })
        );
      } else {
        console.info("mediasoupRouter close non：", roomId, mediasoupRouter.id);
      }
    });
    // mediasoupRouter.observer.on("newtransport", (transport) => {
    //   console.info("mediasoupRouter newtransport：", roomId, mediasoupRouter.id, transport.id);
    // });
    // mediasoupRouter.observer.on("newrtpobserver", (rtpObserver) => {
    //   console.info("mediasoupRouter newrtpobserver：", roomId, mediasoupRouter.id, rtpObserver.id);
    // });
  }
}

module.exports = { Taoyao, signalChannel };
