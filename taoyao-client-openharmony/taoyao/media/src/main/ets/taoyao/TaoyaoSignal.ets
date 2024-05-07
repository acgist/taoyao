/**
 * 信令
 *
 * @author acgist
 */

import hilog from "@ohos.hilog";
import webSocket from "@ohos.net.webSocket";

import { setting } from "./Setting";

import taoyaoModule from "libtaoyao.so";

interface BusinessError<T = void> extends Error {
  code : number;
  data?: T;
}

class TaoyaoSignal {

  // WebSocket信令连接
  socket   : webSocket.WebSocket | null = null;
  // 是否关闭
  closed   : boolean = false;
  // 是否连接成功
  connected: boolean = false;
  // 心跳定时
  heartbeatTimer: number = 0;
  // 同步请求
  callbackMapping = new Map<number, Function>();
  // 当前消息索引
  index      : number = 0;
  // 最大消息索引
  maxIndex   : number = 999;
  // 当前终端索引
  clientIndex: number = 99999;

  /**
   * 连接信令
   */
  connect() {
    if(this.socket) {
      hilog.info(0x0000, "TaoyaoSignal", "信令以及连接关闭旧的连接：%s", setting.signalAddress);
      this.socket.close();
    }
    this.connected = false;
    this.socket = webSocket.createWebSocket();
    this.socket.on("open", (err: BusinessError, value: Object) => {
      hilog.info(0x0000, "TaoyaoSignal", "打开信令：%s", setting.signalAddress, value, err);
      this.register();
      this.connected = true;
    });
    this.socket.on("message", (err: BusinessError, value: string | ArrayBuffer) => {
      const message = value?.toString();
      hilog.debug(0x0000, "TaoyaoSignal", "信令消息：%s", message, err);
      try {
        this.onMessage(message);
      } catch (error) {
        hilog.error(0x0000, "TaoyaoSignal", "处理信令消息异常：%s", message, error);
      }
    });
    this.socket.on("close", (err: BusinessError, value: Object) => {
      hilog.error(0x0000, "TaoyaoSignal", "关闭信令：%s", setting.signalAddress, value, err);
      this.reconnect();
    });
    this.socket.on("error", (err: BusinessError) => {
      hilog.error(0x0000, "TaoyaoSignal", "信令异常：%s", setting.signalAddress, err);
      this.reconnect();
    });
    hilog.info(0x0000, "TaoyaoSignal", "连接信令：%s", setting.signalAddress);
    this.socket.connect(setting.signalAddress, (err: BusinessError, value: boolean) => {
      hilog.info(0x0000, "TaoyaoSignal", "信令连接成功：%s", setting.signalAddress, value, err);
    });
  };

  /**
   * 重连信令
   */
  reconnect() {
    if(this.closed) {
      // 已经关闭忽略重连
    } else {
      hilog.info(0x0000, "TaoyaoSignal", "重连信令连接：%s", setting.signalAddress);
      setTimeout((): void => this.connect(), 5000);
    }
  }

  /**
   * 关闭信令
   */
  close() {
    hilog.info(0x0000, "TaoyaoSignal", "关闭信令：%s", setting.signalAddress);
    this.closed    = true;
    this.connected = false;
    if(this.socket) {
      this.socket.close();
    }
  }

  /**
   * 注册信令
   */
  async register() {
    const response: Record<string, Object> = await this.request("client::register", {
      "name"      : setting.signal.name,
      "clientId"  : setting.signal.clientId,
      "clientType": setting.signal.clientType,
      "username"  : setting.signal.username,
      "password"  : setting.signal.password,
      "battery"   : 100,
      "charging"  : true
    });
    const body  = response.body as Record<string, Object>;
    const index = body.index    as number;
    this.clientIndex = index;
    hilog.info(0x0000, "TaoyaoSignal", "信令注册成功：%d", index);
    this.heartbeat();
  }

  /**
   * 心跳信令
   */
  heartbeat() {
    if(this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
    }
    this.heartbeatTimer = setInterval(() => {
      this.send("client::heartbeat", {
        "battery" : 100,
        "charging": true,
      });
    }, 30 * 1000);
  };

  /**
   * @returns ID
   */
  buildId(): number {
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
  }

