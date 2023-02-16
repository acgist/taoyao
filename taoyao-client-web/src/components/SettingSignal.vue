<!-- 终端设置 -->
<template>
  <el-dialog v-model="localVisible" title="终端设置" width="30%" :show-close="false" center>
    <el-form ref="SettingSignalForm">
      <el-form-item label="终端名称">
        <el-input v-model="config.clientId" placeholder="终端名称" />
      </el-form-item>
      <el-form-item label="信令帐号">
        <el-input v-model="config.username" placeholder="信令帐号" />
      </el-form-item>
      <el-form-item label="信令密码">
        <el-input v-model="config.password" placeholder="信令密码" />
      </el-form-item>
      <el-form-item label="信令地址">
        <el-input v-model="config.host" placeholder="信令地址" />
      </el-form-item>
      <el-form-item label="信令端口">
        <el-input v-model="config.port" placeholder="信令端口" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="setting">设置</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { config } from "./Config.js";

export default {
  name: "SettingSignal",
  data: () => {
    return {
      config,
      localVisible: true,
    };
  },
  props: {
    signalVisible: true,
  },
  watch: {
    signalVisible() {
      this.localVisible = this.signalVisible;
    },
  },
  methods: {
    setting: function () {
      this.localVisible = false;
      this.$emit("buildSignal");
    },
  },
};
</script>
