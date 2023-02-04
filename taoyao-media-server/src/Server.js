#!/usr/bin/env node
/**
 * 服务
 */
const fs = require("fs");
const ws = require("ws");
const https = require("https");
const mediasoup = require("mediasoup");
const config = require("./Config");
const Logger = require("./Logger");
const Signal = require("./Signal");
const command = require("./Command");

// 日志
const logger = new Logger();
const signal = new Signal();
// HTTPS server
let httpsServer;
// WebSocket server
let webSocketServer;
// Mediasoup Worker列表
const mediasoupWorkers = [];
// Mediasoup Worker下个索引
let nextMediasoupWorkerIndex = 0;

process.title = config.name;
process.env.DEBUG = process.env.DEBUG || "*mediasoup* *INFO* *WARN* *ERROR*";
logger.info("开始启动：%s", config.name);

run();

async function run() {
  // 启动Mediasoup服务
  await runMediasoupWorkers();
  // 启动服务
  await runSignalServer();
  logger.info("启动完成：%s", config.name);
  // 交互式命令行
  if (config.command) {
    await command();
  }
}

async function runMediasoupWorkers() {
  const { numWorkers } = config.mediasoup;
  logger.info("启动Mediasoup服务（%d Worker）...", numWorkers);
  for (let i = 0; i < numWorkers; i++) {
    const worker = await mediasoup.createWorker({
      logLevel: config.mediasoup.workerSettings.logLevel,
      logTags: config.mediasoup.workerSettings.logTags,
      rtcMinPort: Number(config.mediasoup.workerSettings.rtcMinPort),
      rtcMaxPort: Number(config.mediasoup.workerSettings.rtcMaxPort),
    });
    worker.on("died", () => {
      logger.error(
        "Mediasoup Worker停止服务（两秒之后自动退出）... [PID：%d]",
        worker.pid
      );
      setTimeout(() => process.exit(1), 2000);
    });
    mediasoupWorkers.push(worker);
    // 配置WebRTC服务
    if (process.env.MEDIASOUP_USE_WEBRTC_SERVER !== "false") {
      // 每个Worker端口不能相同
      const portIncrement = mediasoupWorkers.length - 1;
      const webRtcServerOptions = JSON.parse(JSON.stringify(config.mediasoup.webRtcServerOptions));
      for (const listenInfo of webRtcServerOptions.listenInfos) {
        listenInfo.port += portIncrement;
      }
      const webRtcServer = await worker.createWebRtcServer(webRtcServerOptions);
      worker.appData.webRtcServer = webRtcServer;
    }
    // 记录日志
    setInterval(async () => {
      const usage = await worker.getResourceUsage();
      logger.info(
        "Mediasoup Worker使用情况 [pid：%d]: %o",
        worker.pid,
        usage
      );
    }, 120 * 1000);
  }
}

async function runSignalServer() {
  const tls = {
    cert: fs.readFileSync(config.https.tls.cert),
    key: fs.readFileSync(config.https.tls.key),
  };
  logger.info("配置HTTPS服务...");
  httpsServer = https.createServer(tls, (request, response) => {
    response.writeHead(200);
    response.end("taoyao media server");
  });
  logger.info("配置WebSocket服务...");
  webSocketServer = new ws.Server({ server: httpsServer });
  webSocketServer.on("connection", (session) => {
    session.on("open", (message) => {
      logger.info("打开信令通道: %s", message);
    });
    session.on("close", (code) => {
      logger.info("关闭信令通道: %o", code);
    });
    session.on("error", (e) => {
      logger.error("信令通道异常: %o", e);
    });
    session.on("message", (message) => {
      logger.debug("收到信令消息: %s", message);
      try {
        signal.on(JSON.parse(message), session);
      } catch (error) {
        logger.error(
          `处理信令消息异常：
          %s
          %o`,
          message,
          error
        );
      }
    });
  });
  logger.info("开启服务监听...");
  await new Promise((resolve) => {
    httpsServer.listen(
      Number(config.https.listenPort),
      config.https.listenIp,
      resolve
    );
  });
}