  /**
   * 发送消息
   *
   * @param signal 信令
   * @param body   主体
   */
  send(signal: string, body: Record<string, Object>) {
    const header: Record<string, Object> = {
      "v"     : setting.version,
      "id"    : this.buildId(),
      "signal": signal
    };
    const message: Record<string, Object> = {
      "header": header,
      "body"  : body
    };
    try {
      hilog.debug(0x0000, "TaoyaoSignal", "发送消息：%o", message);
      this.socket?.send(JSON.stringify(message));
    } catch (error) {
      hilog.error(0x0000, "TaoyaoSignal", "发送消息异常：%o", message, error);
    }
  }

  /**
   * 请求消息
   *
   * @param signal 信令
   * @param body   主体
   *
   * @returns 响应
   */
  async request(signal: string, body: Record<string, Object>): Promise<Record<string, Object>> {
    return new Promise<Record<string, Object>>((resolve, reject) => {
      const id = this.buildId();
      const header: Record<string, Object> = {
        "v"     : setting.version,
        "id"    : id,
        "signal": signal
      };
      const message: Record<string, Object> = {
        "header": header,
        "body"  : body
      };
      // 设置超时
      const rejectTimeout = setTimeout(() => {
        this.callbackMapping.delete(id);
        reject("请求超时");
      }, 5000);
      // 请求回调
      this.callbackMapping.set(id, (response: Record<string, Object>) => {
        resolve(response);
        clearTimeout(rejectTimeout);
        // 默认不用继续处理
        return true;
      });
      // 发送消息
      try {
        hilog.debug(0x0000, "TaoyaoSignal", "发送请求：%o", message);
        this.socket?.send(JSON.stringify(message));
      } catch (error) {
        hilog.error(0x0000, "TaoyaoSignal", "发送消息异常：%o", message, error);
        reject(error);
      }
    });
  }

  /**
   * 处理消息
   *
   * @param message 消息
   */
  onMessage(message: string) {
    const json  : Record<string, Object> = JSON.parse(message);
    const header: Record<string, Object> = json.header as Record<string, Object>;
    const id    : number = header.id     as number;
    const signal: string = header.signal as string;
    if (this.callbackMapping.has(id)) {
      hilog.debug(0x0000, "TaoyaoSignal", "处理同步消息：%s", message);
      try {
        const callback = this.callbackMapping.get(id) as Function;
        if(callback(json)) {
          return;
        }
      } finally {
        this.callbackMapping.delete(id);
      }
    }
    let ret: number = 0;
    switch (signal) {
      case "room::close":
        ret = taoyaoModule.roomClose(message);
        break;
      case "room::enter":
        ret = taoyaoModule.roomEnter(message);
        break;
      case "room::expel":
        ret = taoyaoModule.roomExpel(message);
        break;
      case "room::invite":
        ret = taoyaoModule.roomInvite(message);
        break;
      case "room::leave":
        ret = taoyaoModule.roomLeave(message);
        break;
      case "room::client::list":
        ret = taoyaoModule.roomClientList(message);
        break;
      case "media::consume":
        ret = taoyaoModule.mediaConsume(message);
        break;
      case "media::consumer::close":
        ret = taoyaoModule.mediaConsumerClose(message);
        break;
      case "media::consumer::pause":
        ret = taoyaoModule.mediaConsumerPause(message);
        break;
      case "media::consumer::resume":
        ret = taoyaoModule.mediaConsumerResume(message);
        break;
      case "media::producer::close":
        ret = taoyaoModule.mediaProducerClose(message);
        break;
      case "media::producer::pause":
        ret = taoyaoModule.mediaProducerPause(message);
        break;
      case "media::producer::resume":
        ret = taoyaoModule.mediaProducerResume(message);
        break;
      default:
        ret = -1;
        hilog.warn(0x0000, "TaoyaoSignal", "没有适配信令：%s", signal);
        break;
    }
    hilog.debug(0x0000, "TaoyaoSignal", "处理异步消息：%d %s", ret, message);
  }

  /**
   * 注册发送消息
   *
   * @param signal 信令
   * @param body   主体
   */
  nativeSend(signal: string, body: string) {
    this.send(signal, JSON.parse(body));
  }

  /**
   * 注册请求消息
   *
   * @param signal 信令
   * @param body   主体
   *
   * @returns  响应
   */
  async nativeRequest(signal: string, body: string): Promise<string> {
    const response = await this.request(signal, JSON.parse(body));
    return JSON.stringify(response);
  }

}

const signal = new TaoyaoSignal();

// 加载系统
taoyaoModule.init(JSON.stringify(setting.signal));
// 注册回调
taoyaoModule.registerSend(signal.nativeSend);
taoyaoModule.registerRequest(signal.nativeRequest);

export {
  signal
}
