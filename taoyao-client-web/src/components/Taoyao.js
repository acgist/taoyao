/**
 * 桃夭
 */
import { Logger } from "./Logger.js";
import { TaoyaoClient } from "./TaoyaoClient.js";
import { config, protocol, defaultAudioConfig, defaultVideoConfig } from "./Config.js";

// 日志
const logger = new Logger();

/**
 * 信令通道
 * TODO：获取IP/MAC/信号强度
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
  // 重连失败时间增长倍数
  reconnectionDelayGrowFactor: 2,
  /**
   * 心跳
   */
  heartbeat: function () {
    let self = this;
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
        logger.warn("发送心跳失败", self.channel);
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
  connect: function (address, callback, reconnection = true) {
    let self = this;
    self.address = address;
    self.callback = callback;
    return new Promise((resolve, reject) => {
      logger.debug("连接信令通道", address);
      self.channel = new WebSocket(address);
      self.channel.onopen = async function (e) {
        logger.debug("打开信令通道", e);
        // 注册终端
        const battery = await navigator.getBattery();
        self.push(
          protocol.buildMessage("client::register", {
            ip: null,
            mac: null,
            signal: 100,
            battery: battery.level * 100,
            charging: battery.charging,
            username: config.username,
            password: config.password,
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
        logger.error("信令通道关闭", self.channel, e);
        if (reconnection) {
          self.reconnect();
        }
        reject(e);
      };
      self.channel.onerror = function (e) {
        logger.error("信令通道异常", self.channel, e);
        if (reconnection) {
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
        let data = JSON.parse(e.data);
        // 请求回调
        if (self.callbackMapping.has(data.header.id)) {
          try {
            done = self.callbackMapping.get(data.header.id)(data);
          } finally {
            self.callbackMapping.delete(data.header.id);
          }
        }
        // 全局回调
        if (self.callback) {
          done = self.callback(data);
        }
        // 默认回调
        if (!done) {
          self.defaultCallback(data);
        }
      };
    });
  },
  /**
   * 重连
   */
  reconnect: function () {
    let self = this;
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
    if (self.connectionTimeout >= self.maxReconnectionDelay) {
      self.connectionTimeout = self.maxReconnectionDelay;
    } else {
      self.connectionTimeout = self.connectionTimeout * self.reconnectionDelayGrowFactor;
    }
  },
  /**
   * 发送消息
   *
   * @param {*} data 消息内容
   * @param {*} callback 注册回调
   */
  push: function (data, callback) {
    // 注册回调
    if (data && callback) {
      this.callbackMapping.set(data.header.id, callback);
    }
    // 发送消息
    if (data && data.header) {
      this.channel.send(JSON.stringify(data));
    } else {
      this.channel.send(data);
    }
  },
  /**
   * 关闭通道
   */
  close: function () {
    clearTimeout(this.heartbeatTimer);
  },
  /**
   * 默认回调
   *
   * @param {*} data 消息内容
   */
  defaultCallback: function (data) {
    console.debug("没有适配信令消息默认处理", data);
    switch (data.header.signal) {
      case "platform::error":
        console.error("信令发生错误", data);
        break;
    }
  },
  /**
   * 默认配置回调
   *
   * @param {*} data 消息内容
   */
  defaultClientConfig: function (data) {
    let self = this;
    // 配置终端
    self.taoyao
      .configMedia(data.body.media.audio, data.body.media.video)
      .configWebrtc(data.body.webrtc);
    // 打开媒体通道
    let videoId = self.taoyao.videoId;
    if (videoId) {
      self.taoyao
        .buildLocalMedia()
        .then((stream) => {
          self.taoyao.buildMediaChannel(videoId, stream);
        })
        .catch((e) => console.error("打开终端媒体失败", e));
      console.debug("自动打开媒体通道", videoId);
    } else {
      console.debug("没有配置本地媒体信息跳过自动打开媒体通道");
    }
  },
  /**
   * 默认终端重启回调
   *
   * @param {*} data 消息内容
   */
  defaultClientReboot: function (data) {
    console.info("重启终端");
    location.reload();
  },
};

/**
 * 桃夭
 */
class Taoyao {
  // 本地终端
  localClient;
  // 远程终端
  remoteClientList;
	// 设备状态
	audioEnabled = true;
	videoEnabled = true;
	// 媒体配置
	audioConfig = defaultAudioConfig;
	videoConfig = defaultVideoConfig;
  // 媒体通道
  transSend;
  transRecv;
	// 发送信令
	push = null;
	// 信令通道
	signalChannel = null;
  /**
   * 媒体配置
   * 
   * @param {*} audio 
   * @param {*} video 
   * 
   * @returns 
   */
	configMedia = function(audio = {}, video = {}) {
		this.audioConfig = {...this.audioConfig, ...audio};
		this.videoConfig = {...this.videoConfig, ...video};
		console.debug('终端媒体配置', this.audioConfig, this.videoConfig);
		return this;
	};
	/**
   * WebRTC配置
   * 
   * @param {*} config 
   * 
   * @returns 
   */
	configWebrtc = function(config = {}) {
		return this;
	};
	/**
   * 打开信令通道
   * 
   * @param {*} callback 
   * 
   * @returns 
   */
	buildChannel = function(callback) {
		signalChannel.taoyao = this;
		this.signalChannel = signalChannel;
		// 不能直接this.push = this.signalChannel.push这样导致this对象错误
		this.push = function(data, pushCallback) {
			this.signalChannel.push(data, pushCallback);
		};
		return this.signalChannel.connect(config.signal(), callback);
	};
}

export { Taoyao };
