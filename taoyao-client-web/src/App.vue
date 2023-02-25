<!-- 桃夭 -->
<template>
  <div class="menu">
    <el-button type="primary" @click="signalVisible = true">连接信令</el-button>
    <el-button type="primary" @click="roomVisible = true">选择房间</el-button>
    <el-button type="danger">关闭房间</el-button>
    <el-button>退出房间</el-button>
  </div>
  <SettingRoom :taoyao="taoyao" :roomVisible="roomVisible" @produceMedia="produceMedia"></SettingRoom>
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
      signalVisible: false,
    };
  },
  mounted() {
    console.info(`
      中庭地白树栖鸦，冷露无声湿桂花。
      今夜月明人尽望，不知秋思落谁家。
    `);
  },
  methods: {
    buildSignal(config) {
      let self = this;
      self.taoyao = new Taoyao({...config});
      self.signalVisible = false;
      self.taoyao.buildSignal(self.callback);
    },
    produceMedia() {
      let self = this;
      self.taoyao.produceMedia();
    },
    /**
     * 信令回调
     *
     * @param {*} data 消息
     * @param {*} error 异常
     *
     * @return 是否继续执行
     */
    async callback(data, error) {
      let self = this;
      switch (data.header.signal) {
        case "client::config":
          self.roomVisible = true;
          break;
        case "client::register":
          self.signalVisible = data.code !== '0000';
          return true;
        case "platform::error":
          if(error) {
            console.error("发生异常：", data, error);
          } else {
            console.warn("发生错误：", data);
          }
          break;
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
