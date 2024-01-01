const fs        = require("fs");
const config    = require("./Config");
const process   = require("child_process");
const WebSocket = require("ws");

/**
 * 信令协议
 */
const protocol = {

  // 当前索引
  index      : 0,
  // 最大索引
  maxIndex   : 999,
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
        v     : v  || config.signal.version,
        id    : id || this.buildId(),
        signal: signal,
      },
      body: body,
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

  // 桃夭信令
  taoyao : null,
  // 信令通道
  channel: null,
  // 信令地址
  address: null,
  // 心跳时间
  heartbeatTime : 30 * 1000,
  // 心跳定时器
  heartbeatTimer: null,
  // 是否重连
  reconnection  : true,
  // 防止重复重连
  lockReconnect : false,
  // 重连定时器
  reconnectTimer: null,
  // 当前重连时间
  reconnectionTimeout : 5 * 1000,
  // 最小重连时间
  minReconnectionDelay: 5 * 1000,
  // 最大重连时间
  maxReconnectionDelay: 30 * 1000,

  /**
   * 心跳
   */
  heartbeat() {
    if (this.heartbeatTimer) {
      clearTimeout(this.heartbeatTimer);
    }
    this.heartbeatTimer = setTimeout(async () => {
      if (this.connected()) {
        this.taoyao.push(protocol.buildMessage("client::heartbeat", {
          // TODO：电池信息
          battery : 100,
          charging: true,
        }));
        this.heartbeat();
      } else {
        console.warn("心跳失败", this.address);
      }
    }, this.heartbeatTime);
  },

  /**
   * @returns 是否连接成功
   */
  connected() {
    return this.channel && this.channel.readyState === WebSocket.OPEN;
  },

  /**
   * 连接信令
   *
   * @param {*} address      信令地址
   * @param {*} reconnection 是否重连
   *
   * @returns Promise<WebSocket>
   */
  async connect(address, reconnection = true) {
    if (this.connected()) {
      this.taoyao.connect = true;
      return new Promise((resolve, reject) => {
        resolve(this.channel);
      });
    } else {
      this.taoyao.connect = false;
    }
    this.address      = address;
    this.reconnection = reconnection;
    return new Promise((resolve, reject) => {
      console.debug("连接信令通道", this.address);
      this.channel = new WebSocket(this.address, { rejectUnauthorized: false, handshakeTimeout: 5000 });
      this.channel.on("open", async () => {
        console.debug("打开信令通道", this.address);
        const {
          body
        } = await this.taoyao.request(protocol.buildMessage("client::register", {
          name      : config.signal.name,
          clientId  : config.signal.clientId,
          clientType: config.signal.clientType,
          username  : config.signal.username,
          password  : config.signal.password,
          // TODO：电池信息
          battery   : 100,
          charging  : true,
        }));
        protocol.clientIndex     = body.index;
        this.taoyao.connect      = true;
        this.reconnectionTimeout = this.minReconnectionDelay;
        console.debug("终端注册成功", protocol.clientIndex);
        this.heartbeat();
        resolve(this.channel);
      });
      this.channel.on("close", async () => {
        console.warn("信令通道关闭", this.address);
        this.taoyao.connect = false;
        this.taoyao.closeAllRoom();
        if (this.reconnection) {
          this.reconnect();
        }
        // 不要失败回调
      });
      this.channel.on("error", async (e) => {
        console.error("信令通道异常", this.address, e);
        // 不要失败回调
      });
      this.channel.on("message", async (data) => {
        const content = data.toString();
        try {
          console.debug("信令通道消息", content);
          this.taoyao.on(JSON.parse(content));
        } catch (error) {
          console.error("处理信令通道消息异常", content, error);
        }
      });
    });
  },

  /**
   * 重连信令
   */
  reconnect() {
    if (this.connected() || this.lockReconnect) {
      return;
    }
    this.lockReconnect = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    // 定时重连
    this.reconnectTimer = setTimeout(() => {
      console.debug("重连信令通道", this.address);
      this.connect(this.address, this.reconnection);
      this.lockReconnect = false;
    }, this.reconnectionTimeout);
    // 设置重连时间
    this.reconnectionTimeout = Math.min(
      this.reconnectionTimeout + this.minReconnectionDelay,
      this.maxReconnectionDelay
    );
  },

  /**
   * 关闭通道
   */
  close() {
    console.debug("关闭信令通道", this.address);
    clearTimeout(this.heartbeatTimer);
    clearTimeout(this.reconnectTimer);
    this.reconnection   = false;
    this.taoyao.connect = false;
    this.channel.close();
  },

};

/**
 * 房间
 */
class Room {
  
  // 是否关闭
  close  = null;
  // 房间ID
  roomId = null;
  // 桃夭信令
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
  // 媒体通道
  transports = new Map();
  // 媒体生产者
  producers  = new Map();
  // 媒体消费者
  consumers  = new Map();
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
    this.close  = false;
    this.roomId = roomId;
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
    // 静音监控
    this.audioLevelObserver.on("silence", () => {
      this.taoyao.push(protocol.buildMessage("media::audio::volume", {
        roomId: this.roomId,
      }));
    });
    // this.audioLevelObserver.observer.on("silence", () => {});
    // 音量监控
    this.audioLevelObserver.on("volumes", (volumes) => {
      const notifyVolumes = [];
      for (const value of volumes) {
        const {
          volume,
          producer
        } = value;
        notifyVolumes.push({
          volume  : volume,
          clientId: producer.clientId
        });
      }
      this.taoyao.push(protocol.buildMessage("media::audio::volume", {
        roomId : this.roomId,
        volumes: notifyVolumes
      }));
    });
    // this.audioLevelObserver.observer.on("volumes", (volumes) => {});
  }

  /**
   * 当前讲话终端监控
   */
  handleActiveSpeakerObserver() {
    // 不用通知直接使用音量监控即可
    this.activeSpeakerObserver.on("dominantspeaker", (dominantSpeaker) => {
      const producer = dominantSpeaker.producer;
      console.debug("当前讲话终端", producer.id, producer.clientId);
    });
    // this.activeSpeakerObserver.observer.on("dominantspeaker", (dominantSpeaker) => {});
  }

  /**
   * 房间使用情况
   */
  usage() {
    console.info("房间标识",          this.roomId);
    console.info("房间媒体通道数量",   this.transports.size);
    console.info("房间媒体生产者数量", this.producers.size);
    console.info("房间媒体消费者数量", this.consumers.size);
    console.info("房间数据生产者数量", this.dataProducers.size);
    console.info("房间数据消费者数量", this.dataConsumers.size);
  }

  /**
   * 关闭房间
   */
  closeAll() {
    if (this.close) {
      return;
    }
    console.info("关闭房间", this.roomId);
    this.close = true;
    this.audioLevelObserver.close();
    this.activeSpeakerObserver.close();
    this.consumers.forEach(v => v.close());
    this.producers.forEach(v => v.close());
    this.dataConsumers.forEach(v => v.close());
    this.dataProducers.forEach(v => v.close());
    this.transports.forEach(v => v.close());
    this.mediasoupRouter.close();
  }

};

