/**
 * 配置
 *
 * @author acgist
 */

class Signal {
  // 终端名称
  name: string = "鸿蒙";
  // 终端ID
  clientId: string = "harmony";
  // 终端类型
  clientType: string = "MOBILE";
  // 信令账号
  username: string = "taoyao";
  // 信令密码
  password: string = "taoyao";
};

class Setting {

  // 信令地址
  signalAddress: string = "wss://192.168.8.204:8888/websocket.signal";
  // 信令版本
  version: string = "1.0.0";
  // 信令配置
  signal: Signal = new Signal();

};

const setting = new Setting();

export {
  setting
}
