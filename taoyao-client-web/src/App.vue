<template></template>

<script>
import { Logger } from "./components/Logger.js";
import { Taoyao } from "./components/Taoyao.js";

export default {
  name: "taoyao",
  data() {
    return {
      logger: null,
      taoyao: null,
    };
  },
  mounted() {
    // 填写、密码、帐号
    this.logger = new Logger();
    this.taoyao = new Taoyao();
    this.logger.info("桃夭终端启动");
    this.taoyao.buildChannel(this.callback);
  },
  beforeDestroy() {},
  methods: {
    /**
     * 信令回调
     *
     * @param {*} data 消息
     *
     * @return 是否继续执行
     */
    callback: function (data) {
      let self = this;
      switch (data.header.snail) {
      }
      return false;
    },
  },
};
</script>
