const config    = require("./Config.js");
const process   = require("child_process");
const WebSocket = require("ws");
const { trace } = require("console");

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
      100000000000000 * date.getDate()    +
      1000000000000   * date.getHours()   +
      10000000000     * date.getMinutes() +
      100000000       * date.getSeconds() +
      1000            * this.clientIndex  +
      this.index
    );
  },
  /**
   * @param {*} signal 信令标识
   * @param {*} body   消息主体
   * @param {*} id     消息ID
   * @param {*} v      消息版本
   *
   * @returns 信令消息
   */
  buildMessage(signal, body = {}, id, v) {
    const message = {
      header: {
        v:      v  || config.signal.version,
        id:     id || this.buildId(),
        signal: signal,
      },
      body:     body,
    };
    return message;
  },
};

/**
 * 名称冲突
 */
 const taoyaoProtocol = protocol;

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
    me.producers.forEach(v => v.close());
    me.consumers.forEach(v => v.close());
    me.dataProducers.forEach(v => v.close());
    me.dataConsumers.forEach(v => v.close());
    me.transports.forEach(v => v.close());
    me.audioLevelObserver.close();
    me.activeSpeakerObserver.close();
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
      case "media::consumer::pause":
        me.mediaConsumerPause(message, body);
        break;
      case "media::consumer::request::key::frame":
        me.mediaConsumerRequestKeyFrame(message, body);
        break;
      case "media::consumer::resume":
        me.mediaConsumerResume(message, body);
        break;
      case "media::consumer::set::preferred::layers":
        me.mediaConsumerSetPreferredLayers(message, body);
        break;
      case "media::data::consume":
        me.mediaDataConsume(message, body);
        break;
      case "media::data::consumer::close":
        me.mediaDataConsumerClose(message, body);
        break;
      case "media::data::produce":
        me.mediaDataProduce(message, body);
        break;
      case "media::data::producer::close":
        me.mediaDataProducerClose(message, body);
        break;
      case "media::ice::restart":
        me.mediaIceRestart(message, body);
        break;
      case "media::produce":
        me.mediaProduce(message, body);
        break;
      case "media::producer::close":
        me.mediaProducerClose(message, body);
        break;
      case "media::producer::pause":
        me.mediaProducerPause(message, body);
        break;
      case "media::producer::resume":
        me.mediaProducerResume(message, body);
        break;
      case "media::router::rtp::capabilities":
        me.mediaRouterRtpCapabilities(message, body);
        break;
      case "media::transport::close":
        this.mediaTransportClose(message, body);
        break;
      case "media::transport::plain":
        me.mediaTransportPlain(message, body);
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
      // 关键帧延迟时间
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
      console.info("producer score：", producer.id, score);
      self.push(
        protocol.buildMessage("media::producer::score", {
          score: score,
          roomId: roomId,
          producerId: producer.id,
        })
      );
    });
    producer.on("videoorientationchange", (videoOrientation) => {
      console.info("producer videoorientationchange：", producer.id, videoOrientation);
      self.push(protocol.buildMessage("media::video::orientation::change", {
        ...videoOrientation,
        roomId: roomId,
      }));
    });
    // await producer.enableTraceEvent([ 'rtp', 'keyframe', 'nack', 'pli', 'fir' ]);
    // producer.on("trace", (trace) => {
    //   console.info("producer trace：", producer.id, trace);
    // });
    producer.observer.on("close", () => {
      if(room.producers.delete(producer.id)) {
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
      this.push(
        protocol.buildMessage("media::producer::pause", {
          roomId: roomId,
          producerId: producer.id
        })
      );
    });
    producer.observer.on("resume", () => {
      console.info("producer resume：", producer.id);
      this.push(
        protocol.buildMessage("media::producer::resume", {
          roomId: roomId,
          producerId: producer.id
        })
      );
    });
    // producer.observer.on("score", fn(score));
    // producer.observer.on("videoorientationchange", fn(videoOrientation));
    // producer.observer.on("trace", fn(trace));
    message.body = { kind: kind, roomId: roomId, producerId: producer.id };
    this.push(message);
    if (producer.kind === "audio") {
      room.audioLevelObserver
        .addProducer({ producerId: producer.id })
        .catch((error) => {
          console.error("音量监听异常：", error);
        });
      room.activeSpeakerObserver
        .addProducer({ producerId: producer.id })
        .catch((error) => {
          console.error("声音监听异常：", error);
        });
    }
  }

  /**
   * 关闭生产者信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaProducerClose(message, body) {
    const {
      roomId,
      producerId,
    } = body;
    const room = this.rooms.get(roomId);
    const producer = room?.producers.get(producerId);
    if(producer) {
      console.info("关闭生产者：", producerId);
      await producer.close();
    } else {
      console.info("关闭生产者无效：", producerId);
    }
  }

  /**
   * 暂停生产者信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
   async mediaProducerPause(message, body) {
    const {
      roomId,
      producerId,
    } = body;
    const room = this.rooms.get(roomId);
    const producer = room.producers.get(producerId);
    if(producer) {
      console.info("暂停生产者：", producerId);
      await producer.pause();
    } else {
      console.info("暂停生产者无效：", producerId);
    }
  }

  /**
   * 恢复生产者信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
   async mediaProducerResume(message, body) {
    const {
      roomId,
      producerId,
    } = body;
    const room = this.rooms.get(roomId);
    const producer = room.producers.get(producerId);
    if(producer) {
      console.info("恢复生产者：", producerId);
      await producer.resume();
    } else {
      console.info("恢复生产者无效：", producerId);
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
    const producer = room?.producers.get(producerId);
    const transport = room?.transports.get(transportId);
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
            consumer.close();
          });
          consumer.on("producerclose", () => {
            console.info("consumer producerclose：", consumer.id);
            consumer.close();
          });
          consumer.on("producerpause", () => {
            console.info("consumer producerpause：", consumer.id);
            // consumer.pause();
            this.push(
              protocol.buildMessage("media::consumer::pause", {
                roomId: roomId,
                consumerId: consumer.id,
              })
            );
          });
          consumer.on("producerresume", () => {
            console.info("consumer producerresume：", consumer.id);
            // consumer.resume();
            this.push(
              protocol.buildMessage("media::consumer::resume", {
                roomId: roomId,
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
                roomId: roomId,
                consumerId: consumer.id,
                spatialLayer: layers ? layers.spatialLayer : null,
                temporalLayer: layers ? layers.temporalLayer : null,
              })
            );
          });
					// await consumer.enableTraceEvent([ 'rtp', 'keyframe', 'nack', 'pli', 'fir' ]);
          // consumer.on("trace", (trace) => {
          //   console.info("consumer trace：", consumer.id, trace);
          // });
          // consumer.on("rtp", (rtpPacket) => {
          //   console.info("consumer rtp：", consumer.id, rtpPacket);
          // });
          consumer.observer.on("close", () => {
            if(room.consumers.delete(consumer.id)) {
              console.info("consumer close：", consumer.id);
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
            console.info("consumer pause：", consumer.id);
            this.push(
              protocol.buildMessage("media::consumer::pause", {
                roomId: roomId,
                consumerId: consumer.id
              })
            );
          });
          consumer.observer.on("resume", () => {
            console.info("consumer resume：", consumer.id);
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
          await this.request(
          // this.push(
            protocol.buildMessage("media::consume", {
              kind: consumer.kind,
              type: consumer.type,
              roomId: roomId,
              appData: producer.appData,
              clientId: clientId,
              sourceId: sourceId,
              streamId: streamId,
              producerId: producerId,
              consumerId: consumer.id,
              rtpParameters: consumer.rtpParameters,
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
  async mediaConsumerClose(message, body) {
    const { roomId, consumerId } = body;
    const room = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(consumer) {
      console.info("关闭消费者：", consumerId);
      await consumer.close();
    } else {
      console.info("关闭消费者无效：", consumerId);
    }
  }

  /**
   * 暂停消费者信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
   async mediaConsumerPause(message, body) {
    const { roomId, consumerId } = body;
    const room = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(consumer) {
      console.info("暂停消费者：", consumerId);
      await consumer.pause();
    } else {
      console.info("暂停消费者无效：", consumerId);
    }
  }

  /**
   * 请求关键帧信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaConsumerRequestKeyFrame(message, body) {
    const me = this;
    const { roomId, consumerId } = body;
    const room = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(consumer) {
      console.info("mediaConsumerRequestKeyFrame：", consumerId);
      // 处理trace监听读取关键帧
      await consumer.requestKeyFrame();
      me.push(message);
    } else {
      console.info("mediaConsumerRequestKeyFrame non：", consumerId);
    }
  }

  /**
   * 恢复消费者信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
   async mediaConsumerResume(message, body) {
    const { roomId, consumerId } = body;
    const room = this.rooms.get(roomId);
    const consumer = room.consumers.get(consumerId);
    if(consumer) {
      console.info("恢复消费者：", consumerId);
      await consumer.resume();
    } else {
      console.info("恢复消费者无效：", consumerId);
    }
  }

  /**
   * 修改最佳空间层和时间层信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaConsumerSetPreferredLayers(message, body) {
    const me = this;
    const { roomId, consumerId, spatialLayer, temporalLayer } = body;
    const room = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(consumer) {
      console.info("mediaConsumerSetPreferredLayers：", consumerId);
      await consumer.setPreferredLayers({ spatialLayer, temporalLayer });
      me.push(message);
    } else {
      console.info("mediaConsumerSetPreferredLayers non：", consumerId);
    }
  }

  /**
   * 消费数据信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaDataConsume(message, body) {
    const me = this;
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
    const transport = room?.transports.get(transportId);
    const dataProducer = room?.dataProducers.get(producerId);
    if (
      !room ||
      !transport ||
      !dataProducer
    ) {
      console.warn(
        "不能消费数据：",
        roomId,
        clientId,
        producerId,
        transportId
      );
      return;
    }
		let dataConsumer;
		try {
			dataConsumer = await transport.consumeData({
        dataProducerId : dataProducer.id
      });
		} catch (error) {
      console.error("消费数据异常：", producerId, error);
      return;
		}
    dataConsumer.clientId = clientId;
    dataConsumer.streamId = streamId;
		room.dataConsumers.set(dataConsumer.id, dataConsumer);
    console.info("创建数据消费者：", dataProducer.id);
		dataConsumer.on('transportclose', () => {
      console.info("dataConsumer transportclose：", dataConsumer.id);
			dataConsumer.close();
		});
		dataConsumer.on('dataproducerclose', () => {
      console.info("dataConsumer dataproducerclose：", dataConsumer.id);
      dataConsumer.close();
		});
    dataConsumer.observer.on("close", () => {
      if(room.dataConsumers.delete(dataConsumer.id)) {
        console.info("dataConsumer close：", dataConsumer.id);
        me.push(
          protocol.buildMessage("media::data::consumer::close", {
            roomId: roomId,
            consumerId: dataConsumer.id,
          })
        );
      } else {
        console.info("dataConsumer close non：", dataConsumer.id);
      }
    });
    // dataConsumer.on("message", fn(message, ppid));
    // dataConsumer.on("bufferedamountlow", fn(bufferedAmount));
    // dataConsumer.on("sctpsendbufferfull", fn());
    this.push(
      protocol.buildMessage("media::data::consume", {
        label: dataConsumer.label,
        roomId: roomId,
        appData: dataProducer.appData,
        protocol: dataConsumer.protocol,
        clientId: clientId,
        sourceId: sourceId,
        streamId: streamId,
        producerId: producerId,
        consumerId: dataConsumer.id,
        sctpStreamParameters: dataConsumer.sctpStreamParameters,
      })
    );
  }

  /**
   * 关闭数据消费者信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaDataConsumerClose(message, body) {
    const { roomId, consumerId } = body;
    const room = this.rooms.get(roomId);
    const dataConsumer = room?.dataConsumers.get(consumerId);
    if(dataConsumer) {
      console.info("关闭数据消费者：", consumerId);
      await dataConsumer.close();
    } else {
      console.info("关闭数据消费者无效：", consumerId);
    }
  }

  /**
   * 生产数据信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaDataProduce(message, body) {
    const me = this;
    const {
      label,
      roomId,
      appData,
      clientId,
      streamId,
      protocol,
      transportId,
      sctpStreamParameters,
    } = body;
    const room = me.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    if(!transport) {
      console.warn("生产数据生产者通道无效：", transportId);
      return;
    }
    const dataProducer = await transport.produceData({
      label,
      appData,
      protocol,
      sctpStreamParameters,
    });
    dataProducer.clientId = clientId;
    dataProducer.streamId = streamId;
    room.dataProducers.set(dataProducer.id, dataProducer);
    console.info("创建数据生产者：", dataProducer.id);
    dataProducer.on("transportclose", () => {
      console.info("dataProducer transportclose：", dataProducer.id);
      dataProducer.close();
    });
    dataProducer.observer.on("close", () => {
      if(room.dataProducers.delete(dataProducer.id)) {
        console.info("dataProducer close：", dataProducer.id);
        me.push(
          taoyaoProtocol.buildMessage("media::data::producer::close", {
            roomId: roomId,
            producerId: dataProducer.id,
          })
        );
      } else {
        console.info("dataProducer close non：", dataProducer.id);
      }
    })
    message.body = { roomId: roomId, producerId: dataProducer.id };
    this.push(message);
  }

  /**
   * 关闭数据生产者信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
   async mediaDataProducerClose(message, body) {
    const { roomId, producerId } = body;
    const room = this.rooms.get(roomId);
    const dataProducer = room?.dataProducers.get(producerId);
    if(dataProducer) {
      console.info("关闭数据生产者：", producerId);
      await dataProducer.close();
    } else {
      console.info("关闭数据生产者无效：", producerId);
    }
  }

  /**
   * 重启ICE信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaIceRestart(message, body) {
    const me = this;
    const { roomId, transportId } = body;
    const room = this.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    const iceParameters = await transport.restartIce();
    message.body.iceParameters = iceParameters;
    me.push(message);
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
   * 关闭传输通道信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaTransportClose(message, body) {
    const { roomId, transportId } = body;
    const room = this.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    if(transport) {
      console.info("关闭传输通道：", transportId);
      transport.close();
    } else {
      console.info("关闭传输通道无效：", transportId);
    }
  }

  /**
   * 创建RTP输入通道信令
   * 
   * @param {*} message 消息
   * @param {*} body 消息主体
   */
  async mediaTransportPlain(message, body) {
    const me = this;
    const { roomId, rtcpMux, comedia, clientId, enableSctp, numSctpStreams, enableSrtp, srtpCryptoSuite } = body;
    const plainTransportOptions = {
      ...config.mediasoup.plainTransportOptions,
      rtcpMux:         rtcpMux,
      comedia:         comedia,
      enableSctp:      enableSctp || Boolean(numSctpStreams),
      numSctpStreams:  numSctpStreams || 0,
      enableSrtp:      enableSrtp,
      srtpCryptoSuite: srtpCryptoSuite,
    };
    const room = this.rooms.get(roomId);
    const transport = await room.mediasoupRouter.createPlainTransport(plainTransportOptions);
    me.transportEvent("plain", roomId, transport);
    transport.clientId = clientId;
    room.transports.set(transport.id, transport);
    console.info(transport.tuple)
    console.info(transport.rtcpTuple)
    message.body = {
      ip          : transport.tuple.localIp,
      port        : transport.tuple.localPort,
      roomId      : roomId,
      rtcpPort    : transport.rtcpTuple ? transport.rtcpTuple.localPort : undefined,
      transportId : transport.id,
    };
    me.push(message);
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
    const transport = room?.transports.get(transportId);
    if(transport) {
      console.info("连接WebRTC通道：", transportId);
      await transport.connect({ dtlsParameters });
      message.body = { roomId: roomId, transportId: transport.id };
      this.push(message);
    } else {
      console.info("连接WebRTC通道无效：", transportId);
    }
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
    self.transportEvent("webrtc", roomId, transport);
    transport.clientId = clientId;
    room.transports.set(transport.id, transport);
    message.body = {
      roomId: roomId,
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
   * 通道事件
   * 
   * @param {*} type      类型：webrtc|plain|pipe|direct
   * @param {*} roomId    房间ID
   * @param {*} transport 通道
   */
  transportEvent(type, roomId, transport) {
    const self = this;
    /********************* 通用通道事件 *********************/
    transport.on("routerclose", () => {
      console.info("transport routerclose：", transport.id);
      transport.close();
    });
    transport.on("listenserverclose", () => {
      console.info("transport listenserverclose：", transport.id);
      transport.close();
    });
    // await transport.enableTraceEvent([ 'probation', 'bwe' ]);
    // transport.on("trace", (trace) => {
    //   // 网络评估
    //   if (trace.type === "bwe" && trace.direction === "out") {
    //     logger.debug("transport downlinkBwe：", trace);
    //   } else {
    //     console.debug("transport trace：", transport.id, trace);
    //   }
    // });
    transport.observer.on("close", () => {
      if(room.transports.delete(transport.id)) {
        console.info("transport close：", transport.id);
        self.push(
          protocol.buildMessage("media::transport::close", {
            roomId: roomId,
            transportId: transport.id,
          })
        );
      } else {
        console.info("transport close non：", transport.id);
      }
    });
    // transport.observer.on("newproducer", (producer) => {
    //   console.info("transport newproducer：", transport.id, producer.id);
    // });
    // transport.observer.on("newconsumer", (consumer) => {
    //   console.info("transport newconsumer：", transport.id, consumer.id);
    // });
    // transport.observer.on("newdataproducer", (dataProducer) => {
    //   console.info("transport newdataproducer：", transport.id, dataProducer.id);
    // });
    // transport.observer.on("newdataconsumer", (dataConsumer) => {
    //   console.info("transport newdataconsumer：", transport.id, dataProducer.id);
    // });
    // transport.observer.on("trace", fn(trace));
    /********************* webRtcTransport通道事件 *********************/
    if("webrtc" === type) {
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
    }
    /********************* plainTransport通道事件 *********************/
    if("plain" === type) {
      // transport.on("tuple", fn(tuple));
      // transport.on("rtcptuple", fn(rtcpTuple));
      // transport.on("sctpstatechange", fn(sctpState));
      // transport.observer.on("tuple", fn(tuple));
      // transport.observer.on("rtcptuple", fn(rtcpTuple));
      // transport.observer.on("sctpstatechange", fn(sctpState));
    }
    /********************* pipeTransport通道事件 *********************/
    if("pipe" === type) {
      // transport.on("sctpstatechange", fn(sctpState));
      // transport.observer.on("sctpstatechange", fn(sctpState));
    }
    /********************* directTransport通道事件 *********************/
    if("rtcp" === type) {
      // transport.on("rtcp", fn(rtcpPacket));
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
    me.push(message);
    console.info("创建房间：", roomId, mediasoupRouter.id);
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
