const Logger = require("./Logger");

const logger = new Logger();

function openCommandConsole() {
  logger.info("打开交互式控制台...");
  process.stdin.resume();
  process.stdin.setEncoding("utf-8");
  process.stdin.on("data", (data) => {
    process.stdin.pause();
    const command = data.replace(/^\s\s*/, "").replace(/\s\s*$/, "");
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
      }
    }
    logger.info("");
    process.stdin.resume();
  });
}

module.exports = async function () {
  try {
    openCommandConsole();
  } catch (error) {
    logger.error("执行命令异常：%o", error);
  }
};
