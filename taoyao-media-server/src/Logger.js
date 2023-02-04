/**
 * 日志
 */
const debug = require("debug");
const config = require("./Config");

class Logger {

  constructor(prefix) {
    const appName = config.name;
    if (prefix) {
      this._debug = debug(`${appName}:DEBUG:${prefix}`);
      this._info = debug(`${appName}:INFO:${prefix}`);
      this._warn = debug(`${appName}:WARN:${prefix}`);
      this._error = debug(`${appName}:ERROR:${prefix}`);
    } else {
      this._debug = debug(`${appName}:DEBUG`);
      this._info = debug(`${appName}:INFO`);
      this._warn = debug(`${appName}:WARN`);
      this._error = debug(`${appName}:ERROR`);
    }
    this._debug.log = console.debug.bind(console);
    this._info.log = console.info.bind(console);
    this._warn.log = console.warn.bind(console);
    this._error.log = console.error.bind(console);
  }

  get debug() {
    return this._debug.log;
  }
  
  get info() {
    return this._info.log;
  }

  get warn() {
    return this._warn.log;
  }

  get error() {
    return this._error.log;
  }

}

module.exports = Logger;
