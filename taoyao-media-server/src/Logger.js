/**
 * 日志
 */
const moment = require('moment')
const config = require("./Config");

class Logger {

  // 名称
  name = config.name;
  // 级别
  level = [ "DEBUG", "INFO", "WARN", "ERROR", "OFF" ];
  // 级别索引
  levelIndex = this.level.indexOf(config.logLevel.toUpperCase());

  constructor(prefix) {
    if (prefix) {
      this.name = this.name + ':' + prefix;
    }
  }

  debug(...args) {
    return this.log(console.debug, '37m', 'DEBUG', args);
  }
  
  info(...args) {
    return this.log(console.info, '32m', 'INFO', args);
  }

  warn(...args) {
    return this.log(console.warn, '33m', 'WARN', args);
  }

  error(...args) {
    return this.log(console.error, '31m', 'ERROR', args);
  }

  log(out, color, level, args) {
    if(!args || this.level.indexOf(level) < this.levelIndex) {
      return this;
    }
    if(args.length > 1 && args[0].length > 0) {
      out(`\x1B[${color}${this.name} ${moment().format('yyyy-MM-DD HH:mm:ss')} : [${level.padEnd(5, ' ')}] : ${args[0]}\x1B[0m`, ...args.slice(1));
    } else if(args.length === 1 && args[0].length > 0) {
      out(`\x1B[${color}${this.name} ${moment().format('yyyy-MM-DD HH:mm:ss')} : [${level.padEnd(5, ' ')}] : ${args[0]}\x1B[0m`);
    } else {
      out("");
    }
    return this;
  }

}

module.exports = Logger;
