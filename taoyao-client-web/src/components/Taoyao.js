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
        self.push(
          protocol.buildMessage("client::heartbeat", {
            signal: 100,
            battery: battery.level * 100,
            charging: battery.charging,
          })
        );
        self.heartbeat();
      } else {
        console.warn("发送心跳失败", self.channel);
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
      return;
    }
    self.address = address;
    self.callback = callback;
    self.reconnection = reconnection;
    return new Promise((resolve, reject) => {
      console.debug("连接信令通道", address);
      self.channel = new WebSocket(address);
      self.channel.onopen = async function (e) {
        console.debug("打开信令通道", e);
        // 注册终端
        const battery = await navigator.getBattery();
        self.push(
          protocol.buildMessage("client::register", {
            ip: "localhost",
            clientId: self.taoyao.clientId,
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
        resolve(e);
      };
      self.channel.onclose = function (e) {
        console.error("信令通道关闭", self.channel, e);
        if (self.reconnection) {
          self.reconnect();
        }
        reject(e);
      };
      self.channel.onerror = function (e) {
        console.error("信令通道异常", self.channel, e);
        if (self.reconnection) {
          self.reconnect();
        }
        reject(e);
      };
      /**
       * 回调策略：
       * 1. 如果注册请求回调，同时执行结果返回true不再执行后面所有回调。
       * 2. 如果注册全局回调，同时执行结果返回true不再执行后面所有回调。
       * 3. 如果前面所有回调没有返回true执行默认回调。
       */
      self.channel.onmessage = function (e) {
        console.debug("信令通道消息", e.data);
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
        // 全局回调
        if (!done && self.callback) {
          done = self.callback(message);
        }
        // 默认回调
        if (!done) {
          self.defaultCallback(message);
        }
      };
    });
  },
  /**
   * 重连
   */
  reconnect() {
    const self = this;
    if (self.lockReconnect) {
      return;
    }
    self.lockReconnect = true;
    // 关闭旧的通道
    if (self.channel && self.channel.readyState === WebSocket.OPEN) {
      self.channel.close();
      self.channel = null;
    }
    if (self.reconnectTimer) {
      clearTimeout(self.reconnectTimer);
    }
    // 打开定时重连
    self.reconnectTimer = setTimeout(function () {
      console.info("信令通道重连", self.address);
      self.connect(self.address, self.callback, true);
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
    self.channel.send(JSON.stringify(message));
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
      // 设置回调
      self.callbackMapping.set(message.header.id, (response) => {
        resolve(response);
        done = true;
        return true;
      });
      // 发送请求
      self.channel.send(JSON.stringify(message));
      // 设置超时
      setTimeout(() => {
        if (!done) {
          reject("请求超时", message);
        }
      }, 5000);
    });
  },
  /**
   * 关闭通道
   */
  close() {
    let self = this;
    self.reconnection = false;
    self.channel.close();
    clearTimeout(self.heartbeatTimer);
    clearTimeout(self.reconnectTimer);
  },
  /**
   * 默认回调
   *
   * @param {*} message 消息
   */
  defaultCallback(message) {
    let self = this;
    console.debug("没有适配信令消息执行默认处理", message);
    switch (message.header.signal) {
      case "client::config":
        self.defaultClientConfig(message);
        break;
      case "client::reboot":
        self.defaultClientReboot(message);
        break;
      case "client::register":
        console.info("终端注册成功");
        break;
      case "platform::error":
        console.error("信令异常", message);
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
    self.taoyao.mediaServerList = message.body.media.mediaServerList;
    console.debug(
      "终端配置",
      self.taoyao.audio,
      self.taoyao.video,
      self.taoyao.webrtc,
      self.taoyao.mediaServerList
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
  // 音频媒体配置
  audio;
  // 视频媒体配置
  video;
  // WebRTC配置
  webrtc;
  // 媒体服务配置
  mediaServerList;
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
   * 打开信令通道
   *
   * @param {*} callback
   *
   * @returns
   */
  async buildSignal(callback) {
    const self = this;
    signalChannel.taoyao = self;
    self.signalChannel = signalChannel;
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
   * 打开媒体通道
   * TODO：共享 navigator.mediaDevices.getDisplayMedia();
   */
  async buildMedia(roomId) {
    let self = this;
    // 释放资源
    self.closeMedia();
    if (roomId) {
      self.roomId = roomId;
    }
    self.mediasoupDevice = new mediasoupClient.Device();
    const response = await self.request(
      protocol.buildMessage("media::router::rtp::capabilities", {
        roomId: roomId,
      })
    );
    const routerRtpCapabilities = response.body;
    self.mediasoupDevice.load({ routerRtpCapabilities });
    self.produceMedia();
  }
  /**
   * 生产媒体
   * TODO：验证API试试修改媒体
   * audioTrack.getSettings
   * audioTrack.getCapabilities
   * audioTrack.applyCapabilities
   */
  async produceMedia() {
    const self = this;
    self.checkDevice();
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
            const { id } = await self.request(
              protocol.buildMessage("media::produce", {
                kind,
                appData,
                transportId: self.sendTransport.id,
                rtpParameters,
              })
            );
            callback({ id });
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
                transportId: this.recvTransport.id,
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
   */
  async produceAudio() {
    const self = this;
    if (this.produceAudio && this.mediasoupDevice.canProduce("audio")) {
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
        console.error("打开麦克风异常", error);
        if (track) {
          track.stop();
        }
      }
    } else {
      console.warn("音频打开失败");
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
   */
  async produceVideo() {}
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
        throw new Error("没有音频媒体设备");
      }
      if (!videoEnabled && self.videoProduce) {
        throw new Error("没有视频媒体设备");
      }
    } else {
      throw new Error("没有媒体设备");
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