#!/usr/bin/env node

const config    = require("./Config");
const mediasoup = require("mediasoup");
const { Taoyao, signalChannel } = require("./Taoyao");

// 线程名称
process.title = config.name;

// 工作线程
const mediasoupWorkers = [];
// 桃夭信令
const taoyao = new Taoyao(mediasoupWorkers);

/**
 * 创建工作线程
 */
async function buildMediasoupWorkers() {
  mediasoup.observer.on("newworker", (worker) => {
    console.info("mediasoup newworker", worker.pid);
  });
  const { workerSize } = config.mediasoup;
  console.info("工作线程数量", workerSize);
  for (let index = 0; index < workerSize; index++) {
    // 创建工作线程
    const worker = await mediasoup.createWorker({
      logTags   : config.mediasoup.workerSettings.logTags,
      logLevel  : config.mediasoup.workerSettings.logLevel,
      rtcMinPort: Number(config.mediasoup.workerSettings.rtcMinPort),
      rtcMaxPort: Number(config.mediasoup.workerSettings.rtcMaxPort),
    });
    // 监听事件
    worker.on("died", (error) => {
      // 正常情况不会出现
      console.error("worker died", worker.pid, error);
    });
    worker.observer.on("close", () => {
      console.debug("worker close", worker.pid);
    });
    worker.observer.on("newrouter", (router) => {
      console.debug("worker newrouter", worker.pid, router.id);
    });
    worker.observer.on("newwebrtcserver", (webRtcServer) => {
      console.debug("worker newwebrtcserver", worker.pid, webRtcServer.id);
    });
    // 创建WebRTC服务
    const webRtcServerOptions = JSON.parse(JSON.stringify(config.mediasoup.webRtcServerOptions));
    for (const listenInfos of webRtcServerOptions.listenInfos) {
      listenInfos.port = Number(listenInfos.port) + mediasoupWorkers.length;
    }
    const webRtcServer = await worker.createWebRtcServer(webRtcServerOptions);
    // 监听事件
    webRtcServer.on("workerclose", () => {
      console.debug("webRtcServer workerclose", worker.pid, webRtcServer.id);
    });
    webRtcServer.observer.on("close", () => {
      console.debug("webRtcServer close", worker.pid, webRtcServer.id);
    });
    webRtcServer.observer.on("webrtctransporthandled", (webRtcTransport) => {
      console.debug("webRtcServer webrtctransporthandled", worker.pid, webRtcServer.id, webRtcTransport.id);
    });
    webRtcServer.observer.on("webrtctransportunhandled", (webRtcTransport) => {
      console.debug("webRtcServer webrtctransportunhandled", worker.pid, webRtcServer.id, webRtcTransport.id);
    });
    // 配置WebRTC服务
    worker.appData.webRtcServer = webRtcServer;
    // 添加工作线程
    mediasoupWorkers.push(worker);
  }
}

/**
 * 连接信令
 */
async function connectSignalServer() {
  signalChannel.taoyao = taoyao;
  await signalChannel.connect(`wss://${config.signal.host}:${config.signal.port}/websocket.signal`);
}

/**
 * 启动方法
 */
async function main() {
  console.debug(`
    桃之夭夭，灼灼其华。
    之子于归，宜其室家。

    :: https://gitee.com/acgist/taoyao
  `);
  console.info("开始启动", config.name);
  await buildMediasoupWorkers();
  await connectSignalServer();
  console.info("启动完成", config.name);
}

main();
