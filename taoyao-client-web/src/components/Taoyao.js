/**
 * 桃夭
 */
import { TaoyaoClient } from "./TaoyaoClient.js";
import * as mediasoupClient from "mediasoup-client";
import {
  protocol,
  defaultAudioConfig,
  defaultVideoConfig,
  defaultRTCPeerConnectionConfig,
} from "./Config.js";

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
  // 回调
  callback: null,
  // 回调事件
  callbackMapping: new Map(),
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
        const battery = await navigator.getBattery();
        // TODO：信号强度
        self.push(
          protocol.buildMessage("client::heartbeat", {
            signal: 100,
            battery: battery.level * 100,
            charging: battery.charging,
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
      self.channel = new WebSocket(self.address);
      self.channel.onopen = async function () {
        console.debug("打开信令通道：", self.address);
        // 注册终端
        // TODO：信号强度
        const battery = await navigator.getBattery();
        self.push(
          protocol.buildMessage("client::register", {
            name: "桃夭Web",
            clientId: self.taoyao.clientId,
            clientType: "WEB",
            signal: 100,
            battery: battery.level * 100,
            charging: battery.charging,
            username: self.taoyao.username,
            password: self.taoyao.password,
          })
        );
        // 重置时间
        self.connectionTimeout = self.minReconnectionDelay;
        // 开始心跳
        self.heartbeat();
        // 成功回调
        resolve(self.channel);
      };
      self.channel.onclose = async function () {
        console.warn("信令通道关闭：", self.channel);
        if (self.reconnection) {
          self.reconnect();
        }
        // 不要失败回调
      };
      self.channel.onerror = async function (e) {
        console.error("信令通道异常：", self.channel, e);
        if (self.reconnection) {
          self.reconnect();
        }
        // 不要失败回调
      };
      /**
       * 回调策略：
       * 1. 如果注册请求回调，同时执行结果返回true不再执行后面所有回调。
       * 2. 执行前置回调
       * 3. 如果注册全局回调，同时执行结果返回true不再执行后面所有回调。
       * 3. 执行后置回调
       */
      self.channel.onmessage = async function (e) {
        console.debug("信令通道消息：", e.data);
        let done = false;
        const message = JSON.parse(e.data);
        // 请求回调
        if (self.callbackMapping.has(message.header.id)) {
          try {
            done = self.callbackMapping.get(message.header.id)(message);
          } finally {
            self.callbackMapping.delete(message.header.id);
          }
        }
        // 前置回调
        if (!done) {
          await self.preCallback(message);
        }
        // 全局回调
        if (!done && self.callback) {
          done = await self.callback(message);
        }
        // 后置回调
        if (!done) {
          await self.postCallback(message);
        }
      };
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
   * @param {*} callback 回调
   */
  push(message, callback) {
    const self = this;
    // 注册回调
    if (callback) {
      self.callbackMapping.set(message.header.id, callback);
    }
    // 发送消息
    try {
      self.channel.send(JSON.stringify(message));
    } catch (error) {
      console.error("推送消息异常：", message, error);
    }
  },
  /**
   * 同步请求
   *
   * @param {*} message 消息
   *
   * @returns Promise
   */
  async request(message) {
    const self = this;
    return new Promise((resolve, reject) => {
      let done = false;
      // 注册回调
      self.callbackMapping.set(message.header.id, (response) => {
        resolve(response);
        done = true;
        // 返回true不要继续执行回调
        return true;
      });
      // 发送消息
      try {
        self.channel.send(JSON.stringify(message));
      } catch (error) {
        console.error("请求消息异常：", message, error);
      }
      // 设置超时
      setTimeout(() => {
        if (!done) {
          self.callbackMapping.delete(message.header.id);
          reject("请求超时", message);
        }
      }, 5000);
    });
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
  /**
   * 前置回调
   *
   * @param {*} message
   */
  async preCallback(message) {
    const self = this;
    switch (message.header.signal) {
      case "client::config":
        self.defaultClientConfig(message);
        break;
      case "client::register":
        console.info("终端注册成功");
        break;
      case "media::consume":
        await self.taoyao.consumeMedia(message);
        break;
    }
  },
  /**
   * 后置回调
   *
   * @param {*} message 消息
   */
  async postCallback(message) {
    const self = this;
    switch (message.header.signal) {
      case "client::reboot":
        self.defaultClientReboot(message);
        break;
      case "client::shutdown":
        self.defaultClientShutdown(message);
        break;
      case "platform::error":
        self.callbackError(message);
        break;
    }
  },
  /**
   * 配置默认回调
   *
   * @param {*} message 消息
   */
  defaultClientConfig(message) {
    const self = this;
    self.taoyao.audio = { ...defaultAudioConfig, ...message.body.media.audio };
    self.taoyao.video = { ...defaultVideoConfig, ...message.body.media.video };
    self.taoyao.webrtc = message.body.webrtc;
    console.debug(
      "终端配置",
      self.taoyao.audio,
      self.taoyao.video,
      self.taoyao.webrtc
    );
  },
  /**
   * 终端重启默认回调
   *
   * @param {*} message 消息
   */
  defaultClientReboot(message) {
    console.info("重启终端");
    location.reload();
  },
  /**
   * 终端重启默认回调
   *
   * @param {*} message 消息
   */
  defaultClientShutdown(message) {
    console.info("关闭终端");
    window.close();
  },
};

/**
 * 桃夭
 */
class Taoyao {
  // 房间标识
  roomId;
  // 终端标识
  clientId;
  // 信令地址
  host;
  // 信令端口
  port;
  // 信令帐号
  username;
  // 信令密码
  password;
  // 回调事件
  callback;
  // 音频媒体配置
  audio;
  // 视频媒体配置
  video;
  // WebRTC配置
  webrtc;
  // 发送信令
  push;
  // 请求信令
  request;
  // 本地终端
  localClient;
  // 远程终端
  remoteClients = new Map();
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
  // 是否生产音频
  audioProduce;
  // 是否生成视频
  videoProduce;
  // 强制使用TCP
  forceTcp;
  // 使用数据通道
  useDataChannel;
  // 音频生产者
  audioProducer;
  // 视频生产者
  videoProducer;
  // 数据生产者
  dataChannnelProducer;
  // 媒体消费者
  consumers = new Map();
  // 数据消费者
  dataConsumers = new Map();

  constructor({
    roomId,
    clientId,
    host,
    port,
    username,
    password,
    consume = true,
    produce = true,
    audioProduce = true,
    videoProduce = true,
    forceTcp = false,
    useDataChannel = true,
  }) {
    this.roomId = roomId;
    this.clientId = clientId;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.consume = consume;
    this.produce = produce;
    this.audioProduce = produce && audioProduce;
    this.videoProduce = produce && videoProduce;
    this.forceTcp = forceTcp;
    this.useDataChannel = useDataChannel;
  }

  /**
   * 连接信令
   *
   * @param {*} callback
   *
   * @returns
   */
  async connectSignal(callback) {
    const self = this;
    self.callback = callback;
    self.signalChannel = signalChannel;
    signalChannel.taoyao = self;
    // 不能直接this.push = this.signalChannel.push这样导致this对象错误
    self.push = function (data, pushCallback) {
      self.signalChannel.push(data, pushCallback);
    };
    self.request = async function (data) {
      return await self.signalChannel.request(data);
    };
    return self.signalChannel.connect(
      `wss://${self.host}:${self.port}/websocket.signal`,
      callback
    );
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
    }
    // 错误回调
    const errorMessage = protocol.buildMessage("platform::error", { message }, -9999);
    errorMessage.code = "-9999";
    errorMessage.message = message;
    self.callback(
      errorMessage,
      error
    );
  }
  async roomList() {
    const response = await this.request(
      protocol.buildMessage("room::list")
    );
    return response.body;
  }
  async mediaList() {
    const response = await this.request(
      protocol.buildMessage("client::list", { clientType: "MEDIA" })
    );
    return response.body;
  }
  /**
   * 创建房间
   */
  async createRoom(room) {
    const self = this;
    if (!room) {
      this.callbackError("无效房间");
      return;
    }
    const response = await self.request(
      protocol.buildMessage("room::create", room)
    );
    return response.body;
  }
  async enterRoom(roomId) {
    const self = this;
    if (!roomId) {
      this.callbackError("无效房间");
      return;
    }
    self.roomId = roomId;
    self.mediasoupDevice = new mediasoupClient.Device();
    const response = await self.request(
      protocol.buildMessage("media::router::rtp::capabilities", {
        roomId: self.roomId,
      })
    );
    const routerRtpCapabilities = response.body.rtpCapabilities;
    await self.mediasoupDevice.load({ routerRtpCapabilities });
    await self.request(
      protocol.buildMessage("room::enter", {
        roomId: roomId,
        rtpCapabilities: self.consume
          ? self.mediasoupDevice.rtpCapabilities
          : undefined,
        sctpCapabilities:
          self.consume && self.useDataChannel
            ? self.mediasoupDevice.sctpCapabilities
            : undefined,
      })
    );
  }
  /**
   * TODO：共享 navigator.mediaDevices.getDisplayMedia();
   * 生产媒体
   * TODO：验证API试试修改媒体
   * audioTrack.getSettings
   * audioTrack.getCapabilities
   * audioTrack.applyCapabilities
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
    // TODO：暂时不知道为什么？
    {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const audioTrack = stream.getAudioTracks()[0];
      audioTrack.enabled = false;
      setTimeout(() => audioTrack.stop(), 120000);
    }
    if (self.produce) {
      const response = await self.request(
        protocol.buildMessage("media::transport::webrtc::create", {
          roomId: self.roomId,
          forceTcp: self.forceTcp,
          producing: true,
          consuming: false,
          sctpCapabilities: self.useDataChannel
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
          sctpCapabilities: self.useDataChannel
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
        // TODO：设置配置
        const stream = await navigator.mediaDevices.getUserMedia({
          audio: true,
        });
        track = stream.getAudioTracks()[0];
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
  async produceVideo() {}

  /**
   * 消费媒体
   *
   * @param {*} message
   * @returns
   */
  async consumeMedia(message) {
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
      console.log(consumer)


      const audioElem = document.createElement('video');
      document.getElementsByTagName('body')[0].appendChild(audioElem)

      const stream = new MediaStream();
			stream.addTrack(consumer.track);
			audioElem.srcObject = stream;
			audioElem.play()
				.catch((error) => logger.warn('audioElem.play() failed:%o', error));


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
