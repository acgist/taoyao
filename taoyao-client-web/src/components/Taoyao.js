import * as mediasoupClient from "mediasoup-client";
import {
  defaultAudioConfig,
  defaultVideoConfig,
  defaultKsvcEncodings,
  defaultSimulcastEncodings,
  defaultRTCPeerConnectionConfig,
} from "./Config.js";

/**
 * 信令
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
   * @param {*} v 消息版本
   *
   * @returns 信令消息
   */
  buildMessage(signal, body = {}, id, v) {
    if (!signal) {
      throw new Error("信令标识缺失");
    }
    const message = {
      header: {
        v: v || "1.0.0",
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
        const battery = await navigator.getBattery();
        me.push(
          protocol.buildMessage("client::heartbeat", {
            battery: battery.level * 100,
            charging: battery.charging,
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
      me.channel = new WebSocket(me.address);
      me.channel.onopen = async function () {
        console.debug("打开信令通道：", me.address);
        const battery = await navigator.getBattery();
        me.push(
          protocol.buildMessage("client::register", {
            clientId: me.taoyao.clientId,
            name: me.taoyao.name,
            clientType: "WEB",
            battery: battery.level * 100,
            charging: battery.charging,
            username: me.taoyao.username,
            password: me.taoyao.password,
          })
        );
        me.reconnectionTimeout = me.minReconnectionDelay;
        me.taoyao.connect = true;
        me.heartbeat();
        resolve(me.channel);
      };
      me.channel.onclose = async function () {
        console.warn("信令通道关闭：", me.channel);
        me.taoyao.connect = false;
        if (me.reconnection) {
          me.reconnect();
        }
        // 不要失败回调
      };
      me.channel.onerror = async function (e) {
        console.error("信令通道异常：", me.channel, e);
        me.taoyao.connect = false;
        if (me.reconnection) {
          me.reconnect();
        }
        // 不要失败回调
      };
      me.channel.onmessage = async function (e) {
        try {
          console.debug("信令通道消息：", e.data);
          me.taoyao.on(JSON.parse(e.data));
        } catch (error) {
          console.error("处理信令消息异常：", e, error);
        }
      };
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
      signalChannel.channel.send(JSON.stringify(message));
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
 * 远程终端
 */
class RemoteClient {
  
  // 终端名称
  name;
  // 终端标识
  clientId;
  // 音量
  volume = 0;
  // 代理对象
  proxy;

  constructor({
    name,
    clientId,
  }) {
    this.name = name;
    this.clientId = clientId;
  }

  /**
   * 设置音量
   * 
   * @param {*} volume 音量
   */
  setVolume(volume) {
    this.volume = ((volume + 127) / 127 * 100) + "%";
  }

}

/**
 * 桃夭
 */
class Taoyao extends RemoteClient {
  // 信令连接
  connect = false;
  // 信令地址
  host;
  // 信令端口
  port;
  // 信令帐号
  username;
  // 信令密码
  password;
  // 房间标识
  roomId;
  // 回调事件
  callback;
  // 请求回调
  callbackMapping = new Map();
  // 音频媒体配置
  audioConfig = defaultAudioConfig;
  // 视频媒体配置
  videoConfig = defaultVideoConfig;
  // 媒体配置
  mediaConfig;
  // WebRTC配置
  webrtcConfig;
  // 信令通道
  signalChannel;
  // 发送媒体通道
  sendTransport;
  // 接收媒体通道
  recvTransport;
  // 媒体设备
  mediasoupDevice;
  // 是否消费
  consume;
  // 是否生产
  produce;
  // 视频来源：file | camera | screen
  videoSource = "camera";
  // 强制使用TCP
  forceTcp;
  // 强制使用VP9
  forceVP9;
  // 强制使用H264
  forceH264;
  // 同时上送多种质量媒体
  useSimulcast;
  // 是否生产数据
  dataProduce;
  // 是否生产音频
  audioProduce;
  // 是否生成视频
  videoProduce;
  // 数据生产者
  dataProducer;
  // 音频生产者
  audioProducer;
  // 视频生产者
  videoProducer;
  // 消费者：音频、视频、数据
  consumers = new Map();
  // 远程终端
  remoteClients = new Map();

  constructor({
    name,
    clientId,
    host,
    port,
    username,
    password,
    roomId,
    consume = true,
    produce = true,
    audioProduce = true,
    videoProduce = true,
    forceTcp = false,
    dataProduce = true,
  }) {
    super({ name, clientId });
    this.name = name;
    this.clientId = clientId;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.roomId = roomId;
    this.consume = consume;
    this.produce = produce;
    this.dataProduce = produce && dataProduce;
    this.audioProduce = produce && audioProduce;
    this.videoProduce = produce && videoProduce;
    this.forceTcp = forceTcp;
  }

  /**
   * 连接信令
   *
   * @param {*} callback 回调事件
   *
   * @returns
   */
  async connectSignal(callback) {
    const self = this;
    self.callback = callback;
    self.signalChannel = signalChannel;
    signalChannel.taoyao = self;
    return self.signalChannel.connect(
      `wss://${self.host}:${self.port}/websocket.signal`
    );
  }
  /**
   * 异步请求
   *
   * @param {*} message 消息
   * @param {*} callback 回调
   */
  push(message, callback) {
    const me = this;
    // 请求回调
    if (callback) {
      me.callbackMapping.set(message.header.id, callback);
    }
    // 发送消息
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
      // 请求回调
      me.callbackMapping.set(message.header.id, (response) => {
        resolve(response);
        done = true;
        return true;
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
  /************************ 回调 ************************/
  /**
   * 回调策略：
   * 1. 如果注册请求回调，同时执行结果返回true不再执行后面所有回调。
   * 2. 执行前置回调
   * 3. 如果注册全局回调，同时执行结果返回true不再执行后面所有回调。
   * 4. 执行后置回调
   * 
   * @param {*} message 消息
   */
  async on(message) {
    const me = this;
    let done = false;
    // 请求回调
    if (me.callbackMapping.has(message.header.id)) {
      try {
        done = me.callbackMapping.get(message.header.id)(message);
      } finally {
        me.callbackMapping.delete(message.header.id);
      }
    }
    // 前置回调
    if (!done) {
      await me.preCallback(message);
    }
    // 全局回调
    if (!done && me.callback) {
      done = await me.callback(message);
    }
    // 后置回调
    if (!done) {
      await me.postCallback(message);
    }
  }
  /**
   * 前置回调
   *
   * @param {*} message 消息
   */
  async preCallback(message) {
    const me = this;
    switch (message.header.signal) {
      case "client::config":
        me.defaultClientConfig(message);
        break;
      case "client::register":
        me.defaultClientRegister(message);
        break;
      case "media::consume":
        await me.defaultMediaConsume(message);
        break;
      case "platform::error":
        me.defaultPlatformError(message);
        break;
    }
  }
  /**
   * 后置回调
   *
   * @param {*} message 消息
   */
  async postCallback(message) {
    const me = this;
    switch (message.header.signal) {
      case "client::reboot":
        me.defaultClientReboot(message);
        break;
      case "client::shutdown":
        me.defaultClientShutdown(message);
        break;
      case "media::audio::volume":
        me.defaultMediaAudioVolume(message);
        break;
      case "room::client::list":
        me.defaultRoomClientList(message);
        break;
      case "room::close":
        me.defaultRoomClose(message);
        break;
      case "room::enter":
        me.defaultRoomEnter(message);
        break;
      case "platform::error":
        me.callbackError(message);
        break;
    }
  }
  /************************ 信令 ************************/
  /**
   * 终端配置信令
   *
   * @param {*} message 消息
   */
  defaultClientConfig(message) {
    const me = this;
    const { media, webrtc } = message.body;
    const { audio, video } = media;
    me.audioConfig.sampleSize = { min: media.minSampleSize, ideal: audio.sampleSize, max: media.maxSampleSize };
    me.audioConfig.sampleRate = { min: media.minSampleRate, ideal: audio.sampleRate, max: media.maxSampleRate };
    me.videoConfig.width = { min: media.minWidth, ideal: video.width, max: media.maxWidth };
    me.videoConfig.height = { min: media.minHeight, ideal: video.height, max: media.maxHeight };
    me.videoConfig.frameRate = { min: media.minFrameRate, ideal: video.frameRate, max: media.maxFrameRate };
    me.mediaConfig = media;
    me.webrtcConfig = webrtc;
    console.debug("终端配置：", me.audioConfig, me.videoConfig, me.mediaConfig, me.webrtcConfig);
  }
  /**
   * 重启终端信令
   *
   * @param {*} message 消息
   */
  defaultClientReboot(message) {
    console.info("重启终端");
    location.reload();
  }
  /**
   * 终端注册信令
   * 
   * @param {*} message 消息
   */
   defaultClientRegister(message) {
    const { index } = message.body;
    protocol.clientIndex = index;
  }
  /**
   * 关闭终端信令
   *
   * @param {*} message 消息
   */
  defaultClientShutdown(message) {
    console.info("关闭终端");
    window.close();
  }
  /**
   * 终端音量信令
   * 
   * @param {*} message 消息
   */
  defaultMediaAudioVolume(message) {
    const me = this;
    const { roomId, volumes } = message.body;
    // 静音
    if(!volumes || !volumes.length) {
      me.volume = 0;
      me.remoteClients.forEach(v => v.volume = 0);
      return;
    }
    // 声音
    volumes.forEach(v => {
      const { volume, clientId } = v;
      if(me.clientId === clientId) {
        me.setVolume(volume);
      } else {
        const remoteClient = me.remoteClients.get(clientId);
        if(remoteClient) {
          remoteClient.setVolume(volume);
        }
      }
    });
  }
  /**
   * 消费媒体信令
   *
   * @param {*} message 消息
   */
   async defaultMediaConsume(message) {
    const self = this;
    if (!self.consume) {
      console.log("没有消费媒体");
      return;
    }
    const {
      kind,
      type,
      roomId,
      clientId,
      sourceId,
      streamId,
      producerId,
      consumerId,
      rtpParameters,
      appData,
      producerPaused,
    } = message.body;
    try {
      const consumer = await self.recvTransport.consume({
        id: consumerId,
        kind,
        producerId,
        rtpParameters,
        // NOTE: Force streamId to be same in mic and webcam and different
        // in screen sharing so libwebrtc will just try to sync mic and
        // webcam streams from the same remote peer.
        //streamId: `${peerId}-${appData.share ? "share" : "mic-webcam"}`,
        streamId: `${clientId}-${appData.share ? "share" : "mic-webcam"}`,
        appData, // Trick.
      });
      consumer.clientId = clientId;
      consumer.sourceId = sourceId;
      consumer.streamId = streamId;
      self.consumers.set(consumer.id, consumer);
      consumer.on("transportclose", () => {
        self.consumers.delete(consumer.id);
      });
      const { spatialLayers, temporalLayers } =
        mediasoupClient.parseScalabilityMode(
          consumer.rtpParameters.encodings[0].scalabilityMode
        );
      // store.dispatch(
      //   stateActions.addConsumer(
      //     {
      //       id: consumer.id,
      //       type: type,
      //       locallyPaused: false,
      //       remotelyPaused: producerPaused,
      //       rtpParameters: consumer.rtpParameters,
      //       spatialLayers: spatialLayers,
      //       temporalLayers: temporalLayers,
      //       preferredSpatialLayer: spatialLayers - 1,
      //       preferredTemporalLayer: temporalLayers - 1,
      //       priority: 1,
      //       codec: consumer.rtpParameters.codecs[0].mimeType.split("/")[1],
      //       track: consumer.track,
      //     },
      //     peerId
      //   )
      // );
      self.push(message);
      console.debug("远程媒体：", consumer);
      const remoteClient = self.remoteClients.get(consumer.sourceId);
      if(remoteClient && remoteClient.proxy && remoteClient.proxy.media) {
        remoteClient.proxy.media(consumer.track, consumer);
      } else {
        console.warn("远程终端没有实现服务代理：", remoteClient);
      }
      // If audio-only mode is enabled, pause it.
      if (consumer.kind === "video" && !self.videoProduce) {
        // this.pauseConsumer(consumer);
        // TODO：实现
      }
    } catch (error) {
      self.callbackError("消费媒体异常", error);
    }
  }
  /**
   * 平台异常信令
   * 
   * @param {*} message 消息
   */
  defaultPlatformError(message) {
    const { code } = message;
    if(code === "3401") {
      signalChannel.close();
    }
  }
  /**
   * 房间终端列表信令
   * 
   * @param {*} message 消息
   */
  defaultRoomClientList(message) {
    const me = this;
    message.body.forEach(v => {
      if (v.clientId === me.clientId) {
        // 忽略自己
      } else {
        me.remoteClients.set(v.clientId, new RemoteClient(v));
      }
    });
  }
  /**
   * 关闭房间信令
   */
   async roomClose() {
    const me = this;
    if(!me.roomId) {
      console.warn("房间无效：", me.roomId);
      return;
    }
    me.push(protocol.buildMessage("room::close", {
      roomId: me.roomId
    }));
  }
  /**
   * 关闭房间信令
   * 
   * @param {*} message 消息
   */
  defaultRoomClose(message) {
    const me = this;
    const { roomId } = message.body;
    if(me.roomId !== roomId) {
      return;
    }
    console.info("关闭房间：", roomId);
    me.close();
  }
  /**
   * 创建房间信令
   * 
   * @param {*} room 房间
   * 
   * @returns 房间
   */
  async roomCreate(room) {
    const me = this;
    if (!room) {
      me.callbackError("无效房间");
      return;
    }
    const response = await me.request(
      protocol.buildMessage("room::create", room)
    );
    return response.body;
  }
  /**
   * 进入房间信令
   * 
   * @param {*} roomId 房间ID
   * @param {*} password 房间密码
   */
  async roomEnter(roomId, password) {
    const me = this;
    if (!roomId) {
      this.callbackError("无效房间");
      return;
    }
    me.roomId = roomId;
    me.mediasoupDevice = new mediasoupClient.Device();
    const response = await me.request(
      protocol.buildMessage("media::router::rtp::capabilities", {
        roomId: me.roomId
      })
    );
    const routerRtpCapabilities = response.body.rtpCapabilities;
    await me.mediasoupDevice.load({ routerRtpCapabilities });
    await me.request(
      protocol.buildMessage("room::enter", {
        roomId: roomId,
        password: password,
        rtpCapabilities: me.consume ? me.mediasoupDevice.rtpCapabilities : undefined,
        sctpCapabilities: me.consume && me.dataProduce ? me.mediasoupDevice.sctpCapabilities : undefined,
      })
    );
  }
  /**
   * 进入房间信令
   * 
   * @param {*} message 消息
   */
  defaultRoomEnter(message) {
    const me = this;
    const { roomId, clientId, status } = message.body;
    if (clientId === me.clientId) {
      // 忽略自己
    } else {
      me.remoteClients.set(clientId, new RemoteClient(status));
    }
  }
  /**
   * 错误回调
   */
  callbackError(message, error) {
    const self = this;
    if (!self.callback) {
      if (error) {
        console.error("没有注册回调：", message, error);
      } else {
        console.warn("没有注册回调：", message);
      }
      return;
    }
    // 错误回调
    const errorMessage = protocol.buildMessage(
      "platform::error",
      { message },
      -9999
    );
    errorMessage.code = "-9999";
    errorMessage.message = message;
    self.callback(errorMessage, error);
  }
  async roomList() {
    const response = await this.request(protocol.buildMessage("room::list"));
    return response.body;
  }
  async mediaList() {
    const response = await this.request(
      protocol.buildMessage("client::list", { clientType: "MEDIA" })
    );
    return response.body;
  }
  async clientList() {
    const response = await this.request(
      protocol.buildMessage("client::list", { roomId: self.roomId })
    );
    return response.body;
  }
  /************************ 媒体 ************************/
  /**
   * 生产媒体
   */
  async produceMedia() {
    const self = this;
    if (!self.roomId) {
      this.callbackError("无效房间");
      return;
    }
    // 检查设备
    self.checkDevice();
    // 释放资源
    self.closeMedia();
    /**
     * 解决浏览器的自动播放策略问题
     */
    {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getAudioTracks().forEach(audioTrack => {
        audioTrack.enabled = false;
        setTimeout(() => audioTrack.stop(), 30000);
      });
    }
    if (self.produce) {
      const response = await self.request(
        protocol.buildMessage("media::transport::webrtc::create", {
          roomId: self.roomId,
          forceTcp: self.forceTcp,
          producing: true,
          consuming: false,
          sctpCapabilities: self.dataProduce
            ? self.mediasoupDevice.sctpCapabilities
            : undefined,
        })
      );
      const {
        transportId,
        iceParameters,
        iceCandidates,
        dtlsParameters,
        sctpParameters,
      } = response.body;
      self.sendTransport = self.mediasoupDevice.createSendTransport({
        id: transportId,
        iceCandidates,
        iceParameters,
        dtlsParameters: {
          ...dtlsParameters,
          role: "auto",
        },
        sctpParameters,
        // TODO:iceservers
        iceServers: [],
        // Google配置
        proprietaryConstraints: {
          optional: [{ googDscp: true }],
        },
        additionalSettings:
          // TODO：加密解密
          { encodedInsertableStreams: false },
      });
      self.sendTransport.on(
        "connect",
        ({ dtlsParameters }, callback, errback) => {
          self
            .request(
              protocol.buildMessage("media::transport::webrtc::connect", {
                roomId: self.roomId,
                transportId: self.sendTransport.id,
                dtlsParameters,
              })
            )
            .then(callback)
            .catch(errback);
        }
      );
      self.sendTransport.on(
        "produce",
        async ({ kind, appData, rtpParameters }, callback, errback) => {
          try {
            const { producerId } = await self.request(
              protocol.buildMessage("media::produce", {
                kind,
                roomId: self.roomId,
                appData,
                transportId: self.sendTransport.id,
                rtpParameters,
              })
            );
            callback({ id: producerId });
          } catch (error) {
            errback(error);
          }
        }
      );
      self.sendTransport.on(
        "producedata",
        async (
          { label, protocol, appData, sctpStreamParameters },
          callback,
          errback
        ) => {
          try {
            const { id } = await self.request(
              protocol.buildMessage("media::produceData", {
                label,
                appData,
                protocol,
                transportId: self.sendTransport.id,
                sctpStreamParameters,
              })
            );
            callback({ id });
          } catch (error) {
            errback(error);
          }
        }
      );
    }
    if (this.consume) {
      const self = this;
      const response = await self.request(
        protocol.buildMessage("media::transport::webrtc::create", {
          roomId: self.roomId,
          forceTcp: self.forceTcp,
          producing: false,
          consuming: true,
          sctpCapabilities: self.dataProduce
            ? self.mediasoupDevice.sctpCapabilities
            : undefined,
        })
      );
      const {
        transportId,
        iceCandidates,
        iceParameters,
        dtlsParameters,
        sctpParameters,
      } = response.body;
      self.recvTransport = self.mediasoupDevice.createRecvTransport({
        id: transportId,
        iceParameters,
        iceCandidates,
        dtlsParameters: {
          ...dtlsParameters,
          // Remote DTLS role. We know it's always 'auto' by default so, if
          // we want, we can force local WebRTC transport to be 'client' by
          // indicating 'server' here and vice-versa.
          role: "auto",
        },
        iceServers: [],
        sctpParameters,
        additionalSettings: {
          // TODO：加密解密
          encodedInsertableStreams: false,
        },
      });
      self.recvTransport.on(
        "connect",
        (
          { dtlsParameters },
          callback,
          errback // eslint-disable-line no-shadow
        ) => {
          self
            .request(
              protocol.buildMessage("media::transport::webrtc::connect", {
                roomId: self.roomId,
                transportId: self.recvTransport.id,
                dtlsParameters,
              })
            )
            .then(callback)
            .catch(errback);
        }
      );
    }
    this.produceAudio();
    this.produceVideo();
  }
  /**
   * 生产音频
   * TODO：重复点击
   */
  async produceAudio() {
    const self = this;
    if (self.audioProduce && self.mediasoupDevice.canProduce("audio")) {
      if (this.audioProducer) {
        return;
      }
      let track;
      try {
        console.debug("打开麦克风");
        const stream = await navigator.mediaDevices.getUserMedia({
          audio: self.audioConfig,
        });
        const tracks = stream.getAudioTracks();
        if (tracks.length > 1) {
          console.log("多个音频轨道");
        }
        track = tracks[0];
        // TODO：验证修改API audioTrack.applyCapabilities
        console.debug("音频信息：", track.getSettings(), track.getCapabilities());
        this.audioProducer = await this.sendTransport.produce({
          track,
          codecOptions: {
            opusStereo: 1,
            opusDtx: 1,
          },
          // NOTE: for testing codec selection.
          // codec : this._mediasoupDevice.rtpCapabilities.codecs
          // 	.find((codec) => codec.mimeType.toLowerCase() === 'audio/pcma')
        });

        // TODO：加密解密
        // if (this._e2eKey && e2e.isSupported()) {
        //   e2e.setupSenderTransform(this._micProducer.rtpSender);
        // }

        this.audioProducer.on("transportclose", () => {
          this.audioProducer = null;
        });

        this.audioProducer.on("trackended", () => {
          console.warn("audio producer trackended", this.audioProducer);
          this.closeAudioProducer().catch(() => {});
        });
      } catch (error) {
        self.callbackError("麦克风打开异常", error);
        if (track) {
          track.stop();
        }
      }
    } else {
      self.callbackError("麦克风打开失败");
    }
  }
  async closeAudioProducer() {
    console.debug("closeAudioProducer()");
    if (!this.audioProducer) {
      return;
    }
    this.audioProducer.close();
    try {
      await this.request(
        protocol.buildMessage("media::producer::close", {
          producerId: this.audioProducer.id,
        })
      );
    } catch (error) {
      console.error("关闭麦克风异常", error);
    }
    this.audioProducer = null;
  }

  async pauseAudioProducer() {
    console.debug("静音麦克风");
    this.audioProducer.pause();
    try {
      await this.request(
        protocol.buildMessage("media::producer::pause", {
          producerId: this.audioProducer.id,
        })
      );
    } catch (error) {
      console.error("静音麦克风异常", error);
      // TODO：异常调用回调
    }
  }

  async resumeAudioProducer() {
    console.debug("恢复麦克风");
    this.audioProducer.resume();
    try {
      await this.request(
        protocol.buildMessage("media::producer::resume", {
          producerId: this.audioProducer.id,
        })
      );
    } catch (error) {
      console.error("恢复麦克风异常", error);
    }
  }

  /**
   * 生产视频
   * TODO：重复点击
   */
  async produceVideo() {
    console.debug("打开摄像头");
    const self = this;
    if (self.videoProduce && self.mediasoupDevice.canProduce("video")) {
      if (self.videoProducer) {
        return;
      }
      let track;
      try {
        if (self.videoSource === "file") {
          // TODO：实现文件分享
          // const stream = await this._getExternalVideoStream();
          // track = stream.getVideoTracks()[0].clone();
        } else if (self.videoSource === "camera") {
          console.debug("enableWebcam() | calling getUserMedia()");
          // TODO：参数
          const stream = await navigator.mediaDevices.getUserMedia({
            video: self.videoConfig,
          });
          track = stream.getVideoTracks()[0];
          // TODO：验证修改API videoTrack.applyCapabilities
          console.debug("视频信息：", track.getSettings(), track.getCapabilities());
        } else if (self.videoSource === "screen") {
          const stream = await navigator.mediaDevices.getDisplayMedia({
            // 如果需要共享声音
            audio: false,
            video: {
              cursor: true,
              width: { max: 1920 },
              height: { max: 1080 },
              frameRate: { max: 30 },
              logicalSurface: true,
              displaySurface: "monitor",
            },
          });
          track = stream.getVideoTracks()[0];
        } else {
          // TODO：异常
        }
        if(self.proxy && self.proxy.media) {
          self.proxy.media(track);
        } else {
          console.warn("终端没有实现服务代理：", self);
        }
        let codec;
        let encodings;
        const codecOptions = {
          videoGoogleStartBitrate: 1000,
        };
        if (self.forceH264) {
          codec = self.mediasoupDevice.rtpCapabilities.codecs.find(
            (c) => c.mimeType.toLowerCase() === "video/h264"
          );
          if (!codec) {
            throw new Error(
              "desired H264 codec+configuration is not supported"
            );
          }
        } else if (self.forceVP9) {
          codec = self.mediasoupDevice.rtpCapabilities.codecs.find(
            (c) => c.mimeType.toLowerCase() === "video/vp9"
          );
          if (!codec) {
            throw new Error("desired VP9 codec+configuration is not supported");
          }
        }
        if (this.useSimulcast) {
          // If VP9 is the only available video codec then use SVC.
          const firstVideoCodec =
            this.mediasoupDevice.rtpCapabilities.codecs.find(
              (c) => c.kind === "video"
            );
          if (
            (this.forceVP9 && codec) ||
            firstVideoCodec.mimeType.toLowerCase() === "video/vp9"
          ) {
            encodings = defaultKsvcEncodings;
          } else {
            encodings = defaultSimulcastEncodings;
          }
        }
        this.videoProducer = await this.sendTransport.produce({
          codec,
          track,
          encodings,
          codecOptions,
        });

        // if (this._e2eKey && e2e.isSupported()) {
        //   e2e.setupSenderTransform(this.videoProducer.rtpSender);
        // }

        this.videoProducer.on("transportclose", () => {
          this.videoProducer = null;
        });

        this.videoProducer.on("trackended", () => {
          console.warn("video producer trackended", this.audioProducer);
          this.closeVideoProducer().catch(() => {});
        });
      } catch (error) {
        self.callbackError("摄像头打开异常", error);
        if (track) {
          track.stop();
        }
      }
    } else {
      console.error("打开摄像头失败");
    }
  }

  async closeVideoProducer() {
    console.debug("disableWebcam()");
    if (!this.videoProducer) {
      return;
    }
    this.videoProducer.close();
    try {
      await this.request(
        protocol.buildMessage("media::producer::close", {
          producerId: this.videoProducer.id,
        })
      );
    } catch (error) {
      console.error(error);
    }

    this._webcamProducer = null;
  }

  async pauseVideoProducer() {
    console.debug("关闭摄像头");
    this.videoProducer.pause();
    try {
      await this.request(
        protocol.buildMessage("media::producer::pause", {
          producerId: this.videoProducer.id,
        })
      );
    } catch (error) {
      console.error("关闭摄像头异常", error);
      // TODO：异常调用回调
    }
  }

  async resumeVideoProducer() {
    console.debug("恢复摄像头");
    this.videoProducer.resume();
    try {
      await this.request(
        protocol.buildMessage("media::producer::resume", {
          producerId: this.videoProducer.id,
        })
      );
    } catch (error) {
      console.error("恢复摄像头异常", error);
    }
  }

  async updateVideoConfig(config) {
    console.debug("更新摄像头参数");
    try {
      this.videoProducer.track.stop();
      // TODO：screen、参数配置
      const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
      });
      const track = stream.getVideoTracks()[0];
      await this.videoProducer.replaceTrack({ track });
    } catch (error) {
      console.error("changeWebcam() | failed: %o", error);
    }
  }

  async pauseConsumer(consumer) {
    if (consumer.paused) return;
    try {
      await this._protoo.request("pauseConsumer", { consumerId: consumer.id });
      consumer.pause();
    } catch (error) {
      logger.error("_pauseConsumer() | failed:%o", error);
    }
  }

  async resumeConsumer(consumer) {
    if (!consumer.paused) return;
    try {
      await this._protoo.request("resumeConsumer", { consumerId: consumer.id });
      consumer.resume();
    } catch (error) {
      logger.error("_resumeConsumer() | failed:%o", error);
    }
  }

  /**
   * 验证设备
   */
  async checkDevice() {
    const self = this;
    if (
      self.produce &&
      navigator.mediaDevices &&
      navigator.mediaDevices.getUserMedia &&
      navigator.mediaDevices.enumerateDevices
    ) {
      let audioEnabled = false;
      let videoEnabled = false;
      (await navigator.mediaDevices.enumerateDevices()).forEach((v) => {
        console.debug("终端媒体设备", v, v.kind, v.label);
        switch (v.kind) {
          case "audioinput":
            audioEnabled = true;
            break;
          case "videoinput":
            videoEnabled = true;
            break;
          default:
            console.debug("没有适配设备", v.kind, v.label);
            break;
        }
      });
      if (!audioEnabled && self.audioProduce) {
        self.callbackError("没有音频媒体设备");
      }
      if (!videoEnabled && self.videoProduce) {
        self.callbackError("没有视频媒体设备");
      }
    } else {
      self.callbackError("没有媒体设备");
    }
  }

  async restartIce() {
    const self = this;
    try {
      if (self.sendTransport) {
        const response = await self.request("media::ice::restart", {
          roomId: self.roomId,
          transportId: self.sendTransport.id,
        });
        const iceParameters = response.data.iceParameters;
        await self.sendTransport.restartIce({ iceParameters });
      }
      if (self.recvTransport) {
        const response = await self.request("media::ice::restart", {
          roomId: self.roomId,
          transportId: self.recvTransport.id,
        });
        const iceParameters = response.data.iceParameters;
        await self.recvTransport.restartIce({ iceParameters });
      }
    } catch (error) {
      self.callbackError("重启ICE失败", error);
    }
  }

  /**
   * 关闭媒体
   */
  closeMedia = function () {
    let self = this;
    if (self.sendTransport) {
      self.sendTransport.close();
    }
    if (self.recvTransport) {
      self.recvTransport.close();
    }
  };
  /**
   * 关闭
   */
  close = function () {
    let self = this;
    self.closeMedia();
    if (self.signalChannel) {
      self.signalChannel.close();
    }
  };
}

export { Taoyao };
