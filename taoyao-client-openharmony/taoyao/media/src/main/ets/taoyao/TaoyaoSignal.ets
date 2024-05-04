/**
 * 信令连接
 *
 * @author acgist
 */

import hilog from '@ohos.hilog';
import webSocket from '@ohos.net.webSocket';

import { setting } from './Setting';
import libtaoyao from 'libtaoyao.so'

interface BusinessError<T = void> extends Error {
  code: number;
  data?: T;
}

class TaoyaoSignal {

  // // WebSocket信令连接
  // socket: webSocket.WebSocket;
  // // 是否关闭
  // closed: boolean = false;
  // // 是否连接成功
  // connected: boolean = false;
  // // 心跳定时
  // heartbeatTimer: number;
  // // 同步请求
  // callbackMapping = new Map();
  // // 当前消息索引
  // index: number = 0;
  // // 最大消息索引
  // maxIndex: number = 999;
  // // 当前终端索引
  // clientIndex: number = 99999;
  //
  // connect() {
  //   if(this.socket) {
  //     this.socket.close();
  //   }
  //   this.connected = false;
  //   hilog.info(0x0000, 'TaoyaoSignal', '连接信令：%s', setting.signalAddress);
  //   this.socket = webSocket.createWebSocket();
  //   this.socket.on('open', (err: BusinessError, value: Object) => {
  //     hilog.info(0x0000, 'TaoyaoSignal', '打开信令：%s', setting.signalAddress);
  //     this.register();
  //     this.connected = true;
  //   });
  //   this.socket.on('message', (err: BusinessError, value: string | ArrayBuffer) => {
  //     hilog.debug(0x0000, 'TaoyaoSignal', '信令消息：%s', value);
  //     try {
  //       this.onMessage(value);
  //     } catch (error) {
  //       hilog.error(0x0000, 'TaoyaoSignal', '处理信令消息异常：%s', value, error);
  //     }
  //   });
  //   this.socket.on('close', (err: BusinessError, value: Object) => {
  //     hilog.error(0x0000, 'TaoyaoSignal', '关闭信令：%s', setting.signalAddress, err);
  //     this.reconnect();
  //   });
  //   this.socket.on('error', (err: BusinessError) => {
  //     hilog.error(0x0000, 'TaoyaoSignal', '信令异常：%s', setting.signalAddress, err);
  //     this.reconnect();
  //   });
  //   this.socket.connect(setting.signalAddress, (err: BusinessError, value: boolean) => {
  //     hilog.info(0x0000, 'TaoyaoSignal', '信令连接成功：%s', setting.signalAddress);
  //   });
  // };
  //
  // reconnect() {
  //   if(!this.closed) {
  //     setTimeout(() => this.connect(), 5000);
  //   }
  // }
  //
  // close() {
  //   hilog.info(0x0000, 'TaoyaoSignal', '关闭信令：%s', setting.signalAddress);
  //   this.closed = true;
  //   this.connected = false;
  //   if(this.socket) {
  //     this.socket.close();
  //   }
  // }
  //
  // async register() {
  //   const response: any = await this.request("client::register", {
  //       name      : setting.signal.name,
  //       clientId  : setting.signal.clientId,
  //       clientType: setting.signal.clientType,
  //       username  : setting.signal.username,
  //       password  : setting.signal.password,
  //       battery   : 100,
  //       charging  : true,
  //     });
  //   const { body } = response;
  //   const { index } = body;
  //   this.clientIndex = index;
  //   hilog.info(0x0000, "TaoyaoSignal", "信令注册成功：%d", index);
  //   this.heartbeat();
  // }
  //
  // heartbeat() {
  //   if(this.heartbeatTimer) {
  //     clearInterval(this.heartbeatTimer);
  //   }
  //   this.heartbeatTimer = setInterval(() => {
  //     this.send("client::heartbeat", {
  //       battery : 100,
  //       charging: true,
  //     });
  //   }, 30 * 1000);
  // };
  //
  // buildId() {
  //   if (++this.index > this.maxIndex) {
  //     this.index = 0;
  //   }
  //   const date = new Date();
  //   return (
  //     100000000000000 * date.getDate()    +
  //     1000000000000   * date.getHours()   +
  //     10000000000     * date.getMinutes() +
  //     100000000       * date.getSeconds() +
  //     1000            * this.clientIndex  +
  //     this.index
  //   );
  // }
  //
  // send(signal, body) {
  //   const header = {
  //     v: setting.version,
  //     id: this.buildId(),
  //     signal
  //   };
  //   const message = {
  //     header,
  //     body
  //   };
  //   try {
  //     this.socket.send(JSON.stringify(message));
  //   } catch (error) {
  //     hilog.error(0x0000, 'TaoyaoSignal', '发送消息异常：%o', message, error);
  //   }
  // }
  //
  // async request(signal, body) {
  //   const id = this.buildId();
  //   return new Promise((resolve, reject) => {
  //     const header = {
  //       v: setting.version,
  //       id,
  //       signal
  //     };
  //     const message = {
  //       header,
  //       body
  //     };
  //     // 设置超时
  //     const rejectTimeout = setTimeout(() => {
  //       this.callbackMapping.delete(id);
  //       reject("请求超时");
  //     }, 5000);
  //     // 请求回调
  //     this.callbackMapping.set(id, (response) => {
  //       resolve(response);
  //       clearTimeout(rejectTimeout);
  //       // 默认不用继续处理
  //       return true;
  //     });
  //     // 发送消息
  //     try {
  //       this.socket.send(JSON.stringify(message));
  //     } catch (error) {
  //       hilog.error(0x0000, 'TaoyaoSignal', '发送消息异常：%o', message, error);
  //       reject(error);
  //     }
  //   });
  // }
  //
  // onMessage(message) {
  //   const json = JSON.parse(message);
  //   const {
  //     header,
  //     body,
  //   } = json;
  //   const {
  //     id
  //   } = header;
  //   if (this.callbackMapping.has(id)) {
  //     hilog.info(0x0000, "TaoyaoSignal", "处理同步消息：%s", message);
  //     try {
  //       if(
  //         this.callbackMapping.get(id)(json)
  //       ) {
  //         return;
  //       }
  //     } finally {
  //       this.callbackMapping.delete(id);
  //     }
  //   }
  //   hilog.info(0x0000, "TaoyaoSignal", "处理异步消息：%s", message);
  //   // TODO
  // }

}

const signal = new TaoyaoSignal();

export {
  signal
}
