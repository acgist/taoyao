#!/usr/bin/env node

const fs = require("fs");
const ws = require("ws");
const https = require("https");
// const mediasoup = require("mediasoup");
const config = require("./Config");
const Logger = require("./Logger");
const Signal = require("./Signal");
const { fail } = require("assert");

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
// 日志
const logger = new Logger();
// 信令
const signal = new Signal(mediasoupWorkers);

/**
 * 启动Mediasoup Worker
 */
async function buildMediasoupWorkers() {
  const { workerSize } = config.mediasoup;
  logger.info("启动Mediasoup Worker", workerSize);
  for (let i = 0; i < workerSize; i++) {
    // 新建Worker
    const worker = await mediasoup.createWorker({
      logLevel: config.mediasoup.workerSettings.logLevel,
      logTags: config.mediasoup.workerSettings.logTags,
      rtcMinPort: Number(config.mediasoup.workerSettings.rtcMinPort),
      rtcMaxPort: Number(config.mediasoup.workerSettings.rtcMaxPort),
    });
    // 监听Worker事件
    worker.on("died", () => {
      logger.warn("Mediasoup Worker停止服务", worker.pid);
      setTimeout(() => process.exit(1), 2000);
    });
    // 加入Worker队列
    mediasoupWorkers.push(worker);
    // 配置WebRTC服务
    if (process.env.MEDIASOUP_USE_WEBRTC_SERVER !== "false") {
      // 配置Worker端口
      const portIncrement = mediasoupWorkers.length - 1;
      const webRtcServerOptions = JSON.parse(
        JSON.stringify(config.mediasoup.webRtcServerOptions)
      );
      for (const listenInfo of webRtcServerOptions.listenInfos) {
        listenInfo.port += portIncrement;
      }
      // 配置WebRTC服务
      const webRtcServer = await worker.createWebRtcServer(webRtcServerOptions);
      worker.appData.webRtcServer = webRtcServer;
    }
    // 定时记录使用日志
    setInterval(async () => {
      const usage = await worker.getResourceUsage();
      logger.info("Mediasoup Worker使用情况", worker.pid, usage);
    }, 120 * 1000);
  }
}

/**
 * 启动信令服务
 */
async function buildSignalServer() {
  const tls = {
    cert: fs.readFileSync(config.https.tls.cert),
    key:  fs.readFileSync(config.https.tls.key),
  };
  // 配置HTTPS
  httpsServer = https.createServer(tls, (request, response) => {
    response.writeHead(200);
    response.end(fs.readFileSync(config.welcome));
  });
  // 配置WebSocket
  webSocketServer = new ws.Server({ server: httpsServer });
  webSocketServer.on("connection", (session) => {
    logger.info("打开信令通道", session._socket.remoteAddress);
    session.datetime = Date.now();
    session.authorize = false;
    clients.push(session);
    session.on("close", (code) => {
      logger.info("关闭信令通道", session._socket.remoteAddress, code);
    });
    session.on("error", (error) => {
      logger.error("信令通道异常", session._socket.remoteAddress, error);
    });
    session.on("message", (message) => {
      logger.debug("处理信令消息", message.toString());
      try {
        signal.on(JSON.parse(message), session);
      } catch (error) {
        logger.error("处理信令消息异常", message.toString(), error);
      }
    });
  });
  // 打开监听
  httpsServer.listen(
    Number(config.https.listenPort),
    config.https.listenIp,
    () => {
      logger.info("信令服务启动完成");
    }
  );
}

/**
 * 定时清理无效信令终端
 */
async function buildClientInterval() {
  setInterval(() => {
    const datetime = Date.now();
    let failSize = 0;
    let successSize = 0;
    for (let i = 0; i < clients.length; i++) {
      const session = clients[i];
      // 超过五秒自动关闭
      if (datetime - session.datetime >= 5000) {
        clients.splice(i, 1);
        if(session.authorize) {
          successSize++;
        } else {
          failSize++;
          session.close();
        }
        i--;
      }
    }
    logger.info("定时清理无效信令终端", failSize, successSize, clients.length);
  }, 60 * 1000);
}

/**
 * 交互式控制台
 */
async function buildCommandConsole() {
  if (!config.command) {
    return;
  }
  process.stdin.setEncoding("UTF-8");
  process.stdin.resume();
  process.stdin.on("data", (data) => {
    process.stdin.pause();
    const command = data.replace(/^\s*/, "").replace(/\s*$/, "");
    logger.info("");
    switch (command) {
      case "h":
      case "help": {
        logger.info("- h,  help                    ： 帮助信息");
        logger.info("- os                          ： 系统信息");
        break;
      }
      case "":
      default: {
        logger.warn(`未知命令：'${command}'`);
        logger.info("查询命令：'h' | 'help'");
        break;
      }
    }
    logger.info("");
    process.stdin.resume();
  });
}

/**
 * 启动方法
 */
async function main() {
  logger.debug("闹市早行客");
  logger.info("江边独钓翁");
  logger.warn("山中与谁同");
  logger.error("绿竹细雨风");
  logger.info("开始启动", config.name);
  // await buildMediasoupWorkers();
  await buildSignalServer();
  await buildClientInterval();
  await buildCommandConsole();
  logger.info("启动完成", config.name);
}

// 启动服务
main();
