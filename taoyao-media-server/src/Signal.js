/**
 * 信令
 * 1. 终端媒体流向
 * 2. 处理音频视频：降噪、水印等等
 */
class Signal {

  // Mediasoup Worker列表
  mediasoupWorkers = [];
  // Mediasoup Worker下个索引
  nextMediasoupWorkerIndex = 0;

  constructor(mediasoupWorkers) {
    this.mediasoupWorkers = mediasoupWorkers;
  }

  /**
   * 处理事件
   *
   * @param {*} message 消息
   * @param {*} session websocket
   */
  on(message, session) {}
}

module.exports = Signal;
