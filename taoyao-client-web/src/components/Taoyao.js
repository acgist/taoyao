import * as mediasoupClient from "mediasoup-client";
import {
  config,
  defaultAudioConfig,
  defaultVideoConfig,
  defaultShareScreenConfig,
  defaultSvcEncodings,
  defaultSimulcastEncodings,
  defaultRTCPeerConnectionConfig,
} from "./Config.js";

/**
 * 成功编码
 */
const SUCCESS_CODE    = "0000";
/**
 * 成功描述
 */
const SUCCESS_MESSAGE = "成功";

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
    const me = this;
    const message = {
      header: {
        v     : v  || config.signal.version,
        id    : id || me.buildId(),
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
    const me = this;
    if (me.heartbeatTimer) {
      clearTimeout(me.heartbeatTimer);
    }
    me.heartbeatTimer = setTimeout(async () => {
      if (me.connected()) {
        const battery = await navigator.getBattery();
        me.taoyao.push(protocol.buildMessage("client::heartbeat", {
          battery : battery.level * 100,
          charging: battery.charging,
        }));
        me.heartbeat();
      } else {
        console.warn("心跳失败", me.address);
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
   * 连接信令
   *
   * @param {*} address      信令地址
   * @param {*} reconnection 是否重连
   *
   * @returns Promise<WebSocket>
   */
  async connect(address, reconnection = true) {
    const me = this;
    if (me.connected()) {
      return new Promise((resolve, reject) => {
        resolve(me.channel);
      });
    }
    me.address      = address;
    me.reconnection = reconnection;
    return new Promise((resolve, reject) => {
      console.debug("连接信令通道", me.address);
      me.channel = new WebSocket(me.address);
      me.channel.onopen = async () => {
        console.info("打开信令通道", me.address);
        const battery = await navigator.getBattery();
        const { body } = await me.taoyao.request(protocol.buildMessage("client::register", {
          name      : me.taoyao.name,
          clientId  : me.taoyao.clientId,
          clientType: config.signal.clientType,
          username  : me.taoyao.username,
          password  : me.taoyao.password,
          battery   : battery.level * 100,
          charging  : battery.charging,
        }));
        protocol.clientIndex = body.index;
        console.info("终端注册成功", protocol.clientIndex);
        me.reconnectionTimeout = me.minReconnectionDelay;
        me.taoyao.connect      = true;
        me.heartbeat();
        resolve(me.channel);
      };
      me.channel.onclose = async () => {
        console.warn("信令通道关闭", me.channel);
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
      me.channel.onerror = async (e) => {
        console.error("信令通道异常", me.channel, e);
        // 不要失败回调
      };
      me.channel.onmessage = async (e) => {
        const content = e.data;
        try {
          console.debug("信令通道消息", content);
          me.taoyao.on(JSON.parse(content));
        } catch (error) {
          console.error("处理信令通道消息异常", e, error);
        }
      };
    });
  },
  /**
   * 重连信令
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
    me.reconnectTimer = setTimeout(() => {
      console.info("重连信令通道", me.address);
      me.connect(me.address, me.reconnection);
      me.lockReconnect = false;
    }, me.reconnectionTimeout);
    me.reconnectionTimeout = Math.min(
      me.reconnectionTimeout + me.minReconnectionDelay,
      me.maxReconnectionDelay
    );
  },
  /**
   * 关闭通道
   */
  close() {
    const me = this;
    console.info("关闭信令通道", me.address);
    clearTimeout(me.heartbeatTimer);
    clearTimeout(me.reconnectTimer);
    me.reconnection   = false;
    me.taoyao.connect = false;
    me.channel.close();
  },
};

/**
 * 视频会话
 */
class Session {
  // 会话ID
  id;
  // 是否关闭
  closed;
  // 代理对象
  proxy;
  // 音量
  volume;
  // 远程终端名称
  name;
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
  // 本地音频是否可用（暂停关闭）
  localAudioEnabled;
  // 本地视频
  localVideoTrack;
  // 本地视频是否可用（暂停关闭）
  localVideoEnabled;
  // 远程音频
  remoteAudioTrack;
  // 远程音频是否可用（暂停关闭）
  remoteAudioEnabled;
  // 远程视频
  remoteVideoTrack;
  // 远程视频是否可用（暂停关闭）
  remoteVideoEnabled;
  // WebRTC PeerConnection
  peerConnection;

  constructor({
    name,
    clientId,
    sessionId,
    audioEnabled,
    videoEnabled
  }) {
    this.id           = sessionId;
    this.closed       = false;
    this.volume       = "100%";
    this.name         = name;
    this.clientId     = clientId;
    this.sessionId    = sessionId;
    this.audioEnabled = audioEnabled;
    this.videoEnabled = videoEnabled;
  }

  /**
   * 暂停本地媒体
   * 
   * @param {*} type 媒体类型
   */
  async pause(type) {
    if(type === 'audio' && this.localAudioTrack) {
      this.localAudioEnabled       = false;
      this.localAudioTrack.enabled = false;
    }
    if(type === 'video' && this.localVideoTrack) {
      this.localVideoEnabled       = false;
      this.localVideoTrack.enabled = false;
    }
  }
  
  /**
   * 恢复本地媒体
   * 
   * @param {*} type 媒体类型
   */
  async resume(type) {
    if(type === 'audio' && this.localAudioTrack) {
      this.localAudioEnabled       = true;
      this.localAudioTrack.enabled = true;
    }
    if(type === 'video' && this.localVideoTrack) {
      this.localVideoEnabled       = true;
      this.localVideoTrack.enabled = true;
    }
  }
  
  /**
   * 暂停远程媒体
   * 
   * @param {*} type 媒体类型
   */
  async pauseRemote(type) {
    if(type === 'audio' && this.remoteAudioTrack) {
      this.remoteAudioEnabled       = false;
      this.remoteAudioTrack.enabled = false;
    }
    if(type === 'video' && this.remoteVideoTrack) {
      this.remoteVideoEnabled       = false;
      this.remoteVideoTrack.enabled = false;
    }
  }
  
  /**
   * 恢复远程媒体
   * 
   * @param {*} type 媒体类型
   */
  async resumeRemote(type) {
    if(type === 'audio' && this.remoteAudioTrack) {
      this.remoteAudioEnabled       = true;
      this.remoteAudioTrack.enabled = true;
    }
    if(type === 'video' && this.remoteVideoTrack) {
      this.remoteVideoEnabled       = true;
      this.remoteVideoTrack.enabled = true;
    }
  }
  
  /**
   * 关闭视频会话
   */
  async close() {
    if(this.closed) {
      return;
    }
    console.debug("会话关闭", this.sessionId);
    this.closed = true;
    this.localAudioEnabled  = false;
    this.localVideoEnabled  = false;
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

  /**
   * 添加媒体协商
   * 
   * @param {*} candidate 媒体协商
   * @param {*} index     重试次数
   */
  async addIceCandidate(candidate, index = 0) {
    if(this.closed) {
      return;
    }
    if(index >= 32) {
      console.debug("添加媒体协商次数超限", candidate, index);
      return;
    }
    if(
      !candidate ||
      candidate.sdpMid        === undefined ||
      candidate.candidate     === undefined ||
      candidate.sdpMLineIndex === undefined
    ) {
      console.debug("无效媒体协商", candidate);
      return;
    }
    if(this.peerConnection) {
      await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
    } else {
      console.debug("延迟添加媒体协商", candidate, index);
      setTimeout(() => this.addIceCandidate(candidate, ++index), 100);
    }
  }
};

/**
 * 远程终端
 */
class RemoteClient {
  // 终端ID
  id;
  // 是否关闭
  closed;
  // 代理对象
  proxy;
  // 音量
  volume;
  // 终端名称
  name;
  // 终端标识
  clientId;
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
    this.id       = clientId;
    this.closed   = false;
    this.name     = name;
    this.volume   = "100%";
    this.clientId = clientId;
  }

  /**
   * 设置音量
   * 
   * @param {*} volume 音量
   */
  setVolume(volume) {
    const me = this;
    me.volume = ((volume + 127) / 127 * 100) + "%";
  }

  /**
   * 关闭媒体
   */
  async close() {
    const me = this;
    if(me.closed) {
      return;
    }
    console.debug("关闭终端", me.clientId);
    me.closed = true;
    if(me.audioTrack) {
      await me.audioTrack.stop();
      me.audioTrack = null;
    }
    if(me.videoTrack) {
      await me.videoTrack.stop();
      me.videoTrack = null;
    }
    if(me.dataConsumer) {
      await me.dataConsumer.close();
      me.dataConsumer = null;
    }
    if(me.audioConsumer) {
      await me.audioConsumer.close();
      me.audioConsumer = null;
    }
    if(me.videoConsumer) {
      await me.videoConsumer.close();
      me.videoConsumer = null;
    }
  }
}

/**
 * 桃夭终端
 */
class Taoyao extends RemoteClient {
  // 信令连接
  connect;
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
  audioConfig     = defaultAudioConfig;
  // 视频媒体配置
  videoConfig     = defaultVideoConfig;
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
  // 文件共享
  fileVideo;
  // 文件共享地址
  fileVideoObjectURL;
  // 视频来源：file|camera|screen
  videoSource;
  // 强制使用TCP
  forceTcp;
  // 强制使用VP8
  forceVP8;
  // 强制使用VP9
  forceVP9;
  // 强制使用H264
  forceH264;
  // 多种质量媒体
  useLayers;
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
  consumers      = new Map();
  // 消费者：数据
  dataConsumers  = new Map();
  // 远程终端
  remoteClients  = new Map();
  // 会话终端
  sessionClients = new Map();
  // 本地录像机
  mediaRecorder;
  // 本地录像数据
  mediaRecorderChunks = [];

  // TODO：默认关闭data通道
  constructor({
    name,
    clientId,
    host,
    port,
    username,
    password,
    roomId       = null,
    dataConsume  = true,
    audioConsume = true,
    videoConsume = true,
    dataProduce  = true,
    audioProduce = true,
    videoProduce = true,
    videoSource  = "camera",
    forceTcp  = false,
    forceVP8  = false,
    forceVP9  = false,
    forceH264 = false,
    useLayers = false,
  }) {
    super({ name, clientId });
    this.connect  = false;
    this.name     = name;
    this.clientId = clientId;
    this.host     = host;
    this.port     = port;
    this.username = username;
    this.password = password;
    this.roomId   = roomId;
    this.dataConsume  = dataConsume;
    this.audioConsume = audioConsume;
    this.videoConsume = videoConsume;
    this.dataProduce  = dataProduce;
    this.audioProduce = audioProduce;
    this.videoProduce = videoProduce;
    this.videoSource  = videoSource;
    this.forceTcp  = forceTcp;
    this.forceVP8  = forceVP8;
    this.forceVP9  = forceVP9;
    this.forceH264 = forceH264;
    this.useLayers = useLayers;
  }

  /**
   * 连接信令
   *
   * @param {*} callback 回调事件
   *
   * @returns Promise<WebSocket>
   */
  async connectSignal(callback) {
    const me = this;
    me.callback          = callback;
    signalChannel.taoyao = me;
    return await signalChannel.connect(
      `wss://${me.host}:${me.port}/websocket.signal`
    );
  }
  /**
   * 异步请求
   *
   * @param {*} message  信令消息
   * @param {*} callback 信令回调
   */
  push(message, callback) {
    const me = this;
    const { header, body } = message;
    const { id }           = header;
    // 请求回调
    if (callback) {
      me.callbackMapping.set(id, callback);
    }
    // 发送消息
    try {
      signalChannel.channel.send(JSON.stringify(message));
    } catch (error) {
      console.error("异步请求异常", message, error);
    }
  }
  /**
   * 同步请求
   *
   * @param {*} message 信令消息
   *
   * @returns Promise
   */
  async request(message) {
    const me = this;
    return new Promise((resolve, reject) => {
      const { header, body } = message;
      const { id }           = header;
      // 设置超时
      const rejectTimeout = setTimeout(() => {
        me.callbackMapping.delete(id);
        reject("请求超时", message);
      }, 5000);
      // 请求回调
      me.callbackMapping.set(id, (response) => {
        resolve(response);
        clearTimeout(rejectTimeout);
        // 默认不用继续处理
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
   * 回调策略：
   * 
   * 1. 如果注册请求回调，同时执行结果返回true不再执行后面所有回调。
   * 2. 执行前置回调
   * 3. 如果注册全局回调，同时执行结果返回true不再执行后面所有回调。
   * 4. 执行后置回调
   *
   * @param {*} message 信令消息
   */
  async on(message) {
    const me = this;
    const { code, header, body } = message;
    const { id, signal }         = header;
    if(code !== "0000") {
      console.warn("信令错误", message);
    }
    // 请求回调
    if (me.callbackMapping.has(id)) {
      try {
        if(
          me.callbackMapping.get(id)(message)
        ) {
          return;
        }
      } finally {
        me.callbackMapping.delete(id);
      }
    }
    // 前置回调
    await me.preCallback(message);
    // 全局回调
    if (
      me.callback &&
      await me.callback(message)
    ) {
      return;
    }
    // 后置回调
    await me.postCallback(message);
  }

  /**
   * 前置回调
   * 
   * @param {*} message 消息
   */
  async preCallback(message) {
    const me = this;
    const { header, body } = message;
    const { signal }       = header;
    switch (signal) {
      case "client::config":
        me.defaultClientConfig(message);
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
   * @param {*} message 信令消息
   */
  async postCallback(message) {
    const me = this;
    const { header, body } = message;
    const { signal }       = header;
    switch (signal) {
      case "client::broadcast":
        me.defaultClientBroadcast(message);
        break;
      case "client::offline":
        me.defaultClientOffline(message);
        break;
        case "client::online":
        me.defaultClientOnline(message);
        break;
      case "client::reboot":
        me.defaultClientReboot(message);
        break;
      case "client::shutdown":
        me.defaultClientShutdown(message);
        break;
      case "client::unicast":
        me.defaultClientUnicast(message);
        break;
      case "control::bell":
        me.defaultControlBell(message);
        break;
      case "control::client::record":
        me.defaultControlClientReccord(message);
        break;
      case "control::config::audio":
        me.defaultControlConfigAudio(message);
        break;
      case "control::config::video":
        me.defaultControlConfigVideo(message);
        break;
      case "control::photograph":
        me.defaultControlPhotograph(message);
        break;
      case "control::wakeup":
        me.defaultControlWakeup(message);
        break;
      case "media::audio::volume":
        me.defaultMediaAudioVolume(message);
        break;
      case "media::consumer::close":
        me.defaultMediaConsumerClose(message);
        break;
      case "media::consumer::pause":
        me.defaultMediaConsumerPause(message);
        break;
      case "media::consumer::resume":
        me.defaultMediaConsumerResume(message);
        break;
      case "media::consumer::score":
        me.defaultMediaConsumerScore(message);
        break;
      case "media::data::consumer::close":
        me.defaultMediaDataConsumerClose(message);
        break;
      case "media::data::producer::close":
        me.defaultMediaDataProducerClose(message);
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
      case "media::producer::score":
        me.defaultMediaProducerScore(message);
        break;
      case "media::transport::close":
        me.defaultMediaTransportClose(message);
        break;
      case "media::video::orientation::change":
        me.defaultMediaVideoOrientationChange(message);
        break;
      case "platform::error":
        me.platformError(message);
        break;
      case "platform::reboot":
        me.defaultPlatformReboot(message);
        break;
      case "platform::shutdown":
        me.defaultPlatformShutdown(message);
        break;
      case "room::broadcast":
        me.defaultRoomBroadcast(message);
        break;
      case "room::client::list":
        me.defaultRoomClientList(message);
        break;
      case "room::close":
        me.defaultRoomClose(message);
        break;
      case "room::create":
        this.defaultRoomCreate(message);
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
    }
  }

  /**
   * @param {*} producerId 生产者ID
   * 
   * @returns 生产者
   */
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

  /**
   * 选择视频文件
   */
  async getFileVideo() {
    const input  = document.createElement("input");
    input.type   = "file";
    const select = new Promise((resolve, reject) => {
      input.onchange = (e) => {
        resolve(input.value);
      }
      input.oncancel = (e) => {
        resolve(null);
      };
    });
    input.click();
    const file = await select;
    input.remove();
    if(!file) {
      console.debug("没有选择共享文件");
      return;
    }
    console.debug("选择文件", file);
    this.fileVideo = document.createElement("video");
    this.fileVideo.loop     = true;
    this.fileVideo.muted    = true;
    this.fileVideo.controls = true;
    this.fileVideo.src      = URL.createObjectURL(input.files[0]);
    if(config.media.filePreview) {
      this.fileVideo.style         = "position:fixed;top:1rem;left:1rem;width:128px;border:2px solid #FFF;";
    } else {
      this.fileVideo.style.display = "none";
    }
    document.body.appendChild(this.fileVideo);
    // 开始播放不然不能采集
    await this.fileVideo.play();
  }

  /**
   * 释放视频文件
   */
  async closeFileVideo() {
    if(this.fileVideo) {
      this.fileVideo.remove();
    }
    if(this.fileVideoObjectURL) {
      URL.revokeObjectURL(this.fileVideoObjectURL);
    }
  }

  /**
   * @returns 媒体
   */
  async getStream() {
    const me = this;
    let stream;
    if (me.videoSource === "file") {
      await this.getFileVideo();
      stream = me.fileVideo.captureStream();
    } else if (me.videoSource === "camera") {
      console.debug("媒体配置", me.audioConfig, me.videoConfig);
      stream = await navigator.mediaDevices.getUserMedia({
        audio: me.audioConfig,
        video: me.videoConfig,
      });
    } else if (me.videoSource === "screen") {
      // 音频配置：视频可能没有音频
      const audioConfig = {
        ...me.audioConfig
      };
      // 删除min/max
      delete audioConfig.sampleSize.min;
      delete audioConfig.sampleSize.max;
      delete audioConfig.sampleRate.min;
      delete audioConfig.sampleRate.max;
      // 视频配置
      const videoConfig = {
        ...this.videoConfig,
        ...defaultShareScreenConfig
      };
      // 删除min/max
      delete videoConfig.width.min;
      delete videoConfig.width.max;
      delete videoConfig.height.min;
      delete videoConfig.height.max;
      delete videoConfig.frameRate.min;
      delete videoConfig.frameRate.max;
      console.debug("媒体配置", audioConfig, videoConfig);
      stream = await navigator.mediaDevices.getDisplayMedia({
        audio: audioConfig,
        video: videoConfig,
      });
    } else {
      console.warn("不支持的视频来源", me.videoSource);
    }
    stream.getAudioTracks().forEach(track => {
      console.debug(
        "音频轨道信息",
        track.getSettings(),
        track.getCapabilities()
      );
    });
    stream.getVideoTracks().forEach(track => {
      console.debug(
        "视频轨道信息",
        track.getSettings(),
        track.getCapabilities()
      );
    });
    return stream;
  }

  /**
   * @returns 音频轨道
   */
  async getAudioTrack() {
    const me = this;
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: me.audioConfig,
      video: false,
    });
    const track = stream.getAudioTracks()[0];
    console.debug(
      "音频轨道信息",
      track.getSettings(),
      track.getCapabilities()
    );
    return track;
  }

  /**
   * @returns 视频轨道
   */
  async getVideoTrack() {
    let stream;
    const me = this;
    if (me.videoSource === "file") {
      await this.getFileVideo();
      stream = me.fileVideo.captureStream();
    } else if (me.videoSource === "camera") {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: me.videoConfig,
      });
    } else if (me.videoSource === "screen") {
      // 视频配置
      const videoConfig = {
        ...this.videoConfig,
        ...defaultShareScreenConfig
      };
      // 删除min/max
      delete videoConfig.width.min;
      delete videoConfig.width.max;
      delete videoConfig.height.min;
      delete videoConfig.height.max;
      delete videoConfig.frameRate.min;
      delete videoConfig.frameRate.max;
      stream = await navigator.mediaDevices.getDisplayMedia({
        audio: false,
        video: videoConfig,
      });
    } else {
      console.warn("不支持的视频来源", me.videoSource);
    }
    const track = stream.getVideoTracks()[0];
    console.debug(
      "视频轨道信息",
      track.getSettings(),
      track.getCapabilities()
    );
    return track;
  }

  /**
   * 终端告警信令
   * 
   * @param {*} message 
   */
  clientAlarm(message) {
    const me       = this;
    const date     = new Date();
    const datetime = ""                                             +
      date.getFullYear()                                            +
      ((date.getMonth()   < 9  ? "0" : "") + (date.getMonth() + 1)) +
      ((date.getDate()    < 10 ? "0" : "") + date.getDate())        +
      ((date.getHours()   < 10 ? "0" : "") + date.getHours())       +
      ((date.getMinutes() < 10 ? "0" : "") + date.getMinutes())     +
      ((date.getSeconds() < 10 ? "0" : "") + date.getSeconds());
    me.push(protocol.buildMessage("client::alarm", {
      message,
      datetime,
    }));
  }

  /**
   * 终端广播信令
   * 
   * @param {*} message    广播信息
   * @param {*} clientType 终端类型（可选）
   */
  clientBroadcast(message, clientType) {
    const me = this;
    me.push(protocol.buildMessage("client::broadcast", {
      ...message,
      clientType,
    }));
  }

  /**
   * 终端广播信令
   * 
   * @param {*} message 信令消息
   */
  defaultClientBroadcast(message) {
    const me = this;
    const {
      header,
      body,
    } = message;
    console.debug("终端广播", header, body);
  }

  /**
   * 关闭终端信令
   */
  async clientClose() {
    const me = this;
    await me.request(protocol.buildMessage("client::close", {}));
    me.closeAll();
  }

  /**
   * 终端配置信令
   *
   * @param {*} message 信令消息
   */
  defaultClientConfig(message) {
    const me = this;
    const { media, webrtc } = message.body;
    const { audio, video }  = media;
    me.audioConfig.sampleSize = {
      min  : media.minSampleSize,
      ideal: audio.sampleSize,
      max  : media.maxSampleSize,
    };
    me.audioConfig.sampleRate = {
      min  : media.minSampleRate,
      ideal: audio.sampleRate,
      max  : media.maxSampleRate,
    };
    me.videoConfig.width = {
      min  : media.minWidth,
      ideal: video.width,
      max  : media.maxWidth,
    };
    me.videoConfig.height = {
      min  : media.minHeight,
      ideal: video.height,
      max  : media.maxHeight,
    };
    me.videoConfig.frameRate = {
      min  : media.minFrameRate,
      ideal: video.frameRate,
      max  : media.maxFrameRate,
    };
    me.options      = Object.keys(media.videos).map(key => ({
      value: key,
      label: media.videos[key].resolution
    }));
    me.mediaConfig  = media;
    me.webrtcConfig = webrtc;
    console.debug(
      "终端媒体配置",
      me.options,
      me.audioConfig,
      me.videoConfig,
      me.mediaConfig,
      me.webrtcConfig
    );
  }

  /**
   * @returns 媒体服务列表
   */
  async mediaList() {
    const response = await this.request(protocol.buildMessage("client::list", {
      clientType: "MEDIA" 
    }));
    return response.body;
  }

  /**
   * @returns 媒体终端列表
   */
  async mediaClientList() {
    const response = await this.request(protocol.buildMessage("client::list", {}));
    return response.body.filter(v => {
      return v.clientType === "WEB" || v.clientType === "CAMERA" || v.clientType === "MOBILE";
    });
  }

  /**
   * @param {*} clientType 终端类型（可选）
   * 
   * @returns 终端列表
   */
  async clientList(clientType) {
    const response = await this.request(protocol.buildMessage("client::list", {
      clientType
    }));
    return response.body;
  }

  /**
   * 终端下线信令
   * 
   * @param {*} message 信令消息
   */
  defaultClientOffline(message) {
    console.debug("终端下线", message);
  }

  /**
   * 终端上线信令
   * 
   * @param {*} message 信令消息
   */
  defaultClientOnline(message) {
    console.debug("终端上线", message);
  }

  /**
   * 重启终端信令
   *
   * @param {*} message 信令消息
   */
  defaultClientReboot(message) {
    console.info("重启终端", message);
    location.reload();
  }

  /**
   * 关闭终端信令
   *
   * @param {*} message 消息
   */
  defaultClientShutdown(message) {
    console.info("关闭终端", message);
    window.close();
  }

  /**
   * 终端状态信令
   * 
   * @param {*} clientId 终端ID
   * 
   * @returns 终端状态
   */
  async clientStatus(clientId) {
    const response = await this.request(protocol.buildMessage("client::status", {
      clientId
    }));
    return response.body;
  }

  /**
   * 终端单播信令
   * 
   * @param {*} clientId 接收终端ID
   * @param {*} message  消息内容
   */
  clientUnicast(clientId, message) {
    this.push(protocol.buildMessage("client::unicast", {
      ...message,
      to: clientId
    }));
  }

  /**
   * 终端单播信令
   * 
   * @param {*} message 信令消息
   */
  defaultClientUnicast(message) {
    console.debug("终端单播消息", message);
  }

  /**
   * 响铃信令
   * 
   * @param {*} clientId 目标终端ID
   * @param {*} enabled  是否响铃
   */
  controlBell(clientId, enabled) {
    this.request(protocol.buildMessage("control::bell", {
      enabled,
      to: clientId,
    }));
  }

  /**
   * 响铃信令
   * 
   * @param {*} message 信令消息
   */
  defaultControlBell(message) {
    console.debug("响铃", message);
    this.push(message);
  }

  /**
   * 终端录像信令
   * 
   * @param {*} clientId 终端ID
   * @param {*} enabled  录制状态
   */
  controlClientRecord(clientId, enabled) {
    const me = this;
    me.request(protocol.buildMessage("control::client::record", {
      to     : clientId,
      enabled: enabled
    }));
  }

  /**
   * 终端录像信令
   * 
   * @param {*} message 信令消息
   */
  defaultControlClientReccord(message) {
    console.debug("终端录像", message);
    this.push(message);
  }

  /**
   * 配置音频信令
   * 
   * @param {*} clientId 终端ID
   * @param {*} config   音频配置
   */
  controlConfigAudio(clientId, config) {
    this.request(protocol.buildMessage("control::config::audio", {
      ...config,
      to: clientId
    }));
  }

  /**
   * 配置音频信令
   * 
   * @param {*} message 信令消息
   */
  defaultControlConfigAudio(message) {
    console.debug("配置音频", message);
    this.push(message);
    // TODO：配置本地音频
  }

  /**
   * 配置视频信令
   * 
   * @param {*} clientId 终端ID
   * @param {*} config   视频配置
   */
  controlConfigVideo(clientId, config) {
    this.request(protocol.buildMessage("control::config::video", {
      ...config,
      to: clientId
    }));
  }

  /**
   * 配置视频信令
   * 
   * @param {*} message 信令消息
   */
  defaultControlConfigVideo(message) {
    console.debug("配置视频", message);
    this.push(message);
    // TODO：配置本地视频
  }

  /**
   * 拍照信令
   * 
   * @param {*} clientId 终端ID
   */
  controlPhotograph(clientId) {
    const me = this;
    me.request(protocol.buildMessage("control::photograph", {
      to: clientId
    }));
  }
  
  /**
   * 拍照信令
   * 
   * @param {*} message 信令消息
   */
  defaultControlPhotograph(message) {
    console.debug("拍照", message);
    this.push(message);
  }

  /**
   * 服务端录像信令
   * 
   * @param {*} clientId 终端ID
   * @param {*} enabled  录制状态
   */
   controlServerRecord(clientId, enabled) {
    const me = this;
    me.request(protocol.buildMessage("control::server::record", {
      to     : clientId,
      roomId : me.roomId,
      enabled: enabled
    }));
  }

  /**
   * 终端唤醒信令
   * 
   * @param {*} clientId 目标终端ID
   */
  controlWakeup(clientId) {
    this.request(protocol.buildMessage("control::wakeup", {
      to: clientId
    }));
  }

  /**
   * 终端唤醒信令
   * 
   * @param {*} message 信令消息
   */
  defaultControlWakeup(message) {
    console.debug("终端唤醒", message);
    this.push(message);
  }

  /**
   * 终端音量信令
   *
   * @param {*} message 信令消息
   */
  defaultMediaAudioVolume(message) {
    const me = this;
    const {
      roomId,
      volumes
    } = message.body;
    if (volumes && volumes.length > 0) {
      // 声音
      volumes.forEach(v => {
        const {
          volume,
          clientId
        } = v;
        if (me.clientId === clientId) {
          me.setVolume(volume);
        } else {
          const remoteClient = me.remoteClients.get(clientId);
          remoteClient?.setVolume(volume);
        }
      });
    } else {
      // 静音
      me.volume = 0;
      me.remoteClients.forEach(v => {
        v.volume = 0;
      });
    }
  }

  /**
   * 关闭消费者信令
   *
   * @param {*} consumerId 消费者ID
   */
  mediaConsumerClose(consumerId) {
    const me = this;
    me.push(protocol.buildMessage("media::consumer::close", {
      roomId    : me.roomId,
      consumerId: consumerId,
    }));
  }
  
  /**
   * 关闭消费者信令
   *
   * @param {*} message 信令消息
   */
  defaultMediaConsumerClose(message) {
    const me = this;
    const {
      roomId,
      consumerId
    } = message.body;
    const consumer = me.consumers.get(consumerId);
    if (consumer) {
      console.debug("关闭消费者", consumerId);
      consumer.close();
      me.consumers.delete(consumerId);
    } else {
      console.debug("关闭消费者无效", consumerId);
    }
  }

  /**
   * 暂停消费者信令
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
      console.debug("暂停消费者", consumerId);
      me.push(protocol.buildMessage("media::consumer::pause", {
        roomId    : me.roomId,
        consumerId: consumerId,
      }));
    } else {
      console.debug("暂停消费者无效", consumerId);
    }
  }

  /**
   * 暂停消费者信令
   * 
   * @param {*} message 消息
   */
  defaultMediaConsumerPause(message) {
    const me = this;
    const {
      roomId,
      consumerId
    } = message.body;
    const consumer = me.consumers.get(consumerId);
    if (consumer) {
      console.debug("暂停消费者", consumerId);
      consumer.pause();
    } else {
      console.debug("暂停消费者无效", consumerId);
    }
  }

  /**
   * 请求关键帧信令
   * 
   * @param {*} consumerId 消费者ID
   */
  mediaConsumerRequestKeyFrame(consumerId) {
    const me = this;
    const consumer = me.consumers.get(consumerId);
    if(!consumer) {
      me.platformError("请求关键帧消费者无效");
      return;
    }
    if(consumer.kind !== "video") {
      me.platformError("只能请求视频消费者关键帧");
      return;
    }
    me.push(protocol.buildMessage("media::consumer::request::key::frame", {
      roomId    : me.roomId,
      consumerId: consumerId,
    }));
  }

  /**
   * 恢复消费者信令
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
      console.debug("恢复消费者", consumerId);
      me.push(protocol.buildMessage("media::consumer::resume", {
        roomId    : me.roomId,
        consumerId: consumerId,
      }));
    } else {
      console.debug("恢复消费者无效", consumerId);
    }
  }

  /**
  * 恢复消费者信令
  * 
  * @param {*} message 信令消息
  */
  defaultMediaConsumerResume(message) {
    const me = this;
    const {
      roomId,
      consumerId
    } = message.body;
    const consumer = me.consumers.get(consumerId);
    if (consumer) {
      console.debug("恢复消费者", consumerId);
      consumer.resume();
    } else {
      console.debug("恢复消费者无效", consumerId);
    }
  }

  /**
   * 媒体消费者评分信令
   * 
   * @param {*} message 信令消息
   */
  defaultMediaConsumerScore(message) {
    console.debug("消费者评分", message);
  }

  /**
   * 修改最佳空间层和时间层信令
   * 
   * @param {*} consumerId    消费者ID
   * @param {*} spatialLayer  空间层
   * @param {*} temporalLayer 时间层
   */
  mediaConsumerSetPreferredLayers(consumerId, spatialLayer, temporalLayer) {
    const me = this;
    const consumer = me.consumers.get(consumerId);
    if(!consumer) {
      me.platformError("修改最佳空间层和时间层消费者无效");
      return;
    }
    if(consumer.kind !== "video") {
      me.platformError("只能修改视频消费者最佳空间层和时间层");
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
   * 设置消费者优先级信令
   * 
   * @param {*} consumerId 消费者ID
   * @param {*} priority   优先级：1~255
   */
  mediaConsumerSetPriority(consumerId, priority) {
    const me = this;
    const consumer = me.consumers.get(consumerId);
    if(!consumer) {
      me.platformError("设置消费者优先级消费者无效");
      return;
    }
    me.push(protocol.buildMessage("media::consumer::set::priority", {
      roomId: me.roomId,
      consumerId,
      priority,
    }));
  }

  /**
   * 查询消费者状态信令
   * 
   * @param {*} consumerId 消费者ID
   */
  async mediaConsumerStatus(consumerId) {
    const me = this;
    return await me.request(protocol.buildMessage('media::consumer::status', {
      roomId: me.roomId,
      consumerId
    }));
  }

  /**
   * 关闭数据消费者信令
   *
   * @param {*} consumerId 数据消费者ID
   */
  mediaDataConsumerClose(consumerId) {
    const me = this;
    me.push(protocol.buildMessage("media::data::consumer::close", {
      roomId    : me.roomId,
      consumerId: consumerId,
    }));
  }

  /**
   * 关闭数据消费者信令
   * 
   * @param {*} message 信令消息
   */
  defaultMediaDataConsumerClose(message) {
    const me = this;
    const { roomId, consumerId } = message.body;
    const dataConsumer = me.dataConsumers.get(consumerId);
    if (dataConsumer) {
      console.debug("关闭数据消费者", consumerId);
      dataConsumer.close();
      me.dataConsumers.delete(consumerId);
    } else {
      console.debug("关闭数据消费者无效", consumerId);
    }
  }

  /**
   * 消费媒体信令
   * 
   * @param {*} producerId 生产者ID
   */
  mediaConsume(producerId) {
    const me = this;
    if(!me.recvTransport) {
      me.platformError("没有连接接收通道");
      return;
    }
    me.push(protocol.buildMessage("media::consume", {
      roomId    : me.roomId,
      producerId: producerId,
    }));
  }

  /**
   * 消费媒体信令
   * 
   * 如果需要加密：consumer.rtpReceiver
   * const receiverStreams = receiver.createEncodedStreams();
   * const readableStream  = receiverStreams.readable || receiverStreams.readableStream;
   * const writableStream  = receiverStreams.writable || receiverStreams.writableStream;
   *
   * @param {*} message 信令消息
   */
  async defaultMediaConsume(message) {
    const me = this;
    if (!me.audioConsume && !me.videoConsume) {
      console.debug("没有消费媒体");
      return;
    }
    const {
      roomId,
      clientId,
      sourceId,
      streamId,
      producerId,
      consumerId,
      kind,
      type,
      appData,
      rtpParameters,
      producerPaused,
    } = message.body;
    try {
      const consumer = await me.recvTransport.consume({
        id: consumerId,
        appData: { ...appData, clientId, sourceId, streamId },
        // 让libwebrtc同步相同来源媒体
        streamId: `${clientId}-${appData.videoSource || "taoyao"}`,
        kind,
        producerId,
        rtpParameters,
      });
      consumer.clientId = clientId;
      consumer.sourceId = sourceId;
      consumer.streamId = streamId;
      me.consumers.set(consumer.id, consumer);
      consumer.on("transportclose", () => {
        console.debug("消费者关闭（通道关闭）", consumer.id, streamId);
        consumer.close();
      });
      consumer.observer.on("close", () => {
        if(me.consumers.delete(consumer.id)) {
          console.debug("消费者关闭", consumer.id, streamId);
        } else {
          console.debug("消费者关闭（无效）", consumer.id, streamId);
        }
      });
      const {
        spatialLayers,
        temporalLayers
      } = mediasoupClient.parseScalabilityMode(
        consumer.rtpParameters.encodings[0].scalabilityMode
      );
      console.debug("时间层空间层", spatialLayers, temporalLayers);
      me.push(message);
      console.debug("远程媒体消费者", consumer);
      const track        = consumer.track;
      const remoteClient = me.remoteClients.get(consumer.sourceId);
      me.callbackTrack(sourceId, track);
      if (
        remoteClient       &&
        remoteClient.proxy &&
        remoteClient.proxy.media
      ) {
        if (track.kind === "audio") {
          remoteClient.audioTrack    = track;
          remoteClient.audioConsumer = consumer;
        } else if (track.kind === "video") {
          remoteClient.videoTrack    = track;
          remoteClient.videoConsumer = consumer;
        } else {
          console.warn("不支持的媒体类型", track);
        }
        remoteClient.proxy.media(track, consumer);
      } else {
        console.warn("远程终端没有实现代理", remoteClient);
      }
    } catch (error) {
      me.platformError("消费媒体异常", error);
    }
  }

  /**
   * 消费数据信令
   * 
   * @param {*} producerId 数据生产者ID
   */
  mediaDataConsume(producerId) {
    const me = this;
    if(!me.recvTransport) {
      me.platformError("没有连接接收通道");
      return;
    }
    me.push(protocol.buildMessage("media::data::consume", {
      roomId    : me.roomId,
      producerId: producerId,
    }));
  }

  /**
   * 消费数据信令
   * 
   * @param {*} message 消息
   */
  async defaultMediaDataConsume(message) {
    const me = this;
    const {
      roomId,
      clientId,
      sourceId,
      streamId,
      producerId,
      consumerId,
      label,
      appData,
      protocol,
      sctpStreamParameters,
    } = message.body;
    try {
      const dataConsumer = await me.recvTransport.consumeData({
        id            : consumerId,
        dataProducerId: producerId,
        label,
        appData,
        protocol,
        sctpStreamParameters,
      });
      me.dataConsumers.set(dataConsumer.id, dataConsumer);
      dataConsumer.on("open", () => {
        console.debug("数据消费者打开", dataConsumer.id);
      });
      dataConsumer.on("transportclose", () => {
        console.debug("数据消费者关闭（通道关闭）", dataConsumer.id, streamId);
        dataConsumer.close();
      });
      // dataConsumer.observer.on("close", fn())
      dataConsumer.on("close", () => {
        if(me.dataConsumers.delete(dataConsumer.id)) {
          console.debug("数据消费者关闭", dataConsumer.id, streamId);
        } else {
          console.debug("数据消费者关闭（无效）", dataConsumer.id, streamId);
        }
      });
      dataConsumer.on("error", (error) => {
        console.error("数据消费者异常", dataConsumer.id, streamId, error);
      });
      dataConsumer.on("message", (message, ppid) => {
        console.debug("数据消费者消息", dataConsumer.id, streamId, message.toString("UTF-8"), ppid);
      });
    } catch (error) {
      console.error("打开数据消费者异常", error);
    }
  }

  /**
   * 查询数据消费者状态信令
   * 
   * @param {*} consumerId 消费者ID
   */
  async mediaDataConsumerStatus(consumerId) {
    return await this.request(protocol.buildMessage("media::data::consumer::status", {
      consumerId,
      roomId: this.roomId,
    }));
  }

  /**
   * 生产数据
   */
  async produceData() {
    if(this.dataProducer) {
      console.debug("已经存在数据生产者");
      return;
    }
    if(!this.dataProduce) {
      console.debug("不能生产数据");
      return;
    }
    this.dataProducer = await this.sendTransport.produceData({
      label            : "taoyao",
      ordered          : false,
      priority         : "medium",
      // maxRetransmits: 1,
      maxPacketLifeTime: 2000,
    });
    this.dataProducer.on("transportclose", () => {
      console.debug("数据生产者关闭（通道关闭）", this.dataProducer.id);
      this.dataProducer.close();
    });
    this.dataProducer.on("open", () => {
      console.debug("数据生产者打开", this.dataProducer.id);
    });
    this.dataProducer.on("close", () => {
      console.debug("数据生产者关闭", this.dataProducer.id);
      this.dataProducer = null;
    });
    this.dataProducer.on("error", (error) => {
      console.debug("数据生产者异常", this.dataProducer.id, error);
    });
    // this.dataProducer.on("bufferedamountlow",  fn(bufferedAmount));
  }

  /**
   * 关闭数据生产者
   */
  async closeDataProducer() {
    this.mediaDataProducerClose(this.dataProducer?.id);
  }

  /**
   * 通过数据生产者发送数据
   * 
   * @param {*} data 数据
   */
  async sendDataProducer(data) {
    this.dataProducer?.send(data);
  }

  /**
   * 关闭数据生产者信令
   * 
   * @param {*} producerId 数据生产者ID
   */
  mediaDataProducerClose(producerId) {
    this.push(protocol.buildMessage("media::data::producer::close", {
      producerId,
      roomId: this.roomId,
    }));
  }

  /**
   * 关闭数据生产者信令
   * 
   * @param {*} message 信令消息
   */
  defaultMediaDataProducerClose(message) {
    const {
      producerId
    } = message.body;
    const producer = this.getProducer(producerId);
    if(!producer) {
      console.debug("关闭数据生产者（数据生产者无效）", producerId);
      return;
    }
    console.debug("关闭数据生产者", producerId);
    producer.close();
  }

  /**
   * 查询数据生产者状态信令
   * 
   * @param {*} producerId 生产者ID
   */
  async mediaDataProducerStatus(producerId) {
    return await this.request(protocol.buildMessage("media::data::producer::status", {
      producerId,
      roomId: this.roomId,
    }));
  }

  /**
   * 重启ICE信令
   */
  async mediaIceRestart() {
    if (this.sendTransport) {
      const response = await this.request(protocol.buildMessage("media::ice::restart", {
        roomId     : this.roomId,
        transportId: this.sendTransport.id
      }));
      const {
        iceParameters
      } = response.body;
      await this.sendTransport.restartIce({
        iceParameters
      });
    }
    if (this.recvTransport) {
      const response = await this.request(protocol.buildMessage("media::ice::restart", {
        roomId     : this.roomId,
        transportId: this.recvTransport.id
      }));
      const {
        iceParameters
      } = response.body;
      await this.recvTransport.restartIce({
        iceParameters
      });
    }
  }

  /**
   * 生产媒体
   * 
   * 如果需要加密：producer.rtpSender
   * const senderStreams  = sender.createEncodedStreams();
   * const readableStream = senderStreams.readable || senderStreams.readableStream;
   * const writableStream = senderStreams.writable || senderStreams.writableStream;
   * 
   * @returns 所有生产者
   */
  async mediaProduce(audioTrack, videoTrack) {
    if(!audioTrack || !videoTrack) {
      await this.checkDevice();
    }
    await this.mediaConnect();
    await this.produceAudio(audioTrack);
    await this.produceVideo(videoTrack);
    await this.produceData();
    return {
      dataProducer : this.dataProducer,
      audioProducer: this.audioProducer,
      videoProducer: this.videoProducer,
    }
  }

  /**
   * 连接媒体
   */
  async mediaConnect() {
    if(!this.sendTransport) {
      await this.createSendTransport();
    }
    if(!this.recvTransport) {
      await this.createRecvTransport();
    }
  }

  /**
   * 生产音频
   * 
   * @param {*} audioTrack 音频轨道（可以为空自动）
   */
  async produceAudio(audioTrack) {
    if (this.audioProducer) {
      console.debug("已经存在音频生产者");
      return;
    }
    if (
      !this.audioProduce ||
      !this.mediasoupDevice.canProduce("audio")
    ) {
      console.debug("不能生产音频数媒");
      return;
    }
    const codecOptions = {
      opusDtx    : true,
      opusFec    : true,
      opusNack   : true,
      opusStereo : true,
    };
    const track        = audioTrack || await this.getAudioTrack();
    this.audioTrack    = track;
    this.audioProducer = await this.sendTransport.produce({
      track,
      codecOptions,
      appData: {
        videoSource: this.videoSource
      },
    });
    this.callbackTrack(this.clientId, track);
    if (this.proxy && this.proxy.media) {
      this.proxy.media(track, this.audioProducer);
    } else {
      console.warn("终端没有实现服务代理");
    }
    this.audioProducer.on("transportclose", () => {
      console.debug("关闭音频生产者（通道关闭）", this.audioProducer.id);
      this.audioProducer.close();
    });
    this.audioProducer.on("trackended", () => {
      console.debug("关闭音频生产者（媒体结束）", this.audioProducer.id);
      this.audioProducer.close();
    });
    this.audioProducer.observer.on("close", () => {
      console.debug("关闭音频生产者", this.audioProducer.id);
      this.audioProducer = null;
    });
  }

  /**
   * 关闭音频生产者
   */
  async closeAudioProducer() {
    this.mediaProducerClose(this.audioProducer?.id);
  }

  /**
   * 暂停音频生产者
   */
  async pauseAudioProducer() {
    this.mediaProducerPause(this.audioProducer?.id);
  }

  /**
   * 恢复音频生产者
   */
  async resumeAudioProducer() {
    this.mediaProducerResume(this.audioProducer?.id);
  }

  /**
   * 生产视频
   * 
   * @param {*} videoTrack 音频轨道（可以为空自动）
   */
  async produceVideo(videoTrack) {
    if (this.videoProducer) {
      console.debug("已经存在视频生产者");
      return;
    }
    if (
      !this.videoProduce ||
      !this.mediasoupDevice.canProduce("video")
    ) {
      console.debug("不能生产视频媒体");
      return;
    }
    const codecOptions = {
      videoGoogleStartBitrate: 400,
      videoGoogleMinBitrate  : 800,
      videoGoogleMaxBitrate  : 1600,
    };
    let codec;
    if(this.forceVP8) {
      codec = this.mediasoupDevice.rtpCapabilities.codecs.find((c) => c.mimeType.toLowerCase() === "video/vp8");
      if (codec) {
        console.debug("强制使用VP8视频编码");
      } else {
        console.debug("不支持VP8视频编码");
      }
    }
    if (this.forceVP9) {
      codec = this.mediasoupDevice.rtpCapabilities.codecs.find((c) => c.mimeType.toLowerCase() === "video/vp9");
      if (codec) {
        console.debug("强制使用VP9视频编码");
      } else {
        console.debug("不支持VP9视频编码");
      }
    }
    if (this.forceH264) {
      codec = this.mediasoupDevice.rtpCapabilities.codecs.find((c) => c.mimeType.toLowerCase() === "video/h264");
      if (codec) {
        console.debug("强制使用H264视频编码");
      } else {
        console.debug("不支持H264视频编码");
      }
    }
    let encodings;
    if (this.useLayers) {
      const priorityVideoCodec = this.mediasoupDevice.rtpCapabilities.codecs.find((c) => c.kind === "video");
      if ((this.forceVP9 && codec) || priorityVideoCodec.mimeType.toLowerCase() === "video/vp9") {
        encodings = defaultSvcEncodings;
      } else {
        encodings = defaultSimulcastEncodings;
      }
    }
    const track        = videoTrack || await this.getVideoTrack();
    this.videoTrack    = track;
    this.videoProducer = await this.sendTransport.produce({
      codec,
      track,
      encodings,
      codecOptions,
      appData: {
        videoSource: this.videoSource
      },
    });
    this.callbackTrack(this.clientId, track);
    if (this.proxy && this.proxy.media) {
      this.proxy.media(track, this.videoProducer);
    } else {
      console.warn("终端没有实现服务代理");
    }
    this.videoProducer.on("transportclose", () => {
      console.debug("关闭视频生产者（通道关闭）", this.videoProducer.id);
      this.videoProducer.close();
    });
    this.videoProducer.on("trackended", () => {
      console.debug("关闭视频生产者（媒体结束）", this.videoProducer.id);
      this.videoProducer.close();
    });
    this.videoProducer.observer.on("close", () => {
      console.debug("关闭视频生产者", this.videoProducer.id);
      this.videoProducer = null;
    });
  }

  /**
   * 关闭视频生产者
   */
  async closeVideoProducer() {
    this.mediaProducerClose(this.videoProducer?.id);
  }

  /**
   * 暂停视频生产者
   */
  async pauseVideoProducer() {
    this.mediaProducerPause(this.videoProducer?.id);
  }

  /**
   * 恢复视频生产者
   */
  async resumeVideoProducer() {
    this.mediaProducerResume(this.videoProducer?.id);
  }

  /**
   * 切换视频来源
   * 
   * @param {*} videoSource 视频来源（可以为空）
   */
  async exchangeVideoSource(videoSource) {
    console.debug("切换视频来源", videoSource, this.videoSource);
    const old = this.videoSource;
    if(videoSource) {
      this.videoSource = videoSource;
    } else {
      if(this.videoSource === "file") {
        this.videoSource = "camera";
      } else if(this.videoSource === "camera") {
        this.videoSource = "screen";
      } else if(this.videoSource === "screen") {
        this.videoSource = "file";
      } else {
        this.videoSource = "camera";
      }
    }
    await this.updateVideoProducer();
    if(old === "file") {
      this.closeFileVideo();
    }
  }

  /**
   * 更新视频轨道
   * 
   * @param {*} 更新视频轨道
   */
  async updateVideoProducer(videoTrack) {
    const track = videoTrack || await this.getVideoTrack();
    await this.videoProducer.replaceTrack({
      track
    });
    this.callbackTrack(this.clientId, track);
    if (this.proxy && this.proxy.media) {
      this.proxy.media(track, this.videoProducer);
    } else {
      console.warn("终端没有实现服务代理");
    }
  }

  /**
   * 验证设备
   */
  async checkDevice() {
    if (
      navigator.mediaDevices              &&
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
            console.debug("未知设备类型", v.kind, v.label);
            break;
        }
      });
      if (!audioEnabled && this.audioProduce) {
        this.platformError("没有音频媒体设备");
        // 强制修改
        this.audioProduce = false;
      }
      if (!videoEnabled && this.videoProduce) {
        this.platformError("没有视频媒体设备");
        // 强制修改
        this.videoProduce = false;
      }
    } else {
      this.platformError("没有媒体权限");
    }
  }

  /**
   * 关闭生产者信令
   * 
   * @param {*} producerId 生产者ID
   */
  mediaProducerClose(producerId) {
    this.push(protocol.buildMessage("media::producer::close", {
      producerId,
      roomId: this.roomId,
    }));
  }

  /**
   * 关闭生产者信令
   * 
   * @param {*} message 信令消息
   */
  async defaultMediaProducerClose(message) {
    const {
      producerId
    } = message.body;
    const producer = this.getProducer(producerId);
    if(!producer) {
      console.debug("关闭生产者（生产者无效）", producerId);
      return;
    }
    console.debug("关闭生产者", producerId);
    producer.close();
  }

  /**
   * 暂停生产者信令
   * 
   * @param {*} producerId 生产者ID
   */
  mediaProducerPause(producerId) {
    const producer = this.getProducer(producerId);
    if(!producer) {
      console.debug("暂停生产者（生产者无效）", producerId);
      return;
    }
    if(producer.paused) {
      console.debug("暂停生产者（生产者已经暂停）", producerId);
      return;
    }
    console.debug("暂停生产者", producerId);
    this.push(protocol.buildMessage("media::producer::pause", {
      producerId,
      roomId: this.roomId,
    }));
  }

  /**
   * 暂停生产者信令
   * 
   * @param {*} message 消息
   */
  async defaultMediaProducerPause(message) {
    const {
      producerId
    } = message.body;
    const producer = this.getProducer(producerId);
    if (!producer) {
      console.debug("暂停生产者（生产者无效）", producerId);
      return;
    }
    if(producer.paused) {
      console.debug("暂停生产者（生产者已经暂停）", producerId);
      return;
    }
    console.debug("暂停生产者", producerId);
    producer.pause();
  }

  /**
   * 恢复生产者信令
   * 
   * @param {*} producerId 生产者ID
   */
  mediaProducerResume(producerId) {
    const producer = this.getProducer(producerId);
    if(!producer) {
      console.debug("恢复生产者（生产者无效）", producerId);
      return;
    }
    if(!producer.paused) {
      console.debug("恢复生产者（生产者已经恢复）", producerId);
      return;
    }
    console.debug("恢复生产者", producerId);
    this.push(protocol.buildMessage("media::producer::resume", {
      producerId,
      roomId: this.roomId,
    }));
  }

  /**
   * 恢复生产者信令
   * 
   * @param {*} message 信令消息
   */
  async defaultMediaProducerResume(message) {
    const {
      producerId
    } = message.body;
    const producer = this.getProducer(producerId);
    if (!producer) {
      console.debug("恢复生产者（生产者无效）", producerId);
      return;
    }
    if(!producer.paused) {
      console.debug("恢复生产者（生产者已经恢复）", producerId);
      return;
    }
    console.debug("恢复生产者", producerId);
    producer.resume();
  }

  /**
   * 媒体生产者评分信令
   * 
   * @param {*} message 信令消息
   */
  defaultMediaProducerScore(message) {
    console.debug("生产者评分", message);
  }

  /**
   * 查询生产者状态信令
   * 
   * @param {*} producerId 生产者ID
   */
  async mediaProducerStatus(producerId) {
    return await this.request(protocol.buildMessage("media::producer::status", {
      producerId,
      roomId: this.roomId,
    }));
  }

  /**
   * 关闭通道信令
   * 
   * @param {*} transportId 通道ID
   */
  mediaTransportClose(transportId) {
    console.debug("关闭通道", transportId);
    this.push(protocol.buildMessage("media::transport::close", {
      transportId,
      roomId: this.roomId,
    }));
  }

  /**
   * 关闭通道信令
   * 
   * @param {*} message 信令消息
   */
  defaultMediaTransportClose(message) {
    const {
      roomId,
      transportId
    } = message.body;
    if(this.recvTransport && this.recvTransport.id === transportId) {
      console.debug("关闭接收通道", transportId);
      this.recvTransport.close();
      this.recvTransport = null;
    } else if(this.sendTransport && this.sendTransport.id === transportId) {
      console.debug("关闭发送通道", transportId);
      this.sendTransport.close();
      this.sendTransport = null;
    } else {
      console.debug("关闭通道无效", roomId, transportId);
    }
  }

  /**
   * 查询通道状态信令
   * 
   * @param {*} transportId 通道ID
   */
  async mediaTransportStatus(transportId) {
    return await this.request(protocol.buildMessage('media::transport::status', {
      transportId,
      roomId: this.roomId,
    }));
  }

  /**
   * 创建媒体发送通道
   */
  async createSendTransport() {
    if (
      !this.dataProduce  &&
      !this.audioProduce &&
      !this.videoProduce
    ) {
      console.debug("没有任何数据生产忽略创建媒体发送通道");
      return;
    }
    const response = await this.request(protocol.buildMessage("media::transport::webrtc::create", {
      roomId          : this.roomId,
      forceTcp        : this.forceTcp,
      producing       : true,
      consuming       : false,
      sctpCapabilities: this.dataProduce ? this.mediasoupDevice.sctpCapabilities : undefined,
    }));
    const {
      transportId,
      iceCandidates,
      iceParameters,
      dtlsParameters,
      sctpParameters,
    } = response.body;
    this.sendTransport = await this.mediasoupDevice.createSendTransport({
      iceCandidates,
      iceParameters,
      sctpParameters,
      id            : transportId,
      iceServers    : [],
      dtlsParameters: {
        ...dtlsParameters,
        role: "auto",
      },
      proprietaryConstraints: {
        optional: [{
          googDscp               : true,
          // googIPv6            : true,
          // DtlsSrtpKeyAgreement: true,
        }],
      },
    });
    this.sendTransport.on("connect", ({
      dtlsParameters
    }, callback, errback) => {
      this.request(protocol.buildMessage("media::transport::webrtc::connect", {
        dtlsParameters,
        roomId     : this.roomId,
        transportId: this.sendTransport.id,
      }))
      .then(callback)
      .catch(errback);
    });
    this.sendTransport.on("produce", ({
      kind,
      appData,
      rtpParameters
    }, callback, errback) => {
      this.request(protocol.buildMessage("media::produce", {
        kind,
        appData,
        rtpParameters,
        roomId     : this.roomId,
        transportId: this.sendTransport.id,
      }))
      .then((response) => {
        const {
          streamId,
          producerId
        } = response.body;
        callback({
          id: producerId
        });
      })
      .catch(errback);
    });
    this.sendTransport.on("producedata", ({
      label,
      appData,
      protocol,
      sctpStreamParameters
    }, callback, errback) => {
      this.request(taoyaoProtocol.buildMessage("media::data::produce", {
        label,
        appData,
        protocol,
        sctpStreamParameters,
        roomId     : this.roomId,
        transportId: this.sendTransport.id,
      }))
      .then((response) => {
        const {
          treamId,
          producerId
        } = response.body;
        callback({
          id: producerId
        });
      })
      .catch(errback);
    });
  }

  /**
   * 创建媒体接收通道
   */
  async createRecvTransport() {
    if (
      !this.dataConsume  &&
      !this.audioConsume &&
      !this.videoConsume
    ) {
      console.debug("没有任何数据消费忽略创建媒体接收通道");
      return;
    }
    const response = await this.request(protocol.buildMessage("media::transport::webrtc::create", {
      roomId          : this.roomId,
      forceTcp        : this.forceTcp,
      producing       : false,
      consuming       : true,
      sctpCapabilities: this.dataProduce ? this.mediasoupDevice.sctpCapabilities : undefined,
    }));
    const {
      transportId,
      iceCandidates,
      iceParameters,
      dtlsParameters,
      sctpParameters,
    } = response.body;
    this.recvTransport = await this.mediasoupDevice.createRecvTransport({
      iceCandidates,
      iceParameters,
      sctpParameters,
      id            : transportId,
      iceServers    : [],
      dtlsParameters: {
        ...dtlsParameters,
        role: "auto",
      },
      proprietaryConstraints: {
        optional: [{
          googDscp               : true,
          // googIPv6            : true,
          // DtlsSrtpKeyAgreement: true,
        }],
      },
    });
    this.recvTransport.on("connect", ({
      dtlsParameters
    }, callback, errback) => {
      this.request(protocol.buildMessage("media::transport::webrtc::connect", {
        dtlsParameters,
        roomId     : this.roomId,
        transportId: this.recvTransport.id,
      }))
      .then(callback)
      .catch(errback);
    });
  }

  /**
   * 视频方向变化信令
   * 
   * @param {*} message 信令消息
   */
  defaultMediaVideoOrientationChange(message) {
    console.debug("视频方向变化", message);
  }

  /**
   * 错误回调
   * 
   * @param {*} message 错误消息
   * @param {*} error   异常信息
   */
  platformError(message, error) {
    if (this.callback) {
      let callbackMessage;
      if(message instanceof Object) {
        callbackMessage = message;
      } else {
        callbackMessage         = protocol.buildMessage("platform::error", {});
        callbackMessage.code    = "9999";
        callbackMessage.message = message;
      }
      this.callback(callbackMessage, error);
    } else {
      if (error) {
        console.error("发生异常", message, error);
      } else {
        console.warn("发生错误", message);
      }
    }
  }

  /**
   * 平台异常信令
   *
   * @param {*} message 消息
   */
  defaultPlatformError(message) {
    const {
      code
    } = message;
    if (code === "3401") {
      // 没有授权直接关闭
      this.closeAll();
    } else {
      console.warn("平台异常", message);
    }
  }

  /**
   * 重启平台信令
   * 
   * @returns 响应
   */
  async platformReboot() {
    return await this.request(protocol.buildMessage("platform::reboot", {}));
  }

  /**
   * 重启平台信令
   * 
   * @param {*} message 信令消息
   */
  defaultPlatformReboot(message) {
    console.debug("重启平台", message);
  }

  /**
   * 执行命令信令
   * 
   * @param {*} script 命令
   * 
   * @returns 响应
   */
  async platformScript(script) {
    return await this.request(protocol.buildMessage("platform::script", {
      script
    }));
  }

  /**
   * 关闭平台信令
   * 
   * @returns 响应
   */
  async platformShutdown() {
    return await this.request(protocol.buildMessage("platform::shutdown", {}));
  }

  /**
   * 关闭平台信令
   * 
   * @param {*} message 信令消息
   */
  defaultPlatformShutdown(message) {
    console.debug("平台关闭", message);
  }

  /**
   * 房间广播信令
   * 
   * @param {*} message 信令消息
   */
  roomBroadcast(message) {
    this.push(protocol.buildMessage("room::broadcast", {
      ...message,
      roomId : this.roomId,
    }));
  }

  /**
   * 房间广播信令
   * 
   * @param {*} message 信令消息
   */
  defaultRoomBroadcast(message) {
    console.debug("房间广播", message);
  }

  /**
   * 房间终端ID集合信令
   * 
   * @param {*} clientId 终端ID
   * 
   * @returns 房间终端ID集合
   */
  async roomClientListId(clientId) {
    const response = await this.request(protocol.buildMessage("room::client::list::id", {
      roomId  : this.roomId,
      clientId: clientId
    }));
    return response.body;
  }

  /**
   * 房间终端列表信令
   * 
   * @param {*} roomId 房间ID
   * 
   * @returns 设备列表
   */
  async roomClientList(roomId) {
    const response = await this.request(protocol.buildMessage("room::client::list", {
      roomId: roomId || this.roomId
    }));
    return response.body;
  }

  /**
   * 房间终端列表信令
   *
   * @param {*} message 信令消息
   */
  defaultRoomClientList(message) {
    const {
      clients
    } = message.body;
    clients.forEach(v => {
      if (v.clientId === this.clientId) {
        // 忽略自己
      } else {
        this.remoteClients.set(v.clientId, new RemoteClient(v));
      }
    });
  }

  /**
   * 关闭房间信令
   */
  async roomClose() {
    this.push(protocol.buildMessage("room::close", {
      roomId: this.roomId,
    }));
  }

  /**
   * 关闭房间信令
   *
   * @param {*} message 信令消息
   */
  defaultRoomClose(message) {
    const {
      roomId
    } = message.body;
    if (roomId !== this.roomId) {
      return;
    }
    console.debug("关闭房间", roomId);
    this.closeRoomMedia();
  }

  /**
   * 创建房间信令
   *
   * @param {*} room 房间信息
   *
   * @returns 响应消息
   */
  async roomCreate(room) {
    if(this.roomId) {
      this.platformError("终端已经进入房间");
      return {
        code   : 9999,
        message: "终端已经进入房间"
      };
    }
    console.debug("创建房间", room);
    const response = await this.request(protocol.buildMessage("room::create", {
      ...room
    }));
    return response.body;
  }

  /**
   * 创建房间信令
   * 用于房间重建
   * 
   * @param {*} message 信令消息
   */
  async defaultRoomCreate(message) {
    console.debug("创建房间", message);
    const {
      roomId,
      password
    } = message.body;
    if(this.roomId && roomId === this.roomId) {
      await this.roomLeave();
      await this.roomEnter(roomId, password);
      await this.mediaProduce();
    }
  }

  /**
   * 进入房间信令
   *
   * @param {*} roomId   房间ID
   * @param {*} password 房间密码
   */
  async roomEnter(roomId, password) {
    if(this.roomId) {
      this.platformError("终端已经进入房间");
      return {
        code   : 9999,
        message: "终端已经进入房间",
      };
    }
    this.roomId  = roomId;
    let response = await this.request(protocol.buildMessage("media::router::rtp::capabilities", {
      roomId: this.roomId,
    }));
    if(response.code !== SUCCESS_CODE) {
      this.roomId = null;
      this.platformError(response);
      return response;
    }
    const routerRtpCapabilities = response.body.rtpCapabilities;
    this.mediasoupDevice        = new mediasoupClient.Device();
    await this.mediasoupDevice.load({ routerRtpCapabilities });
    response = await this.request(protocol.buildMessage("room::enter", {
      roomId          : roomId,
      password        : password,
      rtpCapabilities : this.audioConsume || this.videoConsume || this.audioProduce || this.videoProduce ? this.mediasoupDevice.rtpCapabilities  : undefined,
      sctpCapabilities: this.dataConsume  || this.dataProduce                                            ? this.mediasoupDevice.sctpCapabilities : undefined,
    }));
    if(response.code !== SUCCESS_CODE) {
      this.roomId = null;
      this.platformError(response);
      return response;
    }
    return response;
  }

  /**
   * 进入房间信令
   * 其他终端进入房间
   *
   * @param {*} message 信令消息
   */
  defaultRoomEnter(message) {
    const {
      roomId,
      clientId,
      status
    } = message.body;
    if (clientId === this.clientId) {
      // 忽略自己
    } else if(this.remoteClients.has(clientId)) {
      console.debug("终端已经进入房间", clientId);
    } else {
      console.debug("远程终端进入房间", clientId);
      this.remoteClients.set(clientId, new RemoteClient(status));
    }
  }

  /**
   * 踢出房间信令
   * 
   * @param {*} clientId 终端ID
   */
  roomExpel(clientId) {
    this.push(protocol.buildMessage("room::expel", {
      roomId: this.roomId,
      clientId,
    }));
  }

  /**
   * 踢出房间信令
   * 
   * @param {*} message 信令消息
   */
  async defaultRoomExpel(message) {
    console.debug("收到提出房间信令", message);
    await this.roomLeave();
  }

  /**
   * 邀请终端信令
   * 
   * @param {*} clientId 终端ID
   */
  roomInvite(clientId) {
    this.push(protocol.buildMessage("room::invite", {
      roomId: this.roomId,
      clientId,
    }));
  }

  /**
   * 邀请终端信令
   * 
   * @param {*} message 信令消息
   */
  async defaultRoomInvite(message) {
    // 默认自动进入：如果需要确认使用回调函数重写
    const {
      roomId,
      password
    } = message.body;
    // H5只能同时进入一个房间
    if(this.roomId) {
      this.platformError("终端拒绝房间邀请");
      return;
    }
    console.debug("房间邀请终端", roomId);
    await this.roomEnter(roomId, password);
    await this.mediaProduce();
  }

  /**
   * 离开房间信令
   */
  async roomLeave() {
    this.push(protocol.buildMessage("room::leave", {
      roomId: this.roomId
    }));
    await this.closeRoomMedia();
  }

  /**
   * 离开房间信令
   *
   * @param {*} message 信令消息
   */
  defaultRoomLeave(message) {
    const { clientId } = message.body;
    if(clientId === this.clientId) {
      this.closeRoomMedia();
      console.debug("终端离开房间", clientId);
    } else if(this.remoteClients.has(clientId)) {
      const remoteClient = this.remoteClients.get(clientId);
      remoteClient.close();
      this.remoteClients.delete(clientId);
      console.debug("终端离开房间", clientId);
    } else {
      console.debug("终端已经离开", clientId);
    }
  }

  /**
   * 房间列表信令
   * 
   * @returns 房间列表
   */
  async roomList() {
    const response = await this.request(protocol.buildMessage("room::list"));
    return response.body;
  }

  /**
   * 房间状态信令
   * 
   * @param {*} roomId 房间ID
   * 
   * @returns 房间状态
   */
  async roomStatus(roomId) {
    const response = await this.request(protocol.buildMessage("room::status", {
      roomId: roomId || this.roomId
    }));
    return response.body;
  }

  /**
   * 发起会话信令
   * 
   * @param {*} clientId 目标ID
   * @param {*} audio    打开音频
   * @param {*} video    打开视频
   */
  async sessionCall(clientId, audio = true, video = true) {
    if (clientId == this.clientId) {
      this.platformError("不能监控自己");
      return;
    }
    await this.checkDevice();
    const response = await this.request(protocol.buildMessage("session::call", {
      clientId
    }));
    const {
      code,
      body,
      message
    } = response;
    if(code !== SUCCESS_CODE) {
      this.platformError(message);
      return;
    }
    const {
      name,
      sessionId
    } = body;
    console.debug("发起会话", clientId, sessionId);
    const session = new Session({
      name,
      clientId,
      sessionId,
      audioEnabled: this.audioProduce && audio,
      videoEnabled: this.videoProduce && video
    });
    this.sessionClients.set(sessionId, session);
  }

  /**
   * 发起会话信令
   * 
   * @param {*} message 信令消息
   */
  async defaultSessionCall(message) {
    await this.checkDevice();
    const {
      name,
      audio = true,
      video = true,
      clientId,
      sessionId
    } = message.body;
    console.debug("接收会话", clientId, sessionId, audio, video);
    const session = new Session({
      name,
      clientId,
      sessionId,
      audioEnabled: this.audioProduce && audio,
      videoEnabled: this.videoProduce && video
    });
    this.sessionClients.set(sessionId, session);
    await this.buildPeerConnection(session, sessionId);
    session.peerConnection.createOffer().then(async (description) => {
      await session.peerConnection.setLocalDescription(description);
      this.push(protocol.buildMessage("session::exchange", {
        sdp      : description.sdp,
        type     : description.type,
        sessionId: sessionId
      }));
    });
  }

  /**
   * 关闭媒体信令
   * 
   * @param {*} sessionId 会话ID
   */
  async sessionClose(sessionId) {
    this.push(protocol.buildMessage("session::close", {
      sessionId
    }));
  }

  /**
   * 关闭媒体信令
   * 
   * @param {*} message 信令消息
   */
  async defaultSessionClose(message) {
    const { sessionId } = message.body;
    const session = this.sessionClients.get(sessionId);
    if(session) {
      console.debug("关闭媒体", sessionId);
      await session.close();
      this.sessionClients.delete(sessionId);
    } else {
      console.debug("关闭媒体（会话无效）", sessionId);
    }
  }

  /**
   * 媒体交换信令
   * 
   * @param {*} message 信令消息
   */
  async defaultSessionExchange(message) {
    const body = message.body;
    const {
      type,
      candidate,
      sessionId,
    } = body;
    const session = this.sessionClients.get(sessionId);
    if (type === "offer") {
      await this.buildPeerConnection(session, sessionId);
      await session.peerConnection.setRemoteDescription(new RTCSessionDescription(body));
      session.peerConnection.createAnswer().then(async description => {
        await session.peerConnection.setLocalDescription(description);
        this.push(protocol.buildMessage("session::exchange", {
          sdp      : description.sdp,
          type     : description.type,
          sessionId: sessionId
        }));
      });
    } else if (type === "answer") {
      await session.peerConnection.setRemoteDescription(new RTCSessionDescription(body));
    } else if (type === "candidate") {
      await session.addIceCandidate(candidate);
    } else {
      console.warn("媒体交换无效类型", body);
    }
  }

  /**
   * 暂停媒体信令
   * 
   * @param {*} sessionId 会话ID
   * @param {*} type      媒体类型
   */
  async sessionPause(sessionId, type) {
    const session = this.sessionClients.get(sessionId);
    if(session) {
      console.debug("暂停媒体", type, sessionId);
      this.push(protocol.buildMessage("session::pause", {
        type,
        sessionId
      }));
      session.pauseRemote(type);
    } else {
      console.debug("暂停媒体（会话无效）", type, sessionId);
    }
  }

  /**
   * 暂停媒体信令
   * 
   * @param {*} message 信令消息
   */
  async defaultSessionPause(message) {
    const {
      type,
      sessionId
    } = message.body;
    const session = this.sessionClients.get(sessionId);
    if(session) {
      console.debug("暂停媒体", type, sessionId);
      session.pause(type);
    } else {
      console.debug("暂停媒体（会话无效）", type, sessionId);
    }
  }

  /**
   * 恢复媒体信令
   * 
   * @param {*} sessionId 会话ID
   * @param {*} type      媒体类型
   */
  async sessionResume(sessionId, type) {
    const session = this.sessionClients.get(sessionId);
    if(session) {
      console.debug("恢复媒体", type, sessionId);
      this.push(protocol.buildMessage("session::resume", {
        type,
        sessionId
      }));
      session.resumeRemote(type);
    } else {
      console.debug("恢复媒体（会话无效）", type, sessionId);
    }
  }

  /**
   * 恢复媒体信令
   * 
   * @param {*} message 信令消息
   */
  async defaultSessionResume(message) {
    const {
      type,
      sessionId
    } = message.body;
    const session = this.sessionClients.get(sessionId);
    if(session) {
      console.debug("恢复媒体", type, sessionId);
      session.resume(type);
    } else {
      console.debug("恢复媒体（会话无效）", type, sessionId);
    }
  }

  /**
   * 系统信息信令
   * 
   * @returns 系统信息
   */
  async systemInfo() {
    return await this.request(protocol.buildMessage("system::info", {}));
  }

  /**
   * 重启系统信令
   * 
   * @returns 重启系统结果
   */
  async systemReboot() {
    return await this.request(protocol.buildMessage("system::reboot", {}));
  }

  /**
   * 关闭系统信令
   * 
   * @returns 关闭系统结果
   */
  async systemShutdown() {
    return await this.request(protocol.buildMessage("system::shutdown", {}));
  }
  
  /**
   * @param {*} session   会话
   * @param {*} sessionId 会话ID
   * 
   * @returns PeerConnection
   */
  async buildPeerConnection(session, sessionId) {
    if(session.peerConnection)  {
      return session.peerConnection;
    }
    const peerConnection = new RTCPeerConnection({
      "iceServers": this.webrtcConfig.iceServers || defaultRTCPeerConnectionConfig.iceServers
    });
    peerConnection.ontrack = event => {
      console.debug("会话添加远程媒体轨道", event);
      const track = event.track;
      if(track.kind === 'audio') {
        session.remoteAudioTrack   = track;
        session.remoteAudioEnabled = true;
      } else if(track.kind === 'video') {
        session.remoteVideoTrack   = track;
        session.remoteVideoEnabled = true;
      } else {
        console.warn("未知媒体类型", track);
      }
      this.callbackTrack(session.clientId, track);
      if(session.proxy && session.proxy.media) {
        session.proxy.media(track);
      } else {
        console.warn("远程会话没有实现代理", session);
      }
    };
    peerConnection.onicecandidate = event => {
      console.debug("会话媒体协商", event);
      this.push(protocol.buildMessage("session::exchange", {
        type      : "candidate",
        sessionId : sessionId,
        candidate : event.candidate
      }));
    };
    peerConnection.onnegotiationneeded = event => {
      console.debug("会话媒体重新协商", event);
      // TODO：重连
      if(peerConnection.connectionState === "connected") {
        peerConnection.restartIce();
      }
    }
    const localStream      = await this.getStream();
    session.localStream    = localStream;
    session.peerConnection = peerConnection;
    if(session.audioEnabled && localStream.getAudioTracks().length >= 0) {
      session.localAudioTrack = localStream.getAudioTracks()[0];
      if(session.localAudioTrack) {
        session.localAudioEnabled = true;
        await session.peerConnection.addTrack(session.localAudioTrack, localStream);
      } else {
        session.localAudioEnabled = false;
      }
    } else {
      session.localAudioEnabled = false;
    }
    if(session.videoEnabled && localStream.getVideoTracks().length >= 0) {
      session.localVideoTrack = localStream.getVideoTracks()[0];
      if(session.localVideoTrack) {
        session.localVideoEnabled = true;
        await session.peerConnection.addTrack(session.localVideoTrack, localStream);
      } else {
        session.localVideoEnabled = false;
      }
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
    const canvas  = document.createElement("canvas");
    canvas.width  = video.videoWidth;
    canvas.height = video.videoHeight;
    const context = canvas.getContext("2d");
    context.drawImage(video, 0, 0, video.videoWidth, video.videoHeight);
    const dataURL  = canvas.toDataURL("images/png");
    const download = document.createElement("a");
    download.href  = dataURL;
    download.download = "taoyao.png";
    // download.style.display = "none";
    // document.body.appendChild(download);
    download.click();
    // 释放资源
    canvas.remove();
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
        console.debug("本地录像机已经存在");
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
        audioBitsPerSecond: 256  * 1000,
        videoBitsPerSecond: 1600 * 1000,
        mimeType: 'video/webm;codecs=opus,h264',
      });
      me.mediaRecorder.onstop = (e) => {
        const blob             = new Blob(me.mediaRecorderChunks);
        const objectURL        = URL.createObjectURL(blob);
        const download         = document.createElement("a");
        download.href          = objectURL;
        download.download      = "taoyao.mp4";
        // download.style.display = "none";
        // document.body.appendChild(download);
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
        console.debug("本地录像机无效");
        return;
      }
      me.mediaRecorder.stop();
      me.mediaRecorder = null;
    }
  }

  /**
   * 配置视频
   * 
   * @param {*} label 配置
   */
  setLocalVideoConfig(label) {
    const me = this;
    // TODO：设置本地配置
    me.updateVideoProducer();
  }

  /**
   * 配置视频
   * 
   * @param {*} label 配置
   */
  setVideoConfig(clientId, label) {
    const me = this;
    if(clientId === me.clientId) {
      me.setLocalVideoConfig(label);
    }
    // TODO：更新远程配置
  }

  /**
   * 配置媒体轨道
   * 参考连接：https://developer.mozilla.org/en-US/docs/Web/API/MediaTrackSettings
   * 
   * @param {*} track   媒体轨道
   * @param {*} setting 支持属性：navigator.mediaDevices.getSupportedConstraints()
   */
  async setTrack(track, setting) {
   await track.applyConstraints(Object.assign(track.getSettings(), setting));
  }

  /**
   * 媒体回调
   * 
   * @param {*} clientId 终端ID
   * @param {*} track    媒体轨道
   */
  callbackTrack(clientId, track) {
    const callbackMessage = protocol.buildMessage("media::track", {
      track,
      clientId,
    });
    callbackMessage.code    = SUCCESS_CODE;
    callbackMessage.message = SUCCESS_MESSAGE;
    this.callback(callbackMessage);
  }

  /**
   * 统计设备信息
   * 
   * @param {*} clientId 终端ID
   *
   * @returns 设备信息统计
   */
  async getClientStats(clientId) {
    const stats = {};
    if(clientId === this.clientId) {
      stats.sendTransport = await this.sendTransport.getStats();
      stats.recvTransport = await this.recvTransport.getStats();
      stats.audioProducer = await this.audioProducer.getStats();
      stats.videoProducer = await this.videoProducer.getStats();
    } else {
      const consumers = Array.from(this.consumers.values());
      for(const consumer of consumers) {
        if(clientId === consumer.sourceId) {
          stats[consumer.kind] = await consumer.getStats();
        }
      }
    }
    return stats;
  }

  /**
   * 关闭媒体资源
   * 
   * @param {*} mediaStream 媒体资源
   */
  closeMediaStream(mediaStream) {
    if(!mediaStream) {
      return;
    }
    mediaStream.getAudioTracks().forEach(oldTrack => {
      console.debug("关闭音频媒体", oldTrack);
      oldTrack.stop();
    });
    mediaStream.getVideoTracks().forEach(oldTrack => {
      console.debug("关闭视频媒体", oldTrack);
      oldTrack.stop();
    });
  }

  /**
   * 关闭媒体轨道
   * 
   * @param {*} mediaTrack 媒体轨道
   */
  closeMediaTrack(mediaTrack) {
    if(!mediaTrack) {
      return;
    }
    mediaTrack.stop();
  }

  /**
   * 关闭视频房间媒体
   */
  async closeRoomMedia() {
    console.debug("关闭视频房间媒体");
    this.roomId = null;
    await this.close();
    await this.closeRoomMediaConsumer();
    await this.closeRoomMediaProducer();
    await this.closeRoomMediaConnect();
    this.closeFileVideo();
  }

  /**
   * 关闭媒体连接
   */
  async closeRoomMediaConnect() {
    if (this.sendTransport) {
      await this.sendTransport.close();
      this.sendTransport = null;
    }
    if (this.recvTransport) {
      await this.recvTransport.close();
      this.recvTransport = null;
    }
    if(this.mediasoupDevice) {
      this.mediasoupDevice = null;
    }
  }

  /**
   * 关闭媒体生产者
   */
  async closeRoomMediaProducer() {
    if(this.audioProducer) {
      await this.audioProducer.close();
      this.audioProducer = null;
    }
    if(this.videoProducer) {
      await this.videoProducer.close();
      this.videoProducer = null;
    }
    if(this.dataProducer) {
      await this.dataProducer.close();
      this.dataProducer = null;
    }
  }

  /**
   * 关闭媒体消费者
   */
  async closeRoomMediaConsumer() {
    this.consumers.forEach(async (consumer, consumerId) => {
      await consumer.close();
    });
    this.consumers.clear();
    this.dataConsumers.forEach(async (dataConsumer, consumerId) => {
      await dataConsumer.close();
    });
    this.dataConsumers.clear();
    this.remoteClients.forEach(async (remoteClient, clientId) => {
      await remoteClient.close();
    });
    this.remoteClients.clear();
  }

  /**
   * 关闭视频会话媒体
   */
  closeSessionMedia() {
    console.debug("关闭视频会话媒体");
    this.sessionClients.forEach((session, sessionId) => {
      session.close();
      console.debug("关闭会话", sessionId);
    });
    this.sessionClients.clear();
    this.closeFileVideo();
  }

  /**
   * 关闭资源
   */
  closeAll() {
    if(this.closed) {
      return;
    }
    this.closed = true;
    this.closeRoomMedia();
    this.closeSessionMedia();
    signalChannel.close();
  }
}

export { Taoyao };
