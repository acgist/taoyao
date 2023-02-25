<!-- 终端设置 -->
<template>
  <el-dialog
    center
    width="30%"
    title="终端设置"
    :show-close="false"
    v-model="localVisible"
  >
    <el-form ref="SettingSignalForm">
      <el-form-item label="终端名称">
        <el-input v-model="config.clientId" placeholder="终端名称" />
      </el-form-item>
      <el-form-item label="信令地址">
        <el-input v-model="config.host" placeholder="信令地址" />
      </el-form-item>
      <el-form-item label="信令端口">
        <el-input v-model="config.port" placeholder="信令端口" />
      </el-form-item>
      <el-form-item label="信令帐号">
        <el-input v-model="config.username" placeholder="信令帐号" />
      </el-form-item>
      <el-form-item label="信令密码">
        <el-input v-model="config.password" placeholder="信令密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="connect">连接</el-button>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: "SettingSignal",
  data() {
    return {
      config: {
        clientId: "taoyao",
        host: "localhost",
        port: 8888,
        username: "taoyao",
        password: "taoyao",
      },
      localVisible: false,
    };
  },
  props: {
    signalVisible: false,
  },
  watch: {
    signalVisible() {
      this.localVisible = this.signalVisible;
    },
  },
  methods: {
    connect() {
      this.localVisible = false;
      this.$emit("buildSignal", this.config);
    },
  },
};
</script>
