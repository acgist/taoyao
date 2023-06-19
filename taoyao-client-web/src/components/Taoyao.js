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
    const me = this;
    if (++me.index > me.maxIndex) {
      me.index = 0;
    }
    const date = new Date();
    return (
      100000000000000 * date.getDate()    +
      1000000000000   * date.getHours()   +
      10000000000     * date.getMinutes() +
      100000000       * date.getSeconds() +
      1000            * me.clientIndex    +
      me.index
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
        v:      v  || "1.0.0",
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
      if (me.connected()) {
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
   * @returns 是否连接成功
   */
  connected() {
    const me = this;
    return me.channel && me.channel.readyState === WebSocket.OPEN;
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
    if (me.connected()) {
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
            name: me.taoyao.name,
            clientId: me.taoyao.clientId,
            clientType: "WEB",
            username: me.taoyao.username,
            password: me.taoyao.password,
            battery: battery.level * 100,
            charging: battery.charging,
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
        if(!me.connected()) {
          me.taoyao.closeRoomMedia();
          me.taoyao.closeSessionMedia();
        }
        if (me.reconnection) {
          me.reconnect();
        }
        // 不要失败回调
      };
      me.channel.onerror = async function (e) {
        console.error("信令通道异常：", me.channel, e);
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
      me.lockReconnect  ||
      me.taoyao.connect ||
      me.connected()
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
 * 会话
 */
class Session {

  // 会话ID
  id;
  // 远程终端名称
  name;
  // 音量
  volume = "100%";
  // 是否关闭
  closed;
  // 远程终端ID
  clientId;
  // 会话ID
  sessionId;
  // 本地媒体流
  localStream;
  // 是否打开音频
  audioEnabled;
  // 是否打开视频
  videoEnabled;
  // 本地音频
  localAudioTrack;
  localAudioEnabled;
  // 本地视频
  localVideoTrack;
  localVideoEnabled;
  // 远程音频
  remoteAudioTrack;
  remoteAudioEnabled;
  // 远程视频
  remoteVideoTrack;
  remoteVideoEnabled;
  // PeerConnection
  peerConnection;

  constructor({
    name,
    clientId,
    sessionId,
    audioEnabled,
    videoEnabled
  }) {
    this.id         = sessionId;
    this.name       = name;
    this.closed     = false;
    this.clientId   = clientId;
    this.sessionId  = sessionId;
    this.audioEnabled = audioEnabled;
    this.videoEnabled = videoEnabled;
  }

  async pause(type) {
    if(type === 'audio' && this.localAudioTrack) {
      this.localAudioEnabled = false;
      this.localAudioTrack.enabled = false;
    }
    if(type === 'video' && this.localVideoTrack) {
      this.localVideoEnabled = false;
      this.localVideoTrack.enabled = false;
    }
  }
  
  async resume(type) {
    if(type === 'audio' && this.localAudioTrack) {
      this.localAudioEnabled = true;
      this.localAudioTrack.enabled = true;
    }
    if(type === 'video' && this.localVideoTrack) {
      this.localVideoEnabled = true;
      this.localVideoTrack.enabled = true;
    }
  }
  
  async pauseRemote(type) {
    if(type === 'audio') {
      this.remoteAudioEnabled = false;
      this.remoteAudioTrack.enabled = false;
    }
    if(type === 'video') {
      this.remoteVideoEnabled = false;
      this.remoteVideoTrack.enabled = false;
    }
  }
  
  async resumeRemote(type) {
    if(type === 'audio') {
      this.remoteAudioEnabled = true;
      this.remoteAudioTrack.enabled = true;
    }
    if(type === 'video') {
      this.remoteVideoEnabled = true;
      this.remoteVideoTrack.enabled = true;
    }
  }
  
  async close() {
    if(this.closed) {
      return;
    }
    console.debug("会话关闭", this.sessionId);
    this.closed = true;
    this.localAudioEnabled = false;
    this.localVideoEnabled = false;
    this.remoteAudioEnabled = false;
    this.remoteVideoEnabled = false;
    if(this.localAudioTrack) {
      this.localAudioTrack.stop();
      this.localAudioTrack = null;
    }
    if(this.localVideoTrack) {
      this.localVideoTrack.stop();
      this.localVideoTrack = null;
    }
    if(this.remoteAudioTrack) {
      this.remoteAudioTrack.stop();
      this.remoteAudioTrack = null;
    }
    if(this.remoteVideoTrack) {
      this.remoteVideoTrack.stop();
      this.remoteVideoTrack = null;
    }
    if(this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }
  }

  async addIceCandidate(candidate) {
    if(this.closed) {
      return;
    }
    if(!candidate || candidate.sdpMid === undefined || candidate.sdpMLineIndex === undefined && candidate.candidate === undefined) {
      return;
    }
    if(this.peerConnection) {
      await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
    } else {
      setTimeout(() => this.addIceCandidate(candidate), 50);
    }
  }

}

/**
 * 远程终端
 */
class RemoteClient {

  // 终端名称
  name;
  // 终端标识
  clientId;
  // 音量
  volume = "100%";
  // 代理对象
  proxy;
  // 数据消费者
  dataConsumer;
  // 音频消费者
  audioConsumer;
  // 视频消费者
  videoConsumer;
  // 音频Track
  audioTrack;
  // 视频Track
  videoTrack;
  // 终端录制状态
  clientRecording;
  // 服务端录制状态
  serverRecording;

  constructor({
    name,
    clientId,
  }) {
    this.name     = name;
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

  close() {
    if(this.audioTrack) {
      this.audioTrack.stop();
      this.audioTrack = null;
    }
    if(this.videoTrack) {
      this.videoTrack.stop();
      this.videoTrack = null;
    }
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
  // 视频质量
  options;
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
  // 是否消费数据
  dataConsume;
  // 是否消费音频
  audioConsume;
  // 是否消费视频
  videoConsume;
  // 是否生产数据
  dataProduce;
  // 是否生产音频
  audioProduce;
  // 是否生产视频
  videoProduce;
  // 数据生产者
  dataProducer;
  // 音频生产者
  audioProducer;
  // 视频生产者
  videoProducer;
  // 消费者：音频、视频
  consumers = new Map();
  // 消费者：数据
  dataConsumers = new Map();
  // 远程终端
  remoteClients = new Map();
  // 会话终端
  sessionClients = new Map();
  // 本地录像机
  mediaRecorder;
  // 本地录像数据
  mediaRecorderChunks = [];

  constructor({
    name,
    clientId,
    host,
    port,
    username,
    password,
    roomId,
    // TODO：修改默认关闭
    dataConsume = true,
    audioConsume = true,
    videoConsume = true,
    // TODO：修改默认关闭
    dataProduce = true,
    audioProduce = true,
    videoProduce = true,
    forceTcp = false,
  }) {
    super({ name, clientId });
    this.name = name;
    this.clientId = clientId;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.roomId = roomId;
    this.dataConsume = dataConsume;
    this.audioConsume = audioConsume;
    this.videoConsume = videoConsume;
    this.dataProduce = dataProduce;
    this.audioProduce = audioProduce;
    this.videoProduce = videoProduce;
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
      case "media::data::consume":
        me.defaultMediaDataConsume(message);
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
      case "media::consumer::close":
        me.defaultMediaConsumerClose(message);
        break;
      case "media::consumer::pause":
        this.defaultMediaConsumerPause(message);
        break;
      case "media::consumer::request::key::frame":
        me.defaultMediaConsumerRequestKeyFrame(message);
        break;
      case "media::consumer::resume":
        this.defaultMediaConsumerResume(message);
        break;
      case "media::consumer::set::preferred::layers":
        me.defaultMediaConsumerSetPreferredLayers(message);
        break;
      case "media::consumer::status":
        this.defaultMediaConsumerStatus(message);
        break;
      case "media::data::consumer::close":
        me.defaultMediaDataConsumerClose(message);
        break;
      case "media::data::consumer::status":
        me.defaultMediaDataConsumerStatus(message);
        break;
      case "media::data::producer::close":
        me.defaultMediaDataProducerClose(message);
        break;
      case "media::data::producer::status":
        me.defaultMediaDataProducerStatus(message);
        break;
      case "media::producer::close":
        me.defaultMediaProducerClose(message);
        break;
      case "media::producer::pause":
        me.defaultMediaProducerPause(message);
        break;
      case "media::producer::resume":
        me.defaultMediaProducerResume(message);
        break;
      case "media::video::orientation::change":
        me.defaultMediaVideoOrientationChange(message);
        break;
      case "session::call":
        me.defaultSessionCall(message);
        break;
      case "session::close":
        me.defaultSessionClose(message);
        break;
      case "session::exchange":
        me.defaultSessionExchange(message);
        break;
      case "session::pause":
        me.defaultSessionPause(message);
        break;
      case "session::resume":
        me.defaultSessionResume(message);
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
      case "room::expel":
        me.defaultRoomExpel(message);
        break;
      case "room::invite":
        me.defaultRoomInvite(message);
        break;
      case "room::leave":
        me.defaultRoomLeave(message);
        break;
      case "platform::error":
        me.callbackError(message);
        break;
    }
  }
  /************************ 管理 ************************/
  getProducer(producerId) {
    const me = this;
    if(me.audioProducer?.id === producerId) {
      return me.audioProducer;
    } else if(me.videoProducer?.id === producerId) {
      return me.videoProducer;
    } else if(me.dataProducer?.id === producerId) {
      return me.dataProducer;
    } else {
      return null;
    }
  }
  async getStream() {
    let stream;
    const self = this;
    if (self.videoSource === "file") {
      // TODO：实现文件分享
      // const stream = await this._getExternalVideoStream();
      // track = stream.getVideoTracks()[0].clone();
    } else if (self.videoSource === "camera") {
      // TODO：参数
      stream = await navigator.mediaDevices.getUserMedia({
        audio: self.audioConfig,
        video: self.videoConfig,
      });
    } else if (self.videoSource === "screen") {
      stream = await navigator.mediaDevices.getDisplayMedia({
        audio: self.audioConfig,
        video: {
          cursor: true,
          width: { max: 1920 },
          height: { max: 1080 },
          frameRate: { max: 30 },
          logicalSurface: true,
          displaySurface: "monitor",
        },
      });
    } else {
      // TODO：异常
    }
    return stream;
  }
  async getAudioTrack() {
    const self = this;
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: self.audioConfig,
    });
    // TODO：首个？
    const track = stream.getAudioTracks()[0];
    // TODO：验证修改API audioTrack.applyCapabilities
    console.debug(
      "音频信息：",
      track.getSettings(),
      track.getCapabilities()
    );
    return track;
  }
  async getVideoTrack() {
    let track;
    const self = this;
    if (self.videoSource === "file") {
      // TODO：实现文件分享
      // const stream = await this._getExternalVideoStream();
      // track = stream.getVideoTracks()[0].clone();
    } else if (self.videoSource === "camera") {
      // TODO：参数
      const stream = await navigator.mediaDevices.getUserMedia({
        video: self.videoConfig,
      });
      // TODO：首个？
      track = stream.getVideoTracks()[0];
      // TODO：验证修改API videoTrack.applyCapabilities
      console.debug(
        "视频信息：",
        track.getSettings(),
        track.getCapabilities()
      );
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
    return track;
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
    me.audioConfig.sampleSize = {
      min: media.minSampleSize,
      ideal: audio.sampleSize,
      max: media.maxSampleSize,
    };
    // 强制修改
    // me.audioConfig.sampleRate = 8000;
    me.audioConfig.sampleRate = {
      min: media.minSampleRate,
      ideal: audio.sampleRate,
      max: media.maxSampleRate,
    };
    me.videoConfig.width = {
      min: media.minWidth,
      ideal: video.width,
      max: media.maxWidth,
    };
    me.videoConfig.height = {
      min: media.minHeight,
      ideal: video.height,
      max: media.maxHeight,
    };
    me.videoConfig.frameRate = {
      min: media.minFrameRate,
      ideal: video.frameRate,
      max: media.maxFrameRate,
    };
    me.options = Object.keys(media.videos).map(key => ({value: key, label: media.videos[key].resolution}));
    me.mediaConfig = media;
    me.webrtcConfig = webrtc;
    console.debug(
      "终端配置：",
      me.audioConfig,
      me.videoConfig,
      me.mediaConfig,
      me.webrtcConfig
    );
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
   * 拍照
   * 
   * @param {*} clientId 
   */
  controlPhotograph(clientId) {
    const me = this;
    me.push(
      protocol.buildMessage("control::photograph", {
        to: clientId
      })
    );
  }
  /**
   * 终端录像信令
   * 
   * @param {*} clientId 
   * @param {*} enabled
   */
  controlClientRecord(clientId, enabled) {
    const me = this;
    me.push(
      protocol.buildMessage("control::client::record", {
        to: clientId,
        enabled: enabled
      })
    );
  }
  /**
   * 服务端录像信令
   * 
   * @param {*} clientId 
   * @param {*} enabled
   */
   controlServerRecord(clientId, enabled) {
    const me = this;
    me.push(
      protocol.buildMessage("control::server::record", {
        to: clientId,
        roomId: me.roomId,
        enabled: enabled
      })
    );
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
    if (!volumes || !volumes.length) {
      me.volume = 0;
      me.remoteClients.forEach((v) => (v.volume = 0));
      return;
    }
    // 声音
    volumes.forEach((v) => {
      const { volume, clientId } = v;
      if (me.clientId === clientId) {
        me.setVolume(volume);
      } else {
        const remoteClient = me.remoteClients.get(clientId);
        if (remoteClient) {
          remoteClient.setVolume(volume);
        }
      }
    });
  }
  /**
   * 关闭消费者信令
   *
   * @param {*} consumerId 消费者ID
   */
  mediaConsumerClose(consumerId) {
    const me = this;
    me.push(
      protocol.buildMessage("media::consumer::close", {
        roomId: me.roomId,
        consumerId: consumerId,
      })
    );
  }
  /**
   * 关闭消费者信令
   *
   * @param {*} message 消息
   */
  defaultMediaConsumerClose(message) {
    const me = this;
    const { roomId, consumerId } = message.body;
    const consumer = me.consumers.get(consumerId);
    if (consumer) {
      console.debug("关闭消费者：", consumerId);
      consumer.close();
      me.consumers.delete(consumerId);
    } else {
      console.debug("关闭消费者无效：", consumerId);
    }
  }
  /**
   * 暂停消费者
   * 
   * @param {*} consumerId 消费者ID
   */
  mediaConsumerPause(consumerId) {
    const me = this;
    const consumer = me.consumers.get(consumerId);
    if(consumer) {
      if(consumer.paused) {
        return;
      }
      console.debug("mediaConsumerPause：", consumerId);
      me.push(protocol.buildMessage("media::consumer::pause", {
        roomId: me.roomId,
        consumerId: consumerId,
      }));
    } else {
      console.debug("mediaConsumerPause non：", consumerId);
    }
  }
  /**
   * 暂停消费者信令
   * 
   * @param {*} message 消息
   */
  defaultMediaConsumerPause(message) {
    const me = this;
    const { roomId, consumerId } = message.body;
    const consumer = me.consumers.get(consumerId);
    if (consumer) {
      console.debug("暂停消费者：", consumerId);
      consumer.pause();
    } else {
      console.debug("暂停消费者无效：", consumerId);
    }
  }
  /**
   * 请求关键帧
   * 
   * @param {*} consumerId 消费者ID
   */
  mediaConsumerRequestKeyFrame(consumerId) {
    const me = this;
    const consumer = me.consumers.get(consumerId);
    if(!consumer) {
      me.callbackError("消费者无效：" + consumerId);
      return;
    }
    if(consumer.kind !== "video") {
      me.callbackError("消费者不是视频媒体：" + consumerId);
      return;
    }
    me.push(protocol.buildMessage("media::consumer::request::key::frame", {
      roomId: me.roomId,
      consumerId: consumerId,
    }));
  }
  /**
   * 请求关键帧信令
   * 
   * @param {*} message 消息
   */
  defaultMediaConsumerRequestKeyFrame(message) {
    console.debug("defaultMediaConsumerRequestKeyFrame：", message);
  }
  /**
   * 恢复消费者
   * 
   * @param {*} consumerId 消费者ID
   */
   mediaConsumerResume(consumerId) {
    const me = this;
    const consumer = me.consumers.get(consumerId);
    if(consumer) {
      if(!consumer.paused) {
        return;
      }
      console.debug("mediaConsumerResume：", consumerId);
      me.push(protocol.buildMessage("media::consumer::resume", {
        roomId: me.roomId,
        consumerId: consumerId,
      }));
    } else {
      console.debug("mediaConsumerResume non：", consumerId);
    }
  }
  /**
  * 恢复消费者信令
  * 
  * @param {*} message 消息
  */
  defaultMediaConsumerResume(message) {
    const me = this;
    const { roomId, consumerId } = message.body;
    const consumer = me.consumers.get(consumerId);
    if (consumer) {
      console.debug("恢复消费者：", consumerId);
      consumer.resume();
    } else {
      console.debug("恢复消费者无效：", consumerId);
    }
  }
  /**
   * 修改最佳空间层和时间层
   * 
   * @param {*} consumerId 消费者ID
   * @param {*} spatialLayer 空间层
   * @param {*} temporalLayer 时间层
   */
  mediaConsumerSetPreferredLayers(consumerId, spatialLayer, temporalLayer) {
    const me = this;
    const consumer = me.consumers.get(consumerId);
    if(!consumer) {
      me.callbackError("消费者无效：" + consumerId);
      return;
    }
    if(consumer.kind !== "video") {
      me.callbackError("消费者不是视频媒体：" + consumerId);
      return;
    }
    me.push(protocol.buildMessage("media::consumer::set::preferred::layers", {
      roomId: me.roomId,
      consumerId,
      spatialLayer,
      temporalLayer,
    }));
  }
  /**
   * 修改最佳空间层和时间层信令
   * 
   * @param {*} message 消息
   */
  defaultMediaConsumerSetPreferredLayers(message) {
    console.debug("defaultMediaConsumerSetPreferredLayers：", message);
  }
  /**
  * 查询消费者状态信令
  * 
  * @param {*} message 消息
  */
  defaultMediaConsumerStatus(message) {
    console.info("defaultMediaConsumerStatus：", message);
  }
  /**
   * 关闭数据消费者信令
   * 
   * @param {*} message 消息
   */
  defaultMediaDataConsumerClose(message) {
    const me = this;
    const { roomId, consumerId } = message.body;
    const dataConsumer = me.dataConsumers.get(consumerId);
    if (dataConsumer) {
      console.info("关闭数据消费者：", consumerId);
      dataConsumer.close();
      me.dataConsumers.delete(consumerId);
    } else {
      console.debug("关闭数据消费者无效：", consumerId);
    }
  }
  /**
   * 查询数据消费者状态信令
   * 
   * @param {*} message 消息
   */
  defaultMediaDataConsumerStatus(message) {
    console.info("defaultMediaDataConsumerStatus：", message);
  }
  /**
   * 关闭数据生产者信令
   * 
   * @param {*} message 消息
   */
  defaultMediaDataProducerClose(message) {
    const me = this;
    const { roomId, producerId } = message.body;
    const producer = me.dataProducer;
    if (producer) {
      console.debug("关闭数据生产者：", producerId);
      producer.close();
      // TODO：类型判断设置为空
    } else {
      console.debug("关闭数据生产者无效：", producerId);
    }
  }
  /**
   * 关闭数据消费者信令
   * 
   * @param {*} message 消息
   */
  defaultMediaDataProducerStatus(message) {
    console.info("defaultMediaDataProducerStatus：", message);
  }
  /**
   * 关闭生产者信令
   * 
   * @param {*} message 消息
   */
  async defaultMediaProducerClose(message) {
    const me = this;
    const { roomId, producerId } = message.body;
    const producer = me.getProducer(producerId);
    if (producer) {
      console.debug("关闭生产者：", producerId);
      producer.close();
      // TODO：类型判断设置为空
    } else {
      console.debug("关闭生产者无效：", producerId);
    }
  }
  /**
   * 暂停生产者
   * 
   * @param {*} producerId 生产者ID
   */
  mediaProducerPause(producerId) {
    const me = this;
    const producer = me.getProducer(producerId);
    if(producer) {
      if(producer.paused) {
        return;
      }
      console.debug("mediaProducerPause：", producerId);
      me.push(protocol.buildMessage("media::producer::pause", {
        roomId: me.roomId,
        producerId: producerId,
      }));
    } else {
      console.debug("mediaProducerPause non：", producerId);
    }
  }
  /**
   * 暂停生产者信令
   * 
   * @param {*} message 消息
   */
  async defaultMediaProducerPause(message) {
    const me = this;
    const { roomId, producerId } = message.body;
    const producer = me.getProducer(producerId);
    if (producer) {
      console.debug("暂停生产者：", producerId);
      producer.pause();
    } else {
      console.debug("暂停生产者无效：", producerId);
    }
  }
  /**
   * 恢复生产者
   * 
   * @param {*} producerId 生产者ID
   */
   mediaProducerResume(producerId) {
    const me = this;
    const producer = me.getProducer(producerId);
    if(producer) {
      if(!producer.paused) {
        return;
      }
      console.debug("mediaProducerResume：", producerId);
      me.push(protocol.buildMessage("media::producer::resume", {
        roomId: me.roomId,
        producerId: producerId,
      }));
    } else {
      console.debug("mediaProducerResume non：", producerId);
    }
  }
  /**
   * 恢复生产者信令
   * 
   * @param {*} message 消息
   */
  async defaultMediaProducerResume(message) {
    const me = this;
    const { roomId, producerId } = message.body;
    const producer = me.getProducer(producerId);
    if (producer) {
      console.debug("恢复生产者：", producerId);
      producer.resume();
    } else {
      console.debug("恢复生产者无效：", producerId);
    }
  }
  /**
   * 重启ICE
   */
  async mediaIceRestart() {
    const me = this;
    if (me.sendTransport) {
      const response = await me.request(protocol.buildMessage(
        'media::ice::restart',
        { transportId: me.sendTransport.id }
      ));
      const { iceParameters } = response.body;
      await me.sendTransport.restartIce({ iceParameters });
    }
    if (me.recvTransport)
    {
      const response = await me.request(protocol.buildMessage(
        'media::ice::restart',
        { transportId: me.recvTransport.id }
      ));
      const { iceParameters } = response;
      await me.recvTransport.restartIce({ iceParameters });
    }

  }
  /**
   * 视频方向变化信令
   * 
   * @param {*} message 消息
   */
  defaultMediaVideoOrientationChange(message) {
    console.debug("视频方向变化信令：", message);
  }
  /**
   * 消费媒体信令
   *
   * @param {*} message 消息
   */
  async defaultMediaConsume(message) {
    const self = this;
    if (!self.audioConsume && !self.videoConsume) {
      console.debug("没有消费媒体");
      return;
    }
    const {
      kind,
      type,
      roomId,
      appData,
      clientId,
      sourceId,
      streamId,
      producerId,
      consumerId,
      rtpParameters,
      producerPaused,
    } = message.body;
    try {
      const consumer = await self.recvTransport.consume({
        id: consumerId,
        kind,
        producerId,
        rtpParameters,
        // 强制设置streamId，让libwebrtc同步麦克风和摄像头，屏幕共享不要求同步。
        streamId: `${clientId}-${appData?.videoSource ? appData.videoSource : "unknown"}`,
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
      self.push(message);
      console.debug("远程媒体：", consumer);
      const remoteClient = self.remoteClients.get(consumer.sourceId);
      if (remoteClient && remoteClient.proxy && remoteClient.proxy.media) {
        const track = consumer.track;
        // TODO：旧的媒体？
        if (track.kind === "audio") {
          remoteClient.audioTrack = track;
          remoteClient.audioConsumer = consumer;
        } else if (track.kind === "video") {
          remoteClient.videoTrack = track;
          remoteClient.videoConsumer = consumer;
        } else {
          console.warn("不支持的媒体：", track);
        }
        remoteClient.proxy.media(consumer.track, consumer);
      } else {
        console.warn("远程终端没有实现服务代理：", remoteClient);
      }
      // 实现进入自动暂停视频，注：必须订阅所有类型媒体，不然媒体服务直接不会转发视频媒体
      if (consumer.kind === "video" && !self.videoProduce) {
        // this.pauseConsumer(consumer);
        // TODO：实现
      }
    } catch (error) {
      self.callbackError("消费媒体异常", error);
    }
  }
  /**
   * 
   * @param {*} producerId 
   */
  mediaDataConsume(producerId) {
    const me = this;
    if(!me.recvTransport) {
      me.callbackError("没有连接接收通道");
      return;
    }
    me.push(
      protocol.buildMessage("media::data::consume", {
        roomId: me.roomId,
        producerId: producerId,
      })
    );
  }
  /**
   * 消费数据信令
   * 
   * @param {*} message 消息
   */
  async defaultMediaDataConsume(message) {
    const me = this;
    const {
      label,
      appData,
      protocol,
      consumerId,
      producerId,
      sctpStreamParameters,
    } = message.body;
    try {
      const dataConsumer = await me.recvTransport.consumeData({
        id : consumerId,
        dataProducerId : producerId,
        label,
        appData,
        protocol,
        sctpStreamParameters,
      });
      me.dataConsumers.set(dataConsumer.id, dataConsumer);
      dataConsumer.on('transportclose', () => {
        console.info("dataConsumer transportclose：", dataConsumer.id);
        dataConsumer.close();
      });
      dataConsumer.on('open', () => {
        console.info("dataConsumer open：", dataConsumer.id);
        window.dataConsumer = dataConsumer;
      });
      dataConsumer.on('close', () => {
        if(me.dataConsumers.delete(dataConsumer.id)) {
          console.info("dataConsumer close：", dataConsumer.id);
          me.push(
            taoyaoProtocol.buildMessage("media::data::consumer::close", {
              roomId: roomId,
              consumerId: dataConsumer.id,
            })
          );
        } else {
          console.info("dataConsumer close non：", dataConsumer.id);
        }
      });
      dataConsumer.on('error', (error) => {
        console.error("dataConsumer error：", dataConsumer.id, error);
        dataConsumer.close();
      });
      dataConsumer.on('message', (message) => {
        console.info("dataConsume message：", dataConsumer.id, message);
      });
    } catch (error) {
      console.error("打开数据消费者异常", error);
    }
  }
  /**
   * 平台异常信令
   *
   * @param {*} message 消息
   */
  defaultPlatformError(message) {
    const { code } = message;
    if (code === "3401") {
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
    message.body.clients.forEach((v) => {
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
    if (!me.roomId) {
      console.warn("房间无效：", me.roomId);
      return;
    }
    me.push(
      protocol.buildMessage("room::close", {
        roomId: me.roomId,
      })
    );
  }
  /**
   * 关闭房间信令
   *
   * @param {*} message 消息
   */
  defaultRoomClose(message) {
    const me = this;
    const { roomId } = message.body;
    if (me.roomId !== roomId) {
      return;
    }
    console.info("关闭房间：", roomId);
    me.closeRoomMedia();
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
    // TODO：已经进入房间忽略
    me.roomId = roomId;
    const response = await me.request(
      protocol.buildMessage("media::router::rtp::capabilities", {
        roomId: me.roomId,
      })
    );
    if(response.code !== '0000') {
      // TODO：提示
      me.roomId = null;
      return;
    }
    const routerRtpCapabilities = response.body.rtpCapabilities;
    me.mediasoupDevice = new mediasoupClient.Device();
//    mediasoupClient.parseScalabilityMode("L2T3");
//    // => { spatialLayers: 2, temporalLayers: 3 }
//    mediasoupClient.parseScalabilityMode("S3T3");
//    // => { spatialLayers: 3, temporalLayers: 3 }
//    mediasoupClient.parseScalabilityMode("L4T7_KEY_SHIFT");
//    // => { spatialLayers: 4, temporalLayers: 7 }
//    mediasoupClient.parseScalabilityMode(undefined);
//    // => { spatialLayers: 1, temporalLayers: 1 }
    await me.mediasoupDevice.load({ routerRtpCapabilities });
    await me.request(
      protocol.buildMessage("room::enter", {
        roomId: roomId,
        password: password,
        rtpCapabilities: me.audioConsume || me.videoConsume || me.audioProduce || me.videoProduce ? me.mediasoupDevice.rtpCapabilities : undefined,
        sctpCapabilities: me.dataConsume || me.dataProduce ? me.mediasoupDevice.sctpCapabilities : undefined,
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
      // TODO：已经存在忽略
      me.remoteClients.set(clientId, new RemoteClient(status));
    }
  }
  /**
   * 踢出终端
   * 
   * @param {*} clientId 终端ID
   */
   roomExpel(clientId) {
    const me = this;
    if(!me.roomId) {
      this.callbackError("没有进入房间");
      return;
    }
    me.push(protocol.buildMessage("room::expel", {
      roomId: this.roomId,
      clientId,
    }));
  }
  /**
   * 踢出终端信令
   * 
   * @param {*} message 消息
   */
  async defaultRoomExpel(message) {
    const me = this;
    me.roomLeave();
  }
  /**
   * 邀请终端
   * 
   * @param {*} clientId 终端ID
   */
  roomInvite(clientId) {
    const me = this;
    if(!me.roomId) {
      this.callbackError("没有进入房间");
      return;
    }
    me.push(protocol.buildMessage("room::invite", {
      roomId: this.roomId,
      clientId,
    }));
  }
  /**
   * 邀请终端信令
   * 
   * @param {*} message 消息
   */
  async defaultRoomInvite(message) {
    const me = this;
    // 默认进入，如果需要确认使用回调函数重写。
    const { roomId, password } = message.body;
    // TODO：已经进入房间拒绝
    // if(me.roomId) {
    //   this.callbackError();
    //   return;
    // }
    await me.roomEnter(roomId, password);
    await me.mediaProduce();
  }
  /**
   * 离开房间
   */
  roomLeave() {
    const me = this;
    me.push(protocol.buildMessage("room::leave", {
      roomId: me.roomId
    }));
    me.closeRoomMedia();
  }
  /**
   * 离开房间信令
   *
   * @param {*} message
   */
  defaultRoomLeave(message) {
    const me = this;
    const { clientId } = message.body;
    me.remoteClients.delete(clientId);
    // TODO：close
    console.info("终端离开：", clientId);
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
      protocol.buildMessage("client::list", {})
    );
    return response.body;
  }
  async clientStatus(clientId) {
    const response = await this.request(
      protocol.buildMessage("client::status", { clientId })
    );
    return response.body;
  }
  async roomClientList() {
    const me = this;
    const response = await this.request(
      protocol.buildMessage("room::client::list", { roomId: me.roomId })
    );
    return response.body;
  }
  /************************ 媒体 ************************/
  /**
   * 生产媒体
   */
  async mediaProduce() {
    const self = this;
    if (!self.roomId) {
      this.callbackError("无效房间");
      return;
    }
    // 检查设备
    self.checkDevice();
    /**
     * 解决浏览器的自动播放策略问题
     */
    // {
    //   const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    //   stream.getAudioTracks().forEach((audioTrack) => {
    //     audioTrack.enabled = false;
    //     setTimeout(() => audioTrack.stop(), 30000);
    //   });
    // }
    // TODO：代码提取
    if (self.dataProduce || self.audioProduce || self.videoProduce) {
      const response = await self.request(
        protocol.buildMessage("media::transport::webrtc::create", {
          roomId: self.roomId,
          forceTcp: self.forceTcp,
          producing: true,
          consuming: false,
          sctpCapabilities: self.dataProduce ? self.mediasoupDevice.sctpCapabilities : undefined,
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
        additionalSettings: {
          // TODO：加密解密
          encodedInsertableStreams: false
        },
      });
      self.sendTransport.on(
        "connect",
        ({ dtlsParameters }, callback, errback) => {
          self.request(
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
            const response = await self.request(
              protocol.buildMessage("media::produce", {
                kind,
                roomId: self.roomId,
                appData,
                transportId: self.sendTransport.id,
                rtpParameters,
              })
            );
            const { streamId, producerId } = response.body;
            callback({ id: producerId });
          } catch (error) {
            errback(error);
          }
        }
      );
      // 生产数据
      self.sendTransport.on(
        "producedata",
        async ({ label, appData, protocol, sctpStreamParameters }, callback, errback) => {
          try {
            const response = await self.request(
              taoyaoProtocol.buildMessage("media::data::produce", {
                label,
                roomId: self.roomId,
                appData,
                protocol,
                transportId: self.sendTransport.id,
                sctpStreamParameters,
              })
            );
            const { treamId, producerId } = response.body;
            callback({ id: producerId });
          } catch (error) {
            errback(error);
          }
        }
      );
    }
    if (self.dataConsume || self.audioConsume || self.videoConsume) {
      const self = this;
      const response = await self.request(
        protocol.buildMessage("media::transport::webrtc::create", {
          roomId: self.roomId,
          forceTcp: self.forceTcp,
          producing: false,
          consuming: true,
          sctpCapabilities: self.dataProduce ? self.mediasoupDevice.sctpCapabilities : undefined,
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
          role: "auto",
        },
        sctpParameters,
        iceServers: [],
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
          self.request(
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
    // 快速响应
    this.produceAudio();
    this.produceVideo();
    this.produceData();
    // 等待响应
    // await this.produceAudio();
    // await this.produceVideo();
    // await this.produceData();
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
        let track = await self.getAudioTrack();
        this.audioProducer = await this.sendTransport.produce({
          track,
          codecOptions: {
            opusStereo: 1,
            opusDtx: 1,
          },
					// {
					// 	opusStereo : true,
					// 	opusDtx    : true,
					// 	opusFec    : true,
					// 	opusNack   : true
					// }
          // NOTE: for testing codec selection.
          // codec : this._mediasoupDevice.rtpCapabilities.codecs
          // 	.find((codec) => codec.mimeType.toLowerCase() === 'audio/pcma')
        });
        if (self.proxy && self.proxy.media) {
          self.audioTrack = track;
          self.proxy.media(track, this.audioProducer);
        } else {
          console.warn("终端没有实现服务代理：", self);
        }
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
    try {
      await this.request(
        protocol.buildMessage("media::producer::close", {
          roomId: this.roomId,
          producerId: this.audioProducer.id,
        })
      );
    } catch (error) {
      console.error("关闭麦克风异常", error);
    }
  }

  async pauseAudioProducer() {
    console.debug("静音麦克风");
    this.mediaProducerPause(this.audioProducer.id);
  }

  async resumeAudioProducer() {
    console.debug("恢复麦克风");
    this.mediaProducerResume(this.audioProducer.id);
  }

  /**
   * 生产视频
   * TODO：重复点击
   */
  async produceVideo() {
    const self = this;
    if (self.videoProduce && self.mediasoupDevice.canProduce("video")) {
      if (self.videoProducer) {
        return;
      }
      try {
        console.debug("打开摄像头");
        let track = await self.getVideoTrack();
        let codec;
        let encodings;
        const codecOptions = {
          videoGoogleStartBitrate: 1000,
          videoGoogleMaxBitrate  : 1800,
          videoGoogleMinBitrate  : 800
        };
//        encodings   :
//        [
//          { maxBitrate: 100000 },
//          { maxBitrate: 300000 },
//          { maxBitrate: 900000 }
//        ],
        if (self.forceH264) {
          codec = self.mediasoupDevice.rtpCapabilities.codecs.find(
            (c) => c.mimeType.toLowerCase() === "video/h264"
          );
          if (!codec) {
            self.callbackError("不支持H264视频编码");
          }
        } else if (self.forceVP9) {
          codec = self.mediasoupDevice.rtpCapabilities.codecs.find(
            (c) => c.mimeType.toLowerCase() === "video/vp9"
          );
          if (!codec) {
            self.callbackError("不支持VP9视频编码");
          }
        }
        if (this.useSimulcast) {
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
        if (self.proxy && self.proxy.media) {
          self.videoTrack = track;
          self.proxy.media(track, this.videoProducer);
        } else {
          console.warn("终端没有实现服务代理：", self);
        }
        // if (this._e2eKey && e2e.isSupported()) {
        //   e2e.setupSenderTransform(this.videoProducer.rtpSender);
        // }
        this.videoProducer.on("transportclose", () => {
          this.videoProducer = null;
        });
        this.videoProducer.on("trackended", () => {
          console.warn("video producer trackended", this.videoProducer);
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

  /**
   * 生产数据
   */
  async produceData() {
    const me = this;
    try {
      const dataProducer = await me.sendTransport.produceData({
        ordered: false,
        maxPacketLifeTime: 2000,
      });
      me.dataProducer = dataProducer;
      me.dataProducer.on("open", () => {
        console.debug("dataProducer open：", me.dataProducer.id);
        window.dataProducer = me.dataProducer;
      });
      me.dataProducer.on("close", () => {
        console.debug("dataProducer close：", me.dataProducer.id);
        me.dataProducer = null;
      });
      me.dataProducer.on("error", (error) => {
        console.debug("dataProducer error：", me.dataProducer.id, error);
        me.dataProducer.close();
      });
      me.dataProducer.on("transportclose", () => {
        console.debug("dataProducer transportclose：", me.dataProducer.id);
        me.dataProducer.close();
      });
      me.dataProducer.on("bufferedamountlow", () => {
        console.debug("dataProducer bufferedamountlow：", me.dataProducer.id);
      });
    } catch (error) {
      me.callbackError("生产数据异常", error);
    }
  }

  /**
   * 通过数据生产者发送数据
   * 
   * @param {*} data 数据
   */
  async sendDataProducer(data) {
    const me = this;
    if(!me.dataProducer) {
      me.callbackError("数据生产者无效");
      return;
    }
    me.dataProducer.send(data);
  }

  async closeVideoProducer() {
    console.debug("disableWebcam()");
    if (!this.videoProducer) {
      return;
    }
    try {
      await this.request(
        protocol.buildMessage("media::producer::close", {
          roomId: this.roomId,
          producerId: this.videoProducer.id,
        })
        );
      } catch (error) {
        console.error(error);
      }
  }

  async pauseVideoProducer() {
    console.debug("关闭摄像头");
    this.mediaProducerPause(this.videoProducer.id);
  }

  async resumeVideoProducer() {
    console.debug("恢复摄像头");
    this.mediaProducerResume(this.videoProducer.id);
  }

  /**
   * 更新视频生产者
   */
  async updateVideoProducer() {
    const me = this;
    console.debug("更新摄像头参数");
    try {
      const track = await me.getVideoTrack();
      await this.videoProducer.replaceTrack({ track });
      me.proxy.media(track, this.videoProducer);
    } catch (error) {
      console.error("changeWebcam() | failed: %o", error);
    }
  }

  /**
   * 验证设备
   */
  async checkDevice() {
    const self = this;
    if (
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
        // 强制修改
        self.audioProduce = false;
      }
      if (!videoEnabled && self.videoProduce) {
        self.callbackError("没有视频媒体设备");
        // 强制修改
        self.videoProduce = false;
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
   * 发起会话
   * 
   * @param {*} clientId 接收者ID
   */
  async sessionCall(clientId) {
    const me = this;
    if (!clientId) {
      this.callbackError("无效终端");
      return;
    }
    await me.checkDevice();
    const response = await me.request(
      protocol.buildMessage("session::call", {
        clientId
      })
    );
    const { name, sessionId } = response.body;
    const session = new Session({name, clientId, sessionId, audioEnabled: me.audioProduce, videoEnabled: me.videoProduce});
    this.sessionClients.set(sessionId, session);
  }

  async defaultSessionCall(message) {
    const me = this;
    await me.checkDevice();
    const { name, clientId, sessionId } = message.body;
    const session = new Session({name, clientId, sessionId, audioEnabled: me.audioProduce, videoEnabled: me.videoProduce});
    this.sessionClients.set(sessionId, session);
    await me.buildPeerConnection(session, sessionId);
    session.peerConnection.createOffer().then(async description => {
      await session.peerConnection.setLocalDescription(description);
      me.push(
        protocol.buildMessage("session::exchange", {
          sdp      : description.sdp,
          type     : description.type,
          sessionId: sessionId
        })
      );
    });
  }

  async sessionClose(sessionId) {
    const me = this;
    me.push(protocol.buildMessage("session::close", {
      sessionId
    }));
  }

  async defaultSessionClose(message) {
    const me = this;
    const { sessionId } = message.body;
    const session = me.sessionClients.get(sessionId);
    if(session) {
      session.close();
      me.sessionClients.delete(sessionId);
    } else {
      console.debug("关闭会话无效", sessionId);
    }
  }

  async defaultSessionExchange(message) {
    const me = this;
    const { type, candidate, sessionId } = message.body;
    const session = this.sessionClients.get(sessionId);
    if (type === "offer") {
      await me.buildPeerConnection(session, sessionId);
      await session.peerConnection.setRemoteDescription(new RTCSessionDescription(message.body));
      session.peerConnection.createAnswer().then(async description => {
        await session.peerConnection.setLocalDescription(description);
        me.push(
          protocol.buildMessage("session::exchange", {
            sdp      : description.sdp,
            type     : description.type,
            sessionId: sessionId
          })
        );
      });
    } else if (type === "answer") {
      await session.peerConnection.setRemoteDescription(new RTCSessionDescription(message.body));
    } else if (type === "candidate") {
      await session.addIceCandidate(candidate);
    } else {
    }
  }
  async sessionPause(sessionId, type) {
    const me = this;
    const session = me.sessionClients.get(sessionId);
    if(!session) {
      return;
    }
    me.push(protocol.buildMessage("session::pause", {
      type,
      sessionId
    }));
    session.pauseRemote(type);
  }

  async defaultSessionPause(message) {
    const me = this;
    const { type, sessionId } = message.body;
    const session = me.sessionClients.get(sessionId);
    if(session) {
      session.pause(type);
    } else {
    }
  }

  async sessionResume(sessionId, type) {
    const me = this;
    const session = me.sessionClients.get(sessionId);
    if(!session) {
      return;
    }
    me.push(protocol.buildMessage("session::resume", {
      type,
      sessionId
    }));
    session.resumeRemote(type);
  }

  async defaultSessionResume(message) {
    const me = this;
    const { type, sessionId } = message.body;
    const session = me.sessionClients.get(sessionId);
    if(session) {
      session.resume(type);
    }
  }
  
  async buildPeerConnection(session, sessionId) {
    if(session.peerConnection)  {
      return session.peerConnection;
    }
    const me = this;
    const peerConnection = new RTCPeerConnection({"iceServers" : [{"url" : "stun:stun1.l.google.com:19302"}]});
    peerConnection.ontrack = event => {
      console.debug("buildPeerConnection ontrack", event);
      const track = event.track;
      if(track.kind === 'audio') {
        session.remoteAudioTrack = track;
        session.remoteAudioEnabled = true;
      } else if(track.kind === 'video') {
        session.remoteVideoTrack = track;
        session.remoteVideoEnabled = true;
      } else {
      }
      if(session.proxy && session.proxy.media) {
        session.proxy.media(track);
      }
    };
    peerConnection.onicecandidate = event => {
      console.debug("buildPeerConnection onicecandidate", event);
      me.push(
        protocol.buildMessage("session::exchange", {
          type      : "candidate",
          sessionId : sessionId,
          candidate : event.candidate
        })
      );
    };
    peerConnection.onnegotiationneeded = event => {
      console.debug("buildPeerConnection onnegotiationneeded", event);
      if(peerConnection.connectionState === "connected") {
        // TODO：重连
        peerConnection.restartIce();
      }
    }
    const localStream      = await me.getStream();
    session.localStream    = localStream;
    session.peerConnection = peerConnection;
    if(session.audioEnabled) {
      session.localAudioTrack   = localStream.getAudioTracks()[0];
      session.localAudioEnabled = true;
      await session.peerConnection.addTrack(session.localAudioTrack, localStream);
    } else {
      session.localAudioEnabled = false;
    }
    if(session.videoEnabled) {
      session.localVideoTrack   = localStream.getVideoTracks()[0];
      session.localVideoEnabled = true;
      await session.peerConnection.addTrack(session.localVideoTrack, localStream);
    } else {
      session.localVideoEnabled = false;
    }
    return peerConnection;
  }

  /**
   * 本地截图
   * 
   * @param {*} video 视频
   */
  localPhotograph(video) {
    const me = this;
    const canvas  = document.createElement('canvas');
    canvas.width  = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext('2d');
    context.drawImage(video, 0, 0, video.videoWidth, video.videoHeight);
    const dataURL = canvas.toDataURL('images/png');
    const download = document.createElement('a');
    download.href = dataURL;
    download.download = 'taoyao.png';
    download.style.display = 'none';
    document.body.appendChild(download);
    download.click();
    download.remove();
  }

  /**
   * 本地录像
   * 
   * 'video/webm;codecs=aac,vp8',
   * 'video/webm;codecs=aac,vp9',
   * 'video/webm;codecs=aac,h264',
   * 'video/webm;codecs=pcm,vp8',
   * 'video/webm;codecs=pcm,vp9',
   * 'video/webm;codecs=pcm,h264',
   * 'video/webm;codecs=opus,vp8',
   * 'video/webm;codecs=opus,vp9',
   * 'video/webm;codecs=opus,h264',
   * 'video/mp4;codecs=aac,vp8',
   * 'video/mp4;codecs=aac,vp9',
   * 'video/mp4;codecs=aac,h264',
   * 'video/mp4;codecs=pcm,vp8',
   * 'video/mp4;codecs=pcm,vp9',
   * 'video/mp4;codecs=pcm,h264',
   * 'video/mp4;codecs=opus,vp8',
   * 'video/mp4;codecs=opus,vp9',
   * 'video/mp4;codecs=opus,h264',
   * 
   * MediaRecorder.isTypeSupported(mimeType)
   * 
   * video.captureStream().getTracks().forEach((v) => stream.addTrack(v));
   * 
   * @param {*} audioStream 音频流
   * @param {*} videoStream 视频流
   * @param {*} enabled     是否录像
   */
  localClientRecord(audioStream, videoStream, enabled) {
    const me = this;
    if (enabled) {
      if (me.mediaRecorder) {
        return;
      }
      const stream = new MediaStream();
      if(audioStream) {
        audioStream.getAudioTracks().forEach(track => stream.addTrack(track));
      }
      if(videoStream) {
        videoStream.getVideoTracks().forEach(track => stream.addTrack(track));
      }
      me.mediaRecorder = new MediaRecorder(stream, {
        audioBitsPerSecond: 128  * 1000,
        videoBitsPerSecond: 2400 * 1000,
        mimeType: 'video/webm;codecs=opus,h264',
      });
      me.mediaRecorder.onstop = function (e) {
        const blob = new Blob(me.mediaRecorderChunks);
        const objectURL = URL.createObjectURL(blob);
        const download = document.createElement('a');
        download.href = objectURL;
        download.download = 'taoyao.mp4';
        download.style.display = 'none';
        document.body.appendChild(download);
        download.click();
        download.remove();
        URL.revokeObjectURL(objectURL);
        me.mediaRecorderChunks = [];
      };
      me.mediaRecorder.ondataavailable = (e) => {
        me.mediaRecorderChunks.push(e.data);
      };
      me.mediaRecorder.start();
    } else {
      if (!me.mediaRecorder) {
        return;
      }
      me.mediaRecorder.stop();
      me.mediaRecorder = null;
    }
  }

  /**
   * 关闭视频房间媒体
   */
  closeRoomMedia() {
    console.debug("关闭视频房间媒体");
    const me = this;
    me.roomId = null;
    me.close();
    if (me.sendTransport) {
      me.sendTransport.close();
      me.sendTransport = null;
    }
    if (me.recvTransport) {
      me.recvTransport.close();
      me.recvTransport = null;
    }
    if(me.dataProducer) {
      me.dataProducer.close();
      me.dataProducer = null;
    }
    if(me.audioProducer) {
      me.audioProducer.close();
      me.audioProducer = null;
    }
    if(me.videoProducer) {
      me.videoProducer.close();
      me.videoProducer = null;
    }
    me.consumers.forEach((consumer, consumerId) => {
      consumer.close();
    });
    me.consumers.clear();
    me.dataConsumers.forEach((dataConsumer, consumerId) => {
      dataConsumer.close();
    });
    me.dataConsumers.clear();
    me.remoteClients.forEach((client, clientId) => {
      client.close();
    });
    me.remoteClients.clear();
  }
  /**
   * 关闭视频会话媒体
   */
  closeSessionMedia() {
    console.debug("关闭视频会话媒体");
    const me = this;
    me.sessionClients.forEach((session, sessionId) => {
      if(session) {
        session.close();
      } else {
        console.debug("关闭会话无效", sessionId);
      }
    });
    me.sessionClients.clear();
  }
  /**
   * 关闭资源
   */
  closeAll() {
    const me = this;
    me.closeRoomMedia();
    me.closeSessionMedia();
    if (me.signalChannel) {
      me.signalChannel.close();
    }
  }
}

export { Taoyao };

