/**
 * 桃夭
 */
import { Logger } from "./Logger.js";
import { TaoyaoClient } from "./TaoyaoClient.js";
import * as mediasoupClient from 'mediasoup-client';
import {
  config,
  protocol,
  defaultAudioConfig,
  defaultVideoConfig,
} from "./Config.js";

// 日志
const logger = new Logger();

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
  connect: async function (address, callback, reconnection = true) {
    let self = this;
    self.address = address;
    self.callback = callback;
    self.reconnection = reconnection;
    return new Promise((resolve, reject) => {
      logger.debug("连接信令通道", address);
      self.channel = new WebSocket(address);
      self.channel.onopen = async function (e) {
        logger.debug("打开信令通道", e);
        // 注册终端
        const battery = await navigator.getBattery();
        self.push(
          protocol.buildMessage("client::register", {
            ip: 'localhost',
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
        if (self.reconnection) {
          self.reconnect();
        }
        reject(e);
      };
      self.channel.onerror = function (e) {
        logger.error("信令通道异常", self.channel, e);
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
        logger.debug("信令通道消息", e.data);
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
        if (!done && self.callback) {
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
      logger.info("信令通道重连", self.address);
      self.connect(self.address, self.callback, true);
      self.lockReconnect = false;
    }, self.connectionTimeout);
    if (self.connectionTimeout >= self.maxReconnectionDelay) {
      self.connectionTimeout = self.maxReconnectionDelay;
    } else {
      self.connectionTimeout =
        self.connectionTimeout * self.reconnectionDelayGrowFactor;
    }
  },
  /**
   * 异步请求
   * 
   * @param {*} data 消息内容
   * @param {*} callback 注册回调
   */
  push: function (data, callback) {
    // 注册回调
    let self = this;
    if (callback) {
      self.callbackMapping.set(data.header.id, callback);
    }
    // 发送消息
    self.channel.send(JSON.stringify(data));
  },
  /**
   * 同步请求
   * 
   * @param {*} data 消息内容
   * 
   * @returns Promise
   */
  request: async function(data) {
    let self = this;
    return new Promise((resolve, reject) => {
      let callback = false;
      // 设置回调
      self.callbackMapping.set(data.header.id, (response) => {
        callback = true;
        resolve(response);
        return true;
      });
      // 发送请求
      self.channel.send(JSON.stringify(data));
      // 设置超时
      setTimeout(() => {
        if(!callback) {
          reject("请求超时", data);
        }
      }, 5000);
    });
  },
  /**
   * 关闭通道
   */
  close: function () {
    let self = this;
    self.reconnection = false;
    self.channel.close();
    clearTimeout(self.heartbeatTimer);
  },
  /**
   * 默认回调
   *
   * @param {*} data 消息内容
   */
  defaultCallback: function (data) {
    let self = this;
    logger.debug("没有适配信令消息默认处理", data);
    switch (data.header.signal) {
      case "client::config":
        self.defaultClientConfig(data);
        break;
      case "client::register":
        logger.info("桃夭终端注册成功");
        break;
      case "platform::error":
        logger.error("信令发生错误", data);
        break;
    }
  },
  /**
   * 默认配置回调
   *
   * @param {*} data 消息内容
   */
  defaultClientConfig: function (data) {
    config.webrtc = data.body.webrtc;
    config.audio = { ...config.defaultAudioConfig, ...data.body.media.audio };
    config.video = { ...config.defaultVideoConfig, ...data.body.media.video };
    logger.info("终端配置", config.audio, config.video, config.webrtc);
  },
  /**
   * 默认终端重启回调
   *
   * @param {*} data 消息内容
   */
  defaultClientReboot: function (data) {
    logger.info("重启终端");
    location.reload();
  },
};

/**
 * 桃夭
 */
class Taoyao {
  // 发送信令
  push = null;
  // 请求信令
  request = null;
  // 本地视频
  localVideo = null;
  // 本地终端
  localClient;
  // 远程终端
  remoteClientList;
  // 媒体通道
  sendTransport = null;
  recvTransport = null;
  // 信令通道
  signalChannel = null;
  // 媒体设备
  mediasoupDevice = null;
  // 是否消费
  consume = true;
  // 是否生产
  produce = true;
  // 是否生产音频
  audioProduce = true && this.produce;
  // 是否生成视频
  videoProduce = true && this.produce;
  // 音频生产者
  audioProducer = null;
  // 视频生产者
  videoProducer = null;
  // 消费者
	consumers = new Map();
  // 数据消费者
	dataConsumers = new Map();

  /**
   * 打开信令通道
   *
   * @param {*} callback
   *
   * @returns
   */
  buildChannel = async function (callback) {
    signalChannel.taoyao = this;
    this.signalChannel = signalChannel;
    // 不能直接this.push = this.signalChannel.push这样导致this对象错误
    this.push = function (data, pushCallback) {
      this.signalChannel.push(data, pushCallback);
    };
    this.request = async function(data) {
      return await this.signalChannel.request(data);
    }
    return this.signalChannel.connect(config.signal(), callback);
  };
  /**
   * 设置本地媒体
   */
  buildLocal = function() {
    new mediasoupClient.Device();
  };
  /**
   * 打开媒体通道
   */
  buildMediaTransport = function() {
    let self = this;
    // 释放资源
    self.close();

  }
  /**
   * 关闭
   */
  close = function() {
    let self = this;
    if(self.sendTransport) {
      self.sendTransport.close();
    }
    if(self.recvTransport) {
      self.recvTransport.close();
    }
    if(self.signalChannel) {
      self.signalChannel.close();
    }
  };
}

export { Taoyao };
