/**
 * 日志
 */
const debug = require("debug");
const config = require("./Config");

const APP_NAME = config.name;

class Logger {

  constructor(prefix) {
    if (prefix) {
      this._debug = debug(`${APP_NAME}:${prefix}`);
      this._info = debug(`${APP_NAME}:INFO:${prefix}`);
      this._warn = debug(`${APP_NAME}:WARN:${prefix}`);
      this._error = debug(`${APP_NAME}:ERROR:${prefix}`);
    } else {
      this._debug = debug(APP_NAME);
      this._info = debug(`${APP_NAME}:INFO`);
      this._warn = debug(`${APP_NAME}:WARN`);
      this._error = debug(`${APP_NAME}:ERROR`);
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
