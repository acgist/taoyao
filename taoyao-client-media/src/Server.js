#!/usr/bin/env node

const config = require("./Config");
const mediasoup = require("mediasoup");
const { Signal, signalChannel } = require("./Signal");

// 线程名称
process.title = config.name;
// 禁止校验无效证书
process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

// Mediasoup Worker列表
const mediasoupWorkers = [];
// 信令服务
const signal = new Signal(mediasoupWorkers);

/**
 * 创建Mediasoup Worker列表
 */
async function buildMediasoupWorkers() {
  // 可配置的事件
  // mediasoup.observer.on("newworker", fn(worker));
  const { workerSize } = config.mediasoup;
  console.info("创建Mediasoup Worker数量：", workerSize);
  for (let index = 0; index < workerSize; index++) {
    const worker = await mediasoup.createWorker({
      logTags: config.mediasoup.workerSettings.logTags,
      logLevel: config.mediasoup.workerSettings.logLevel,
      rtcMinPort: Number(config.mediasoup.workerSettings.rtcMinPort),
      rtcMaxPort: Number(config.mediasoup.workerSettings.rtcMaxPort),
    });
    worker.on("died", (error) => {
      console.warn("Mediasoup Worker停止服务：", worker.pid, error);
      setTimeout(() => process.exit(1), 2000);
    });
    worker.observer.on("close", () => {
      console.warn("Mediasoup Worker关闭服务：", worker.pid);
    });
    // 可配置的事件
    // worker.observer.on("newrouter", fn(router));
    // worker.observer.on("newwebrtcserver", fn(router));
    // 配置WebRTC服务
    const webRtcServerOptions = JSON.parse(
      JSON.stringify(config.mediasoup.webRtcServerOptions)
    );
    for (const listenInfo of webRtcServerOptions.listenInfos) {
      listenInfo.port = listenInfo.port + mediasoupWorkers.length;
    }
    const webRtcServer = await worker.createWebRtcServer(webRtcServerOptions);
    worker.appData.webRtcServer = webRtcServer;
    mediasoupWorkers.push(worker);
  }
}

/**
 * 连接信令服务
 */
async function connectSignalServer() {
  await signalChannel.connect(
    `wss://${config.signal.host}:${config.signal.port}/websocket.signal`,
    function (message) {
      signal.on(message);
    }
  );
}

/**
 * 启动方法
 */
async function main() {
  console.log(`
    桃之夭夭，灼灼其华。
    之子于归，宜其室家。
  `);
  console.info("开始启动：", config.name);
  await buildMediasoupWorkers();
  await connectSignalServer();
  console.info("启动完成：", config.name);
}

main();
