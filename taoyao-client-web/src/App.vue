<!-- 桃夭 -->
<template>
  <SettingRoom :roomVisible="roomVisible" :taoyao="taoyao" @buildMedia="buildMedia"></SettingRoom>
  <SettingSignal :signalVisible="signalVisible" @buildSignal="buildSignal"></SettingSignal>
</template>

<script>
import { Logger } from "./components/Logger.js";
import { Taoyao } from "./components/Taoyao.js";
import SettingRoom from "./components/SettingRoom.vue";
import SettingSignal from "./components/SettingSignal.vue";

export default {
  name: "Taoyao",
  data() {
    return {
      logger: {},
      taoyao: {},
      roomVisible: false,
      signalVisible: true,
    };
  },
  mounted() {
    this.logger = new Logger();
    this.taoyao = new Taoyao();
    this.logger.info("桃夭终端开始启动");
  },
  methods: {
    buildSignal: function() {
      let self = this;
      self.signalVisible = false;
      self.taoyao.buildSignal(self.callback);
    },
    buildMedia: function(roomId) {
      let self = this;
      self.taoyao.buildMedia(roomId);
    },
    /**
     * 信令回调
     *
     * @param {*} data 消息
     *
     * @return 是否继续执行
     */
    callback: function (data) {
      let self = this;
      switch (data.header.signal) {
        case "client::config":
          self.roomVisible = true;
          break;
        case "client::register":
          if(data.code === '3401') {
            self.signalVisible = true;
          }
          return true;
      }
      return false;
    },
  },
  components: {
    SettingRoom,
    SettingSignal,
  },
};
</script>
