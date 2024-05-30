/**
 * 配置
 *
 * @author acgist
 */

class Config {

  // 终端名称
  name       : string = "鸿蒙";
  // 终端ID
  clientId   : string = "harmony";
  // 终端类型
  clientType : string = "MOBILE";
  // 信令账号
  username   : string = "taoyao";
  // 信令密码
  password   : string = "taoyao";
  // 当前终端索引
  clientIndex: number = 99999;

};

class Setting {

  // 信令配置
  config : Config = new Config();
  // CA证书
  caPath : string = "/cacert.pem";
  // 信令版本
  version: string = "1.0.0";
  // 信令地址
  signalAddress: string = "wss://192.168.8.60:8888/websocket.signal";
  // signalAddress: string = "wss://192.168.1.100:8888/websocket.signal";
  // 启动自动加载系统
  initOnLoad   : boolean = true;

};

const setting = new Setting();

export {
  setting
}
