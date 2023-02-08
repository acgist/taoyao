/**
 * 日志
 */
import moment from "moment";
import { config } from "./Config.js";

/**
 * 日志
 */
class Logger {
  constructor(prefix = "") {
    if (prefix) {
      this.name = this.name + " : " + prefix;
    }
  }

  // 名称
  name = config.name;
  // 级别
  level = ["DEBUG", "INFO", "WARN", "ERROR", "OFF"];
  // 级别索引
  levelIndex = this.level.indexOf(config.logLevel.toUpperCase());

  /**
   * debug
   *
   * @param  {...any} args 参数
   *
   * @returns this
   */
  debug(...args) {
    return this.log(console.debug, "37m", "DEBUG", args);
  }

  /**
   * info
   *
   * @param  {...any} args 参数
   *
   * @returns this
   */
  info(...args) {
    return this.log(console.info, "32m", "INFO", args);
  }

  /**
   * warn
   *
   * @param  {...any} args 参数
   *
   * @returns this
   */
  warn(...args) {
    return this.log(console.warn, "33m", "WARN", args);
  }

  /**
   * error
   *
   * @param  {...any} args 参数
   *
   * @returns this
   */
  error(...args) {
    return this.log(console.error, "31m", "ERROR", args);
  }

  /**
   * 日志
   *
   * @param {*} out 输出
   * @param {*} color 颜色
   * @param {*} level 级别
   * @param {*} args 参数
   *
   * @returns this
   */
  log(out, color, level, args) {
    if (!args || this.level.indexOf(level) < this.levelIndex) {
      return this;
    }
    if (args.length > 1 && args[0].length > 0) {
      out(`\x1B[${color}${this.name} ${moment().format("yyyy-MM-DD HH:mm:ss")} : [${level.padEnd(5, " ")}] :\x1B[0m`, args);
    } else if (args.length === 1 && args[0].length > 0) {
      out(`\x1B[${color}${this.name} ${moment().format("yyyy-MM-DD HH:mm:ss")} : [${level.padEnd(5, " ")}] :\x1B[0m`, args);
    } else {
      // 其他情况直接输出换行
      out("");
    }
    return this;
  }
}

export { Logger };
