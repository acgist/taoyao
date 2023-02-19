<!-- 桃夭 -->
<template>
  <SettingRoom :taoyao="taoyao" :roomVisible="roomVisible" @buildMedia="buildMedia"></SettingRoom>
  <SettingSignal :signalVisible="signalVisible" @buildSignal="buildSignal"></SettingSignal>
</template>

<script>
import { Taoyao } from "./components/Taoyao.js";
import SettingRoom from "./components/SettingRoom.vue";
import SettingSignal from "./components/SettingSignal.vue";

export default {
  name: "Taoyao",
  data() {
    return {
      taoyao: {},
      roomVisible: false,
      signalVisible: true,
    };
  },
  mounted() {
    console.info("桃夭终端开始启动");
  },
  methods: {
    buildSignal(config) {
      let self = this;
      self.taoyao = new Taoyao({...config});
      self.signalVisible = false;
      self.taoyao.buildSignal(self.callback);
    },
    buildMedia(roomId) {
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
    callback(data) {
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
