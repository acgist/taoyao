<!-- 房间设置 -->
<template>
  <el-dialog
    v-model="localVisible"
    @open="init"
    width="30%"
    :show-close="false"
    center
  >
    <el-form ref="SettingRoomForm" :model="room">
      <el-tabs v-model="activeName">
        <el-tab-pane label="进入房间" name="enter">
          <el-form-item label="房间标识">
            <el-select v-model="room.id" placeholder="房间标识">
              <el-option
                v-for="value in rooms"
                :key="value.id"
                :label="value.name"
                :value="value.id"
              />
            </el-select>
          </el-form-item>
        </el-tab-pane>
        <el-tab-pane label="创建房间" name="create">
          <el-form-item label="媒体服务">
            <el-select v-model="room.mediasoup" placeholder="媒体服务">
              <el-option
                v-for="mediasoup in config.webrtc.mediasoupList"
                :key="mediasoup.name"
                :label="mediasoup.name"
                :value="mediasoup.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="房间名称">
            <el-input v-model="room.name" placeholder="房间名称" />
          </el-form-item>
        </el-tab-pane>
      </el-tabs>
      <el-form-item label="房间密码">
        <el-input v-model="room.password" placeholder="房间密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="setting">设置</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { config, protocol } from "./Config.js";

export default {
  name: "SettingRoom",
  data() {
    return {
      config,
      room: {},
      rooms: [],
      activeName: "enter",
      localVisible: false,
    };
  },
  props: {
    taoyao: {},
    roomVisible: false,
  },
  watch: {
    roomVisible() {
      this.localVisible = this.roomVisible;
    },
  },
  methods: {
    async init() {
      let response = await this.taoyao.request(
        protocol.buildMessage("room::list")
      );
      this.rooms = response.body;
    },
    async setting() {
      this.localVisible = false;
      if (this.activeName === "enter") {
        await this.taoyao.request(
          protocol.buildMessage("room::enter", {
            sn: config.sn,
            ...this.room,
          })
        );
      } else {
        await this.taoyao.request(
          protocol.buildMessage("room::create", {
            sn: config.sn,
            ...this.room,
          })
        );
      }
    },
  },
};
</script>
