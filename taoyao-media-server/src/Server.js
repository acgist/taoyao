#!/usr/bin/env node

const fs = require("fs");
const ws = require("ws");
const https = require("https");
const mediasoup = require("mediasoup");
const config = require("./Config");
const Signal = require("./Signal");

// 线程名称
process.title = config.name;

// 无效信令终端列表
const clients = [];
// Mediasoup Worker列表
const mediasoupWorkers = [];

// HTTPS server
let httpsServer;
// WebSocket server
let webSocketServer;
// 信令
const signal = new Signal(mediasoupWorkers);

/**
 * 启动Mediasoup Worker
 */
async function buildMediasoupWorkers() {
  const { workerSize } = config.mediasoup;
  console.info("启动Worker", workerSize);
  for (let index = 0; index < workerSize; index++) {
    // 新建Worker
    const worker = await mediasoup.createWorker({
      logLevel: config.mediasoup.workerSettings.logLevel,
      logTags: config.mediasoup.workerSettings.logTags,
      rtcMinPort: Number(config.mediasoup.workerSettings.rtcMinPort),
      rtcMaxPort: Number(config.mediasoup.workerSettings.rtcMaxPort),
    });
    // 监听Worker事件
    worker.on("died", () => {
      console.warn("Worker停止服务", worker.pid);
      setTimeout(() => process.exit(1), 2000);
    });
    worker.observer.on("close", () => {
      console.warn("Worker关闭服务", worker.pid);
    });
    // 配置WebRTC服务
    const webRtcServerOptions = JSON.parse(
      JSON.stringify(config.mediasoup.webRtcServerOptions)
    );
    for (const listenInfo of webRtcServerOptions.listenInfos) {
      listenInfo.port = listenInfo.port + mediasoupWorkers.length - 1;
    }
    const webRtcServer = await worker.createWebRtcServer(webRtcServerOptions);
    worker.appData.webRtcServer = webRtcServer;
    // 加入Worker队列
    mediasoupWorkers.push(worker);
  }
}

/**
 * 启动信令服务
 */
async function buildSignalServer() {
  const tls = {
    cert: fs.readFileSync(config.https.tls.cert),
    key: fs.readFileSync(config.https.tls.key),
  };
  // 配置HTTPS Server
  httpsServer = https.createServer(tls, (request, response) => {
    response.writeHead(200);
    response.end(fs.readFileSync(config.welcome));
  });
  // 配置WebSocket Server
  webSocketServer = new ws.Server({ server: httpsServer });
  webSocketServer.on("connection", (session) => {
    console.info("打开信令通道", session._socket.remoteAddress);
    session.datetime = Date.now();
    session.authorize = false;
    clients.push(session);
    session.on("close", (code) => {
      console.info("关闭信令通道", session._socket.remoteAddress, code);
    });
    session.on("error", (error) => {
      console.error("信令通道异常", session._socket.remoteAddress, error);
    });
    session.on("message", (message) => {
      console.debug("处理信令消息", message.toString());
      try {
        signal.on(JSON.parse(message), session);
      } catch (error) {
        console.error("处理信令消息异常", message.toString(), error);
      }
    });
  });
  // 打开监听
  httpsServer.listen(
    Number(config.https.listenPort),
    config.https.listenIp,
    () => {
      console.info("信令服务启动完成");
    }
  );
}

/**
 * 定时任务
 */
async function buildInterval() {
  // 定时打印使用情况
  setInterval(async () => {
    signal.usage();
  }, 300 * 1000);
  // 定时清理过期无效终端
  setInterval(() => {
    let failSize = 0;
    let silentSize = 0;
    let successSize = 0;
    const datetime = Date.now();
    for (let index = 0; index < clients.length; index++) {
      const session = clients[index];
      if (session.authorize) {
        clients.splice(index, 1);
        successSize++;
        index--;
      } else if (datetime - session.datetime >= 5000) {
        clients.splice(index, 1);
        session.close();
        failSize++;
        index--;
      } else {
        silentSize++;
      }
    }
    console.info("定时清理无效信令终端（无效|静默|成功|现存）", failSize, silentSize, successSize, clients.length);
  }, 60 * 1000);
}

/**
 * 启动方法
 */
async function main() {
  console.info("桃之夭夭，灼灼其华。")
  console.info("之子于归，宜其室家。")
  console.info("开始启动", config.name);
  await buildMediasoupWorkers();
  await buildSignalServer();
  await buildInterval();
  console.info("启动完成", config.name);
}

// 启动服务
main();
