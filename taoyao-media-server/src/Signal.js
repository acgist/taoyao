const Logger = require("./Logger");

/**
 * 信令
 */
class Signal {

  // 信令终端列表
  client = [];
  // 日志
  logger = new Logger();
  // Mediasoup Worker列表
  mediasoupWorker = [];
  // Mediasoup Worker索引
  nextMediasoupWorkerIndex = 0;

  constructor(mediasoupWorker) {
    this.mediasoupWorker = mediasoupWorker;
  }

  /**
   * 处理事件
   *
   * @param {*} message 消息
   * @param {*} session websocket
   */
  on(message, session) {
    // 授权验证
    if (!session.authorize) {
      if (
        message?.header?.signal === "media::register" &&
        message?.body?.username === config.https.username &&
        message?.body?.password === config.https.password
      ) {
        this.logger.debug("授权成功", session._socket.remoteAddress);
        client.push(session);
        session.authorize = true;
        message.code = "0000";
        message.message = "授权成功";
        message.body.username = null;
        message.body.password = null;
      } else {
        this.logger.warn("授权失败", session._socket.remoteAddress);
        message.code = "3401";
        message.message = "授权失败";
      }
      this.push(message, session);
      return;
    }
    // 处理信令
    switch (message.header.signal) {
    }
  }

  /**
   * 通知信令
   *
   * @param {*} message 消息
   * @param {*} session websocket
   */
  push(message, session) {
    try {
      session.send(JSON.stringify(message));
    } catch (error) {
      logger.error(
        "通知信令失败",
        session._socket.remoteAddress,
        message,
        error
      );
    }
  }

  /**
   * 通知信令
   *
   * @param {*} message 消息
   */
  push(message) {
    this.client.forEach((session) => this.push(message, session));
  }
  
}

module.exports = Signal;
