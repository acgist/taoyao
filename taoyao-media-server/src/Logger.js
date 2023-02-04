/**
 * 日志
 */
const config = require("./Config");

class Logger {

  // 
  name = config.name;

  constructor(prefix) {
    if (prefix) {
      this.name = this.name + ':' + prefix;
    }
  }

  debug(...args) {
    this.log(console.debug, 'DEBUG', args);
  }
  
  info(...args) {
    this.log(console.info, 'INFO', args);
  }

  warn(...args) {
    this.log(console.warn, 'WARN', args);
  }

  error(...args) {
    this.log(console.error, 'ERROR', args);
  }

  log(out, level, args) {
    if(!args) {
      return;
    }
    if(args.length > 1 && args[0].length > 0) {
      out(`${this.name}:${level}:${args[0]}`, ...args.slice(1));
    } else if(args.length === 1 && args[0].length > 0) {
      out(`${this.name}:${level}:${args[0]}`);
    } else {
      out("");
    }
  }

}

module.exports = Logger;