/**
 * 桃夭信令
 */
class Taoyao {

  // 是否连接
  connect = false;
  // 房间列表：房间ID=房间
  rooms = new Map();
  // 回调事件：消息ID=事件
  callbackMapping = new Map();
  // Worker列表
  mediasoupWorkers = [];
  // Worker索引
  nextMediasoupWorkerIndex = 0;

  constructor(mediasoupWorkers) {
    console.info("加载媒体桃夭信令");
    this.mediasoupWorkers = mediasoupWorkers;
    // 定时打印使用情况
    setInterval(async () => {
      this.usage();
    }, 60 * 1000);
  }

  /**
   * 处理信令消息
   *
   * @param {*} message 消息
   */
  on(message) {
    // 解构
    const {
      code,
      header,
      body
    } = message;
    const {
      id,
      signal
    } = header;
    if(code !== "0000") {
      console.warn("信令错误", message);
    }
    // 请求回调
    if (this.callbackMapping.has(id)) {
      try {
        this.callbackMapping.get(id)(message);
      } finally {
        this.callbackMapping.delete(id);
      }
      return;
    }
    // 执行信令
    switch (signal) {
      case "client::reboot":
        this.clientReboot(message, body);
        break;
      case "client::shutdown":
        this.clientShutdown(message, body);
        break;
      case "control::server::record":
        this.controlServerRecord(message, body);
        break;
      case "media::consume":
        this.mediaConsume(message, body);
        break;
      case "media::consumer::close":
        this.mediaConsumerClose(message, body);
        break;
      case "media::consumer::pause":
        this.mediaConsumerPause(message, body);
        break;
      case "media::consumer::request::key::frame":
        this.mediaConsumerRequestKeyFrame(message, body);
        break;
      case "media::consumer::resume":
        this.mediaConsumerResume(message, body);
        break;
      case "media::consumer::set::preferred::layers":
        this.mediaConsumerSetPreferredLayers(message, body);
        break;
      case "media::consumer::set::priority":
        this.mediaConsumerSetPriority(message, body);
        break;
      case "media::consumer::status":
        this.mediaConsumerStatus(message, body);
        break;
      case "media::data::consume":
        this.mediaDataConsume(message, body);
        break;
      case "media::data::consumer::close":
        this.mediaDataConsumerClose(message, body);
        break;
      case "media::data::consumer::status":
        this.mediaDataConsumerStatus(message, body);
        break;
      case "media::data::produce":
        this.mediaDataProduce(message, body);
        break;
      case "media::data::producer::close":
        this.mediaDataProducerClose(message, body);
        break;
      case "media::data::producer::status":
        this.mediaDataProducerStatus(message, body);
        break;
      case "media::ice::restart":
        this.mediaIceRestart(message, body);
        break;
      case "media::produce":
        this.mediaProduce(message, body);
        break;
      case "media::producer::close":
        this.mediaProducerClose(message, body);
        break;
      case "media::producer::pause":
        this.mediaProducerPause(message, body);
        break;
      case "media::producer::resume":
        this.mediaProducerResume(message, body);
        break;
      case "media::producer::status":
        this.mediaProducerStatus(message, body);
        break;
      case "media::router::rtp::capabilities":
        this.mediaRouterRtpCapabilities(message, body);
        break;
      case "media::transport::close":
        this.mediaTransportClose(message, body);
        break;
      case "media::transport::plain::create":
        this.mediaTransportPlainCreate(message, body);
        break;
      case "media::transport::status":
        this.mediaTransportStatus(message, body);
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
      case "room::close":
        this.roomClose(message, body);
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
      console.error("异步请求异常", message, error);
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
    return new Promise((resolve, reject) => {
      const { header, body } = message;
      const { id }           = header;
      // 设置超时
      const rejectTimeout = setTimeout(() => {
        this.callbackMapping.delete(id);
        reject("请求超时", message);
      }, 5000);
      // 请求回调
      this.callbackMapping.set(id, (response) => {
        resolve(response);
        clearTimeout(rejectTimeout);
        return true;
      });
      // 发送消息
      try {
        signalChannel.channel.send(JSON.stringify(message));
      } catch (error) {
        reject("同步请求异常", error);
      }
    });
  }

  /**
   * 打印日志
   */
  async usage() {
    for (const worker of this.mediasoupWorkers) {
      const usage = await worker.getResourceUsage();
      console.info("工作线程使用情况", worker.pid, usage);
    }
    console.info("工作线程数量", this.mediasoupWorkers.length);
    console.info("现存房间数量", this.rooms.size);
    Array.from(this.rooms.values()).forEach((room) => room.usage());
  }

  /**
   * @returns 下个工作线程
   */
  nextMediasoupWorker() {
    const worker = this.mediasoupWorkers[this.nextMediasoupWorkerIndex];
    this.nextMediasoupWorkerIndex = ++this.nextMediasoupWorkerIndex % this.mediasoupWorkers.length;
    return worker;
  }
  
  /**
   * 关闭所有房间
   */
  closeAllRoom() {
    if(this.rooms.size <= 0) {
      return;
    }
    console.info("关闭所有房间", this.rooms.size);
    this.rooms.forEach((room, roomId) => room.closeAll());
    this.rooms.clear();
  }

  /**
   * 重启终端信令
   *
   * @param {*} message 消息
   * @param {*} body    消息主体
   */
  clientReboot(message, body) {
    const { clientId } = config.signal;
    process.exec(
      `pm2 restart ${clientId}`,
      (error, stdout, stderr) => {
        console.info("重启媒体服务", clientId, error, stdout, stderr);
      }
    );
  }

  /**
   * 关闭终端信令
   *
   * @param {*} message 消息
   * @param {*} body    消息主体
   */
  clientShutdown(message, body) {
    const { clientId } = config.signal;
    process.exec(
      `pm2 stop ${clientId}`,
      (error, stdout, stderr) => {
        console.info("关闭媒体服务", clientId, error, stdout, stderr);
      }
    );
  }

  /**
   * 服务端录像信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async controlServerRecord(message, body) {
    const {
      roomId,
      enabled,
    } = body;
    const room = this.rooms.get(roomId);
    if(!room) {
      // 直接关闭房间时，房间关闭可能早于结束录像。
      console.debug("服务端录像房间无效", roomId);
      return;
    }
    if(enabled) {
      await this.controlServerRecordStart(message, body, room);
    } else {
      await this.controlServerRecordStop(message, body, room);
    }
  }

  /**
   * 开始服务端录像
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   * @param {*} room    房间
   */
  async controlServerRecordStart(message, body, room) {
    const {
      host,
      roomId,
      filepath,
      clientId,
      audioPort, audioRtcpPort,
      videoPort, videoRtcpPort,
      rtpCapabilities,
      audioStreamId,   videoStreamId,
      audioProducerId, videoProducerId
    } = body;
    console.info("开始服务端录像", clientId, audioStreamId, videoStreamId);
    const plainTransportOptions = {
      ...config.mediasoup.plainTransportOptions,
      // RTP和RTCP端口复用
      rtcpMux: false,
      // 自动终端端口
      comedia: false
    };
    let audioConsumer;
    let videoConsumer;
    let audioConsumerId;
    let videoConsumerId;
    let audioTransportId;
    let videoTransportId;
    let audioRtpParameters;
    let videoRtpParameters;
    if(audioProducerId) {
      const audioTransport = await room.mediasoupRouter.createPlainTransport(plainTransportOptions);
      audioTransportId     = audioTransport.id;
      room.transports.set(audioTransportId, audioTransport);
      this.transportEvent("plain", roomId, audioTransport);
      audioTransport.clientId = clientId;
      await audioTransport.connect({
        ip      : host,
        port    : audioPort,
        rtcpPort: audioRtcpPort
      });
      audioConsumer = await audioTransport.consume({
        paused    : true,
        producerId: audioProducerId,
        rtpCapabilities,
      });
      audioConsumerId    = audioConsumer.id;
      audioRtpParameters = audioConsumer.rtpParameters;
      audioConsumer.clientId = clientId;
      audioConsumer.streamId = audioStreamId;
      room.consumers.set(audioConsumerId, audioConsumer);
      audioConsumer.observer.on("close", () => {
        console.debug("关闭服务端录像音频消费者", audioConsumerId);
        room.consumers.delete(audioConsumerId);
      });
      console.debug("创建服务器录像音频消费者", audioTransportId, audioConsumerId, audioTransport.tuple, audioRtpParameters.codecs);
    }
    if(videoProducerId) {
      const videoTransport = await room.mediasoupRouter.createPlainTransport(plainTransportOptions);
      videoTransportId     = videoTransport.id;
      room.transports.set(videoTransportId, videoTransport);
      this.transportEvent("plain", roomId, videoTransport);
      videoTransport.clientId = clientId;
      await videoTransport.connect({
        ip      : host,
        port    : videoPort,
        rtcpPort: videoRtcpPort
      });
      videoConsumer = await videoTransport.consume({
        paused    : true,
        producerId: videoProducerId,
        rtpCapabilities,
      });
      videoConsumerId    = videoConsumer.id;
      videoRtpParameters = videoConsumer.rtpParameters;
      videoConsumer.clientId = clientId;
      videoConsumer.streamId = videoStreamId;
      room.consumers.set(videoConsumerId, videoConsumer);
      videoConsumer.observer.on("close", () => {
        console.debug("关闭服务器录像视频消费者", videoConsumerId);
        room.consumers.delete(videoConsumerId);
      });
      console.debug("创建服务器录像视频消费者", videoTransportId, videoConsumerId, videoTransport.tuple, videoRtpParameters.codecs);
    }
    if(audioConsumer) {
      await audioConsumer.resume();
    }
    if(videoConsumer) {
      await videoConsumer.resume();
    }
    try {
      // 请求录像关键帧
      this.requestKeyFrameForRecord(0, filepath, videoConsumer);
    } catch (error) {
      console.error("请求录像关键帧异常", error);
    }
    message.body = {
      roomId            : roomId,
      audioConsumerId   : audioConsumerId,
      videoConsumerId   : videoConsumerId,
      audioTransportId  : audioTransportId,
      videoTransportId  : videoTransportId,
      audioRtpParameters: audioRtpParameters,
      videoRtpParameters: videoRtpParameters,
    };
    this.push(message);
  }

  /**
   * 请求录像关键帧
   * 视频录像需要通过关键帧解析视频信息，关键帧数据太慢会丢弃视频数据包，导致录像文件只有音频没有视频。
   * 
   * @param {*} index         重试次数
   * @param {*} filepath      文件路径
   * @param {*} videoConsumer 视频消费者
   */
  requestKeyFrameForRecord(index, filepath, videoConsumer) {
    const {
      requestKeyFrameMaxIndex,
      requestKeyFrameFileSize
    } = config.record;
    if(index >= requestKeyFrameMaxIndex) {
      console.warn("请求录像关键帧次数超限", filepath, index);
      return;
    }
    if(videoConsumer.closed) {
      console.warn("请求录像关键帧视频关闭", filepath, index);
      return;
    }
    // 判断文件大小验证是否已经开始录像：创建文件 -> 视频信息 -> 视频数据 -> 封装视频
    if(fs.existsSync(filepath) && fs.statSync(filepath).size >= requestKeyFrameFileSize) {
      console.info("请求录像关键帧已经开始录像", filepath, index);
      return;
    }
    console.debug("请求录像关键帧", filepath, index);
    videoConsumer.requestKeyFrame();
    setTimeout(() => {
      this.requestKeyFrameForRecord(++index, filepath, videoConsumer);
    }, 1000);
  }

  /**
   * 结束服务端录像
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   * @param {*} room    房间
   */
  async controlServerRecordStop(message, body, room) {
    const {
      audioStreamId,    videoStreamId,
      audioConsumerId,  videoConsumerId,
      audioTransportId, videoTransportId
    } = body;
    console.info("结束服务端录像", audioStreamId, videoStreamId);
    const audioConsumer = room.consumers.get(audioConsumerId);
    if(audioConsumer) {
      await audioConsumer.close();
    }
    const videoConsumer = room.consumers.get(videoConsumerId);
    if(videoConsumer) {
      await videoConsumer.close();
    }
    const audioTransport = room.transports.get(audioTransportId);
    if(audioTransport) {
      await audioTransport.close();
    }
    const videoTransport = room.transports.get(videoTransportId);
    if(videoTransport) {
      await videoTransport.close();
    }
    this.push(message);
  }

  /**
   * 消费媒体信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaConsume(message, body) {
    const {
      roomId,
      clientId,
      sourceId,
      streamId,
      producerId,
      transportId,
      appData,
      rtpCapabilities,
    } = body;
    const room      = this.rooms.get(roomId);
    const producer  = room?.producers.get(producerId);
    const transport = room?.transports.get(transportId);
    if (
      !room            ||
      !producer        ||
      !transport       ||
      !rtpCapabilities ||
      !room.mediasoupRouter.canConsume({
        producerId,
        rtpCapabilities,
      })
    ) {
      console.warn("不能消费媒体", body);
      return;
    }
    const promises      = [];
    const consumerCount = room.consumerReplicas + 1;
    for (let i = 0; i < consumerCount; i++) {
      promises.push(
        (async () => {
          const consumer = await transport.consume({
            paused: true,
            producerId,
            rtpCapabilities,
          });
          consumer.clientId = clientId;
          consumer.streamId = streamId;
          room.consumers.set(consumer.id, consumer);
          console.debug("创建消费者", consumer.id, streamId);
          consumer.on("transportclose", () => {
            console.debug("消费者关闭（通道关闭）", consumer.id, streamId);
            consumer.close();
          });
          consumer.on("producerclose", () => {
            console.debug("消费者关闭（生产者关闭）", consumer.id, streamId);
            consumer.close();
          });
          consumer.on("producerpause", () => {
            // 本地暂停不要操作
            if(consumer.localPaused) {
              return;
            }
            console.debug("消费者暂停（生产者暂停）", consumer.id, streamId);
            consumer.pause();
          });
          consumer.on("producerresume", () => {
            // 本地暂停不要操作
            if(consumer.localPaused) {
              return;
            }
            console.debug("消费者恢复（生产者恢复）", consumer.id, streamId);
            consumer.resume();
          });
          // consumer.observer.on("score", fn(score));
          consumer.on("score", (score) => {
            console.debug("消费者评分", consumer.id, streamId, score);
            this.push(protocol.buildMessage("media::consumer::score", {
              score,
              roomId,
              consumerId: consumer.id,
            }));
          });
          // consumer.observer.on("layerschange", fn(layers));
          consumer.on("layerschange", (layers) => {
            console.debug("消费者空间层和时间层改变", consumer.id, streamId, layers);
            this.push(protocol.buildMessage("media::consumer::layers::change", {
              roomId,
              consumerId   : consumer.id,
              spatialLayer : layers?.spatialLayer,
              temporalLayer: layers?.temporalLayer,
            }));
          });
          consumer.observer.on("close", () => {
            if(room.consumers.delete(consumer.id)) {
              console.debug("消费者关闭", consumer.id, streamId);
              this.push(protocol.buildMessage("media::consumer::close", {
                roomId,
                consumerId: consumer.id
              }));
            } else {
              console.debug("消费者关闭（消费者无效）", consumer.id, streamId);
            }
          });
          consumer.observer.on("pause", () => {
            console.debug("消费者暂停", consumer.id, streamId);
            this.push(protocol.buildMessage("media::consumer::pause", {
              roomId,
              consumerId: consumer.id
            }));
          });
          consumer.observer.on("resume", () => {
            console.debug("消费者恢复", consumer.id, streamId);
            this.push(protocol.buildMessage("media::consumer::resume", {
              roomId,
              consumerId: consumer.id
            }));
          });
					// await consumer.enableTraceEvent([ 'pli', 'fir', 'rtp', 'nack', 'keyframe' ]);
          // consumer.observer.on("trace", fn(trace));
          // consumer.on("trace", (trace) => {
          //   console.debug("消费者跟踪事件（trace）", consumer.id, streamId, trace);
          // });
          // 等待终端准备就绪：必须等待就绪不然容易导致SSRC重复异常
          await this.request(protocol.buildMessage("media::consume", {
            roomId,
            clientId,
            sourceId,
            streamId,
            producerId,
            consumerId    : consumer.id,
            kind          : consumer.kind,
            type          : consumer.type,
            appData       : producer.appData,
            rtpParameters : consumer.rtpParameters,
            producerPaused: consumer.producerPaused,
          }));
          await consumer.resume();
          consumer.localPaused = false;
        })()
      );
    }
    try {
      await Promise.all(promises);
    } catch (error) {
      console.error("消费媒体异常", error);
    }
  }

  /**
   * 关闭消费者信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaConsumerClose(message, body) {
    const {
      roomId,
      consumerId
    } = body;
    const room     = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(!consumer) {
      console.debug("关闭消费者（消费者无效）", roomId, consumerId);
      return;
    }
    console.debug("关闭消费者", consumerId);
    await consumer.close();
  }

  /**
   * 暂停消费者信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
   async mediaConsumerPause(message, body) {
    const {
      roomId,
      consumerId
    } = body;
    const room     = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(!consumer) {
      console.warn("暂停消费者（消费者无效）", roomId, consumerId);
      return;
    }
    consumer.localPaused = true;
    console.debug("暂停消费者", consumerId);
    await consumer.pause();
  }

  /**
   * 请求关键帧信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaConsumerRequestKeyFrame(message, body) {
    const {
      roomId,
      consumerId
    } = body;
    const room     = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(!consumer) {
      console.debug("请求关键帧（消费者无效）", roomId, consumerId);
      return;
    }
    console.debug("请求关键帧", consumerId);
    // 通过trace事件监听关键帧的信息
    await consumer.requestKeyFrame();
  }

  /**
   * 恢复消费者信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
   async mediaConsumerResume(message, body) {
    const {
      roomId,
      consumerId
    } = body;
    const room     = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(!consumer) {
      console.warn("恢复消费者（消费者无效）", roomId, consumerId);
      return;
    }
    consumer.localPaused = false;
    console.debug("恢复消费者", consumerId);
    await consumer.resume();
  }

  /**
   * 修改最佳空间层和时间层信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaConsumerSetPreferredLayers(message, body) {
    const {
      roomId,
      consumerId,
      spatialLayer,
      temporalLayer,
    } = body;
    const room     = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(!consumer) {
      console.debug("修改最佳空间层和时间层（消费者无效）", roomId, consumerId);
      return;
    }
    console.debug("修改最佳空间层和时间层", consumerId);
    await consumer.setPreferredLayers({
      spatialLayer,
      temporalLayer
    });
  }

  /**
   * 设置消费者优先级信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaConsumerSetPriority(message, body) {
    const {
      roomId,
      consumerId,
      priority,
    } = body;
    const room     = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(!consumer) {
      console.debug("设置消费者优先级（消费者无效）", roomId, consumerId);
      return;
    }
    console.debug("设置消费者优先级", consumerId, priority);
    if(priority <= 0 || priority >= 256) {
      await consumer.unsetPriority();
    } else {
      await consumer.setPriority(priority);
    }
  }

  /**
   * 查询消费者状态信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaConsumerStatus(message, body) {
    const {
      roomId,
      consumerId,
    } = body;
    const room     = this.rooms.get(roomId);
    const consumer = room?.consumers.get(consumerId);
    if(!consumer) {
      console.debug("查询消费者状态（消费者无效）", roomId, consumerId);
      return;
    }
    console.debug("查询消费者状态", consumerId);
    message.body = {
      ...body,
      status: await consumer.getStats()
    };
    this.push(message);
  }

  /**
   * 消费数据信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaDataConsume(message, body) {
    const {
      roomId,
      clientId,
      sourceId,
      streamId,
      producerId,
      transportId,
      rtpCapabilities,
    } = body;
    const room         = this.rooms.get(roomId);
    const transport    = room?.transports.get(transportId);
    const dataProducer = room?.dataProducers.get(producerId);
    if (
      !room      ||
      !transport ||
      !dataProducer
    ) {
      console.warn("不能消费数据", body);
      return;
    }
		let dataConsumer;
		try {
			dataConsumer = await transport.consumeData({
        dataProducerId : dataProducer.id
      });
		} catch (error) {
      console.error("创建数据消费者异常", body, error);
      return;
		}
    dataConsumer.clientId = clientId;
    dataConsumer.streamId = streamId;
		room.dataConsumers.set(dataConsumer.id, dataConsumer);
    console.debug("创建数据消费者", dataProducer.id, streamId);
		dataConsumer.on("transportclose", () => {
      console.debug("数据消费者关闭（通道关闭）", dataConsumer.id, streamId);
			dataConsumer.close();
		});
		dataConsumer.on("dataproducerclose", () => {
      console.debug("数据消费者关闭（生产者关闭）", dataConsumer.id, streamId);
      dataConsumer.close();
		});
    // dataConsumer.on("bufferedamountlow",  fn(bufferedAmount));
    // dataConsumer.on("sctpsendbufferfull", fn());
    dataConsumer.observer.on("close", () => {
      if(room.dataConsumers.delete(dataConsumer.id)) {
        console.debug("数据消费者关闭", dataConsumer.id, streamId);
        this.push(protocol.buildMessage("media::data::consumer::close", {
          roomId,
          consumerId: dataConsumer.id,
        }));
      } else {
        console.debug("数据消费者关闭（数据消费者无效）", dataConsumer.id, streamId);
      }
    });
    this.push(protocol.buildMessage("media::data::consume", {
      roomId              : roomId,
      clientId            : clientId,
      sourceId            : sourceId,
      streamId            : streamId,
      producerId          : producerId,
      consumerId          : dataConsumer.id,
      label               : dataConsumer.label,
      appData             : dataProducer.appData,
      protocol            : dataConsumer.protocol,
      sctpStreamParameters: dataConsumer.sctpStreamParameters,
    }));
  }

  /**
   * 关闭数据消费者信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaDataConsumerClose(message, body) {
    const {
      roomId,
      consumerId
    } = body;
    const room         = this.rooms.get(roomId);
    const dataConsumer = room?.dataConsumers.get(consumerId);
    if(!dataConsumer) {
      console.debug("关闭数据消费者（数据消费者无效）", roomId, consumerId);
      return;
    }
    console.debug("关闭数据消费者", consumerId);
    await dataConsumer.close();
  }

  /**
   * 查询数据消费者状态信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaDataConsumerStatus(message, body) {
    const {
      roomId,
      consumerId,
    } = body;
    const room         = this.rooms.get(roomId);
    const dataConsumer = room?.dataConsumers.get(consumerId);
    if(!dataConsumer) {
      console.warn("查询数据消费者状态（数据消费者无效）", roomId, consumerId);
      return;
    }
    console.debug("查询数据消费者状态", consumerId);
    message.body = {
      ...body,
      status: await dataConsumer.getStats()
    };
    this.push(message);
  }

  /**
   * 生产数据信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaDataProduce(message, body) {
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
    const room      = this.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    if(!transport) {
      console.warn("生产数据通道（通道无效）", roomId, transportId);
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
    console.debug("创建数据生产者", dataProducer.id, streamId);
    dataProducer.on("transportclose", () => {
      console.debug("数据生产者关闭（通道关闭）", dataProducer.id, streamId);
      dataProducer.close();
    });
    dataProducer.observer.on("close", () => {
      if(room.dataProducers.delete(dataProducer.id)) {
        console.debug("数据生产者关闭", dataProducer.id, streamId);
        this.push(taoyaoProtocol.buildMessage("media::data::producer::close", {
          roomId,
          producerId: dataProducer.id,
        }));
      } else {
        console.debug("数据生产者关闭（数据生产者无效）", dataProducer.id, streamId);
      }
    })
    message.body = {
      roomId,
      producerId: dataProducer.id
    };
    this.push(message);
  }

  /**
   * 关闭数据生产者信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
   async mediaDataProducerClose(message, body) {
    const {
      roomId,
      producerId
    } = body;
    const room         = this.rooms.get(roomId);
    const dataProducer = room?.dataProducers.get(producerId);
    if(!dataProducer) {
      console.debug("关闭数据生产者（数据生产者无效）", roomId, producerId);
      return;
    }
    console.debug("关闭数据生产者", producerId);
    await dataProducer.close();
  }

  /**
   * 查询数据生产者状态信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaDataProducerStatus(message, body) {
    const {
      roomId,
      producerId,
    } = body;
    const room         = this.rooms.get(roomId);
    const dataProducer = room?.dataProducers.get(producerId);
    if(!dataProducer) {
      console.warn("查询数据生产者状态（数据生产者无效）", roomId, producerId);
      return;
    }
    console.debug("查询数据生产者状态", producerId);
    message.body = {
      ...body,
      status: await dataProducer.getStats()
    };
    this.push(message);
  }

  /**
   * 重启ICE信令
   *
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaIceRestart(message, body) {
    const {
      roomId,
      transportId
    } = body;
    const room      = this.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    if(!transport) {
      console.warn("重启ICE（通道无效）", roomId, transportId);
      return;
    }
    console.debug("重启ICE", transportId);
    message.body = {
      ...body,
      iceParameters: await transport.restartIce()
    };
    this.push(message);
  }

  /**
   * 生产媒体信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaProduce(message, body) {
    const {
      kind,
      roomId,
      clientId,
      streamId,
      transportId,
      appData,
      rtpParameters
    } = body;
    const room      = this.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    if(!transport) {
      console.warn("生产媒体（通道无效）", roomId, transportId);
      return;
    }
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
    console.debug("创建生产者", producer.id, streamId);
    producer.on("transportclose", () => {
      console.debug("生产者关闭（通道关闭）", producer.id, streamId);
      producer.close();
    });
    producer.observer.on("close", () => {
      if(room.producers.delete(producer.id)) {
        console.debug("生产者关闭", producer.id, streamId);
        // 生产者关闭时自动删除
        // if(producer.kind === "audio") {
        //   room.audioLevelObserver
        //     .removeProducer({ producerId: producer.id })
        //     .then(() => console.debug("删除音量监听", clientId, streamId))
        //     .catch((error) => {
        //       console.error("删除音量监听", clientId, streamId, error);
        //     });
        //   room.activeSpeakerObserver
        //     .removeProducer({ producerId: producer.id })
        //     .then(() => console.debug("删除声音监听", clientId, streamId))
        //     .catch((error) => {
        //       console.error("删除声音监听", clientId, streamId, error);
        //     });
        // }
        this.push(protocol.buildMessage("media::producer::close", {
          roomId,
          producerId: producer.id
        }));
      } else {
        console.debug("生产者关闭（生产者无效）", producer.id, streamId);
      }
    });
    producer.observer.on("pause", () => {
      console.debug("生产者暂停", producer.id, streamId);
      this.push(protocol.buildMessage("media::producer::pause", {
        roomId,
        producerId: producer.id
      }));
    });
    producer.observer.on("resume", () => {
      console.debug("生产者恢复", producer.id, streamId);
      this.push(protocol.buildMessage("media::producer::resume", {
        roomId,
        producerId: producer.id
      }));
    });
    // producer.observer.on("score", fn(score));
    producer.on("score", (score) => {
      console.debug("生产者评分", producer.id, streamId, score);
      this.push(protocol.buildMessage("media::producer::score", {
        score,
        roomId,
        producerId: producer.id,
      }));
    });
    // producer.observer.on("videoorientationchange", fn(videoOrientation));
    producer.on("videoorientationchange", (videoOrientation) => {
      console.debug("生产者视频方向改变", producer.id, streamId, videoOrientation);
      this.push(protocol.buildMessage("media::video::orientation::change", {
        ...videoOrientation,
        roomId,
        producerId: producer.id,
      }));
    });
    // await producer.enableTraceEvent([ 'pli', 'fir', 'rtp', 'nack', 'keyframe' ]);
    // producer.observer.on("trace", fn(trace));
    // producer.on("trace", (trace) => {
    //   console.debug("生产者跟踪事件（trace）", producer.id, streamId, trace);
    // });
    message.body = {
      kind      : kind,
      roomId    : roomId,
      producerId: producer.id
    };
    this.push(message);
    if (producer.kind === "audio") {
      room.audioLevelObserver
        .addProducer({ producerId: producer.id })
        .catch((error) => {
          console.error("音量监听异常", error);
        });
      room.activeSpeakerObserver
        .addProducer({ producerId: producer.id })
        .catch((error) => {
          console.error("声音监听异常", error);
        });
    }
  }

  /**
   * 关闭生产者信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaProducerClose(message, body) {
    const {
      roomId,
      producerId
    } = body;
    const room     = this.rooms.get(roomId);
    const producer = room?.producers.get(producerId);
    if(!producer) {
      console.debug("关闭生产者（生产者无效）", roomId, producerId);
      return;
    }
    console.debug("关闭生产者", producerId);
    await producer.close();
  }

  /**
   * 暂停生产者信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaProducerPause(message, body) {
    const {
      roomId,
      producerId
    } = body;
    const room     = this.rooms.get(roomId);
    const producer = room?.producers.get(producerId);
    if(!producer) {
      console.warn("暂停生产者（生产者无效）", roomId, producerId);
      return;
    }
    console.debug("暂停生产者", producerId);
    await producer.pause();
  }

 /**
  * 恢复生产者信令
  * 
  * @param {*} message 信令消息
  * @param {*} body    消息主体
  */
  async mediaProducerResume(message, body) {
    const {
      roomId,
      producerId
    } = body;
    const room     = this.rooms.get(roomId);
    const producer = room?.producers.get(producerId);
    if(!producer) {
      console.warn("恢复生产者（生产者无效）", roomId, producerId);
      return;
    }
    console.debug("恢复生产者", producerId);
    await producer.resume();
  }

  /**
   * 查询生产者状态信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaProducerStatus(message, body) {
    const {
      roomId,
      producerId,
    } = body;
    const room     = this.rooms.get(roomId);
    const producer = room?.producers.get(producerId);
    if(!producer) {
      console.warn("查询生产者状态（生产者无效）", roomId, producerId);
      return;
    }
    console.debug("查询生产者状态", producerId);
    message.body = {
      ...body,
      status: await producer.getStats()
    };
    this.push(message);
  }

  /**
   * 路由RTP协商信令
   *
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  mediaRouterRtpCapabilities(message, body) {
    const {
      roomId
    } = body;
    const room            = this.rooms.get(roomId);
    const rtpCapabilities = room?.mediasoupRouter.rtpCapabilities;
    message.body = {
      ...body,
      rtpCapabilities
    };
    this.push(message);
  }

  /**
   * 关闭传输通道信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaTransportClose(message, body) {
    const {
      roomId,
      transportId
    } = body;
    const room      = this.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    if(!transport) {
      console.debug("关闭传输通道（通道无效）", roomId, transportId);
      return;
    }
    console.debug("关闭传输通道", transportId);
    await transport.close();
  }

  /**
   * 创建RTP输入通道信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaTransportPlainCreate(message, body) {
    const {
      roomId,
      clientId,
      rtcpMux,
      comedia,
      enableSctp,
      numSctpStreams,
      enableSrtp,
      srtpCryptoSuite
    } = body;
    const plainTransportOptions = {
      ...config.mediasoup.plainTransportOptions,
      rtcpMux         : rtcpMux,
      comedia         : comedia,
      enableSctp      : enableSctp     || Boolean(numSctpStreams),
      numSctpStreams  : numSctpStreams || 0,
      enableSrtp      : enableSrtp,
      srtpCryptoSuite : srtpCryptoSuite,
    };
    const room = this.rooms.get(roomId);
    if(!room) {
      console.warn("创建RTP输入通道（房间无效）", roomId);
      return;
    }
    const transport = await room?.mediasoupRouter.createPlainTransport(plainTransportOptions);
    console.debug("创建RTP输入通道", transport.id);
    this.transportEvent("plain", roomId, transport);
    transport.clientId = clientId;
    room.transports.set(transport.id, transport);
    message.body = {
      roomId     : roomId,
      transportId: transport.id,
      ip         : transport.tuple.localIp,
      port       : transport.tuple.localPort,
      rtcpPort   : transport.rtcpTuple?.localPort,
    };
    this.push(message);
  }

  /**
   * 查询通道状态信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaTransportStatus(message, body) {
    const {
      roomId,
      transportId,
    } = body;
    const room      = this.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    if(!transport) {
      console.warn("查询通道状态（通道无效）", roomId, transportId);
      return;
    }
    console.debug("查询通道状态", transportId);
    message.body = {
      ...body,
      status: await transport.getStats()
    };
    this.push(message);
  }

  /**
   * 连接WebRTC通道信令
   *
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaTransportWebrtcConnect(message, body) {
    const {
      roomId,
      transportId,
      dtlsParameters
    } = body;
    const room      = this.rooms.get(roomId);
    const transport = room?.transports.get(transportId);
    if(!transport) {
      console.warn("连接WebRTC通道（通道无效）", roomId, transportId);
      return;
    }
    await transport.connect({
      dtlsParameters
    });
    console.debug("连接WebRTC通道", transportId);
    message.body = {
      roomId     : roomId,
      transportId: transport.id
    };
    this.push(message);
  }

  /**
   * 创建WebRTC通道信令
   *
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async mediaTransportWebrtcCreate(message, body) {
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
      appData: {
        producing,
        consuming
      },
      enableSctp    : Boolean(sctpCapabilities),
      numSctpStreams: (sctpCapabilities || {}).numStreams,
    };
    if (forceTcp) {
      webRtcTransportOptions.enableUdp = false;
      webRtcTransportOptions.enableTcp = true;
    }
    const room = this.rooms.get(roomId);
    if(!room) {
      console.warn("创建WebRTC通道（房间无效）", roomId);
      return;
    }
    const transport = await room.mediasoupRouter.createWebRtcTransport({
      ...webRtcTransportOptions,
      webRtcServer: room.webRtcServer,
    });
    console.debug("创建WebRTC通道", transport.id);
    this.transportEvent("webrtc", roomId, transport);
    transport.clientId = clientId;
    room.transports.set(transport.id, transport);
    message.body = {
      roomId        : roomId,
      transportId   : transport.id,
      iceCandidates : transport.iceCandidates,
      iceParameters : transport.iceParameters,
      dtlsParameters: transport.dtlsParameters,
      sctpParameters: transport.sctpParameters,
    };
    this.push(message);
    const {
      maxOutgoingBitrate,
      maxIncomingBitrate,
    } = config.mediasoup.webRtcTransportOptions;
    if(maxOutgoingBitrate) {
      await transport.setMaxOutgoingBitrate(maxOutgoingBitrate);
    }
    if(maxIncomingBitrate) {
      await transport.setMaxIncomingBitrate(maxIncomingBitrate);
    }
  }

  /**
   * 通道事件
   * 
   * @param {*} type      类型：pipe|plain|direct|webrtc
   * @param {*} roomId    房间ID
   * @param {*} transport 通道
   */
  transportEvent(type, roomId, transport) {
    const room        = this.rooms.get(roomId);
    const transportId = transport.id;
    /********************* 通用通道事件 *********************/
    transport.on("routerclose", () => {
      console.debug("通道关闭（路由关闭）", roomId, transportId);
      transport.close();
    });
    transport.on("listenserverclose", () => {
      console.debug("通道关闭（监听服务关闭）", roomId, transportId);
      transport.close();
    });
    transport.observer.on("close", () => {
      if(room.transports.delete(transportId)) {
        console.debug("通道关闭", roomId, transportId);
        this.push(protocol.buildMessage("media::transport::close", {
          roomId,
          transportId,
        }));
      } else {
        console.debug("通道关闭（通道无效）", roomId, transportId);
      }
    });
    // transport.observer.on("newproducer",     (producer) => {});
    // transport.observer.on("newconsumer",     (consumer) => {});
    // transport.observer.on("newdataproducer", (dataProducer) => {});
    // transport.observer.on("newdataconsumer", (dataConsumer) => {});
    // 设置跟踪事件
    // await transport.enableTraceEvent([ 'bwe', 'probation' ]);
    // transport.on("trace",                    (trace) => {});
    // transport.observer.on("trace",           fn(trace));
    /********************* pipeTransport通道事件 *********************/
    if("pipe" === type) {
      // transport.on("sctpstatechange",          fn(sctpState));
      // transport.observer.on("sctpstatechange", fn(sctpState));
    }
    /********************* plainTransport通道事件 *********************/
    if("plain" === type) {
      // transport.on("tuple",                    fn(tuple));
      // transport.on("rtcptuple",                fn(rtcpTuple));
      // transport.on("sctpstatechange",          fn(sctpState));
      // transport.observer.on("tuple",           fn(tuple));
      // transport.observer.on("rtcptuple",       fn(rtcpTuple));
      // transport.observer.on("sctpstatechange", fn(sctpState));
    }
    /********************* directTransport通道事件 *********************/
    if("direct" === type) {
      // transport.on("rtcp", fn(rtcpPacket));
    }
    /********************* webRtcTransport通道事件 *********************/
    if("webrtc" === type) {
      // transport.on("icestatechange",                  (iceState) => {});
      // transport.on("iceselectedtuplechange",          (iceSelectedTuple) => {});
      // transport.on("dtlsstatechange",                 (dtlsState) => {});
      // transport.on("sctpstatechange",                 (sctpState) => {});
      // transport.observer.on("icestatechange",         fn(iceState));
      // transport.observer.on("iceselectedtuplechange", fn(iceSelectedTuple));
      // transport.observer.on("dtlsstatechange",        fn(dtlsState));
      // transport.observer.on("sctpstatechange",        fn(sctpState));
    }
  }

  /**
   * 平台异常信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  platformError(message, body) {
    const {
      code
    } = message;
    if(code === "3401") {
      signalChannel.close();
      console.warn("授权异常（关闭信令）", message);
    } else {
      console.warn("平台异常", message);
    }
  }

  /**
   * 关闭房间信令
   * 
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async roomClose(message, body) {
    const {
      roomId
    } = body;
    const room = this.rooms.get(roomId);
    if(!room) {
      console.warn("关闭房间（房间无效）", roomId);
      return;
    }
    room.closeAll();
  }

  /**
   * 创建房间信令
   *
   * @param {*} message 信令消息
   * @param {*} body    消息主体
   */
  async roomCreate(message, body) {
    const {
      roomId
    } = body;
    if (this.rooms.has(roomId)) {
      console.warn("创建房间（已经存在）", roomId);
      this.push(message);
      return;
    }
    const {
      mediaCodecs
    } = config.mediasoup.routerOptions;
    const mediasoupWorker = this.nextMediasoupWorker();
    const mediasoupRouter = await mediasoupWorker.createRouter({
      mediaCodecs
    });
    // 音量监控
    const audioLevelObserver = await mediasoupRouter.createAudioLevelObserver({
      // 监控周期
      interval  : 500,
      // 监控范围：-127~0
      threshold : -127,
      // 监控数量
      maxEntries: 2,
    });
    // 采样监控
    const activeSpeakerObserver = await mediasoupRouter.createActiveSpeakerObserver({
      interval: 500,
    });
    const room = new Room({
      roomId,
      mediasoupRouter,
      audioLevelObserver,
      activeSpeakerObserver,
      taoyao      : this,
      webRtcServer: mediasoupWorker.appData.webRtcServer,
    });
    console.debug("创建房间", roomId, mediasoupRouter.id);
    this.rooms.set(roomId, room);
    this.push(message);
    mediasoupRouter.on("workerclose", () => {
      console.debug("路由关闭（工作线程关闭）", roomId, mediasoupRouter.id);
      mediasoupRouter.close();
    });
    mediasoupRouter.observer.on("close", () => {
      if(this.rooms.delete(roomId)) {
        console.debug("路由关闭", roomId, mediasoupRouter.id);
        room.closeAll();
        this.push(protocol.buildMessage("room::close", {
            roomId
        }));
      } else {
        console.debug("路由关闭（房间无效）", roomId, mediasoupRouter.id);
      }
    });
    // mediasoupRouter.observer.on("newtransport",   (transport)   => {});
    // mediasoupRouter.observer.on("newrtpobserver", (rtpObserver) => {});
  }
};

module.exports = {
  Taoyao,
  signalChannel
};
