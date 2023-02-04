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

// HTTPS server
let httpsServer;
// WebSocket server
let webSocketServer;
// 日志
const logger = new Logger();
// 信令
const signal = new Signal();
// 无效信令终端列表
const client = [];
// Mediasoup Worker列表
const mediasoupWorker = [];

process.title = config.name;

/**
 * 启动Mediasoup Worker
 */
async function buildMediasoupWorker() {
  const { numWorkers } = config.mediasoup;
  logger.info("启动Mediasoup Worker（%d）...", numWorkers);
  for (let i = 0; i < numWorkers; i++) {
    // 新建Worker
    const worker = await mediasoup.createWorker({
      logLevel: config.mediasoup.workerSettings.logLevel,
      logTags: config.mediasoup.workerSettings.logTags,
      rtcMinPort: Number(config.mediasoup.workerSettings.rtcMinPort),
      rtcMaxPort: Number(config.mediasoup.workerSettings.rtcMaxPort),
    });
    // 监听Worker事件
    worker.on("died", () => {
      logger.error(
        "Mediasoup Worker停止服务（两秒之后自动退出）... [PID：%d]",
        worker.pid
      );
      setTimeout(() => process.exit(1), 2000);
    });
    // 加入Worker队列
    mediasoupWorker.push(worker);
    // 配置WebRTC服务
    if (process.env.MEDIASOUP_USE_WEBRTC_SERVER !== "false") {
      // 配置Worker端口
      const portIncrement = mediasoupWorker.length - 1;
      const webRtcServerOptions = JSON.parse(JSON.stringify(config.mediasoup.webRtcServerOptions));
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
      logger.info("Mediasoup Worker使用情况 [pid：%d]: %o", worker.pid, usage);
    }, 120 * 1000);
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
  logger.info("配置HTTPS服务...");
  httpsServer = https.createServer(tls, (request, response) => {
    response.writeHead(200);
    response.end(config.wellcome);
  });
  logger.info("配置WebSocket服务...");
  webSocketServer = new ws.Server({ server: httpsServer });
  webSocketServer.on("connection", (session) => {
    logger.info("打开信令通道: %s", session._socket.remoteAddress);
    session.datetime = new Date().getTime();
    session.authorize = false;
    client.push(session);
    session.on("close", (code) => {
      logger.info("关闭信令通道: %o", code);
    });
    session.on("error", (error) => {
      logger.error("信令通道异常: %o", error);
    });
    session.on("message", (message) => {
      onmessage(message, session);
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
 * 交互式控制台
 */
 async function buildCommandConsole() {
  if(!config.command) {
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
        logger.info("查询命令：`h` | `help`");
        break;
      }
    }
    logger.info("");
    process.stdin.resume();
  });
}

/**
 * 定时清理无效信令终端
 */
async function buildClientInterval() {
  setInterval(() => {
    const datetime = new Date().getTime();
    const oldLength = client.length;
    for (let i = 0; i < client.length; i++) {
      const session = client[i];
      // 超过五秒自动关闭
      if (datetime - session.datetime >= 5000) {
        client.splice(i, 1);
        session.close();
        i--;
      }
    }
    const newLength = client.length;
    logger.info("定时清理无效信令终端：%d", oldLength - newLength);
  }, 60 * 1000);
}

/**
 * 处理信令消息
 *
 * @param {*} message 消息
 * @param {*} session websocket
 */
async function onmessage(message, session) {
  try {
    const data = JSON.parse(message);
    // 授权验证
    if (!session.authorize) {
      if (
        data.username === config.https.username &&
        data.password === config.https.password
      ) {
        logger.debug("授权成功：%s", session._socket.remoteAddress);
        session.authorize = true;
      } else {
        logger.warn("授权失败：%s", session._socket.remoteAddress);
        session.close();
      }
      for (let i = 0; i < client.length; i++) {
        if (client[i] === session) {
          client.splice(i, 1);
          break;
        }
      }
      return;
    }
    // 处理信令
    logger.debug("处理信令消息: %s", message);
    signal.on(data, session);
  } catch (error) {
    logger.error(
      `处理信令消息异常：
      %s
      %o`,
      message,
      error
    );
  }
}

/**
 * 启动方法
 */
async function main() {
  logger.debug("DEBUG").info("INFO").warn("WARN").error("ERROR");
  logger.info("开始启动：%s", config.name);
  await buildMediasoupWorker();
  await buildSignalServer();
  await buildCommandConsole();
  await buildClientInterval();
  logger.info("启动完成：%s", config.name);
}

// 启动
main();
