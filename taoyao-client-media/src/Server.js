#!/usr/bin/env node

const config = require("./Config");
const mediasoup = require("mediasoup");
const { Taoyao, signalChannel } = require("./Taoyao");

// 线程名称
process.title = config.name;
// 禁止校验无效证书
process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

// Mediasoup Worker列表
const mediasoupWorkers = [];
// 桃夭
const taoyao = new Taoyao(mediasoupWorkers);

/**
 * 创建Mediasoup Worker列表
 */
async function buildMediasoupWorkers() {
  // 监听事件
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
    worker.on("died", (error) => {
      console.warn("worker died：", worker.pid, error);
      setTimeout(() => process.exit(1), 2000);
    });
    worker.observer.on("close", () => {
      console.info("worker close：", worker.pid);
    });
    // worker.observer.on("newrouter", (router) => {
    //   console.info("worker newrouter：", worker.pid, router.id);
    // });
    // worker.observer.on("newwebrtcserver", (webRtcServer) => {
    //   console.info("worker newwebrtcserver：", worker.pid, webRtcServer.id);
    // });
    // webRtcServer.on("workerclose", () => {
    //   console.info("webRtcServer workerclose：", worker.pid, webRtcServer.id);
    // });
    // webRtcServer.observer.on("close", () => {
    //   console.info("webRtcServer close：", worker.pid, webRtcServer.id);
    // });
    // webRtcServer.observer.on("webrtctransporthandled", (webRtcTransport) => {
    //   console.info("webRtcServer webrtctransporthandled：", worker.pid, webRtcServer.id, webRtcTransport.id);
    // });
    // webRtcServer.observer.on("webrtctransportunhandled", (webRtcTransport) => {
    //   console.info("webRtcServer webrtctransportunhandled：", worker.pid, webRtcServer.id, webRtcTransport.id);
    // });
  }
}

/**
 * 连接信令服务
 */
async function connectSignalServer() {
  signalChannel.taoyao = taoyao;
  await signalChannel.connect(`wss://${config.signal.host}:${config.signal.port}/websocket.signal`);
}

/**
 * 启动方法
 */
async function main() {
  console.log(`
    桃之夭夭，灼灼其华。
    之子于归，宜其室家。

    :: https://gitee.com/acgist/taoyao
  `);
  console.info("开始启动：", config.name);
  await buildMediasoupWorkers();
  await connectSignalServer();
  console.info("启动完成：", config.name);
}

main();
