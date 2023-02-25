<!-- 房间设置 -->
<template>
  <el-dialog
    center
    width="30%"
    title="房间设置"
    @open="init"
    :show-close="false"
    v-model="localVisible"
  >
    <el-form ref="SettingRoomForm" :model="room">
      <el-tabs v-model="activeName">
        <el-tab-pane label="进入房间" name="enter">
          <el-form-item label="房间标识">
            <el-select v-model="room.roomId" placeholder="房间标识">
              <el-option
                v-for="value in rooms"
                :key="value.roomId"
                :label="value.name || value.roomId"
                :value="value.roomId"
              />
            </el-select>
          </el-form-item>
        </el-tab-pane>
        <el-tab-pane label="创建房间" name="create">
          <el-form-item label="媒体服务">
            <el-select v-model="room.mediaId" placeholder="媒体服务标识">
              <el-option
                v-for="value in medias"
                :key="value.clientId"
                :label="value.name || value.clientId"
                :value="value.clientId"
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
      <el-button type="primary" @click="enter" v-if="activeName === 'enter'"
        >进入</el-button
      >
      <el-button type="primary" @click="create" v-if="activeName === 'create'"
        >创建</el-button
      >
    </template>
  </el-dialog>
</template>

<script>
import { protocol } from "./Config.js";

export default {
  name: "SettingRoom",
  data() {
    return {
      room: {},
      rooms: [],
      medias: [],
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
      const roomResponse = await this.taoyao.request(
        protocol.buildMessage("room::list")
      );
      this.rooms = roomResponse.body;
      const mediaResponse = await this.taoyao.request(
        protocol.buildMessage("client::list", {clientType:"MEDIA"})
      );
      this.medias = mediaResponse.body;
    },
    async enter() {
      await this.taoyao.enter(this.room.roomId);
      this.localVisible = false;
      this.$emit("produceMedia");
    },
    async create() {
      const room = await this.taoyao.create(this.room);
      this.room.roomId = room.roomId;
      await this.enter(room.roomId);
      this.localVisible = false;
      this.$emit("produceMedia");
    },
  },
};
</script>
