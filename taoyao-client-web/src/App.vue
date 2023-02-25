<!-- 桃夭 -->
<template>
  <!-- 信令 -->
  <el-dialog
    center
    width="30%"
    title="终端设置"
    :show-close="false"
    v-model="signalVisible"
  >
    <el-form ref="SignalSetting">
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
      <el-button type="primary" @click="connectSignal">连接信令</el-button>
    </template>
  </el-dialog>

  <!-- 房间 -->
  <el-dialog
    center
    width="30%"
    title="房间设置"
    @open="init"
    :show-close="false"
    v-model="roomVisible"
  >
    <el-form ref="RoomSetting" :model="room">
      <el-tabs v-model="roomActive">
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
      <el-button type="primary" @click="enterRoom" v-if="roomActive === 'enter'">进入</el-button>
      <el-button type="primary" @click="createRoom" v-if="roomActive === 'create'">创建</el-button>
    </template>
  </el-dialog>

  <!-- 菜单 -->
  <div class="menu">
    <el-button type="primary" @click="signalVisible = true">连接信令</el-button>
    <el-button type="primary" @click="roomActive = 'enter'; roomVisible = true;">选择房间</el-button>
    <el-button type="primary" @click="roomActive = 'create';roomVisible = true;">创建房间</el-button>
    <el-button>退出房间</el-button>
    <el-button type="danger">关闭房间</el-button>
  </div>

  <!-- 终端 -->
  <div class="client">
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Taoyao } from "./components/Taoyao.js";

export default {
  name: "Taoyao",
  data() {
    return {
      room: {},
      rooms: [],
      medias: [],
      config: {
        clientId: "taoyao",
        host: "localhost",
        port: 8888,
        username: "taoyao",
        password: "taoyao",
      },
      taoyao: {},
      roomActive: "enter",
      roomVisible: false,
      signalVisible: true,
    };
  },
  mounted() {
    console.info(`
      中庭地白树栖鸦，冷露无声湿桂花。
      今夜月明人尽望，不知秋思落谁家。
    `);
  },
  methods: {
    async init() {
      this.rooms = await this.taoyao.roomList();
      this.medias = await this.taoyao.mediaList();
    },
    async enterRoom() {
      await this.taoyao.enterRoom(this.room.roomId);
      await this.taoyao.produceMedia();
      this.roomVisible = false;
    },
    async createRoom() {
      const room = await this.taoyao.createRoom(this.room);
      this.room = room;
      await this.enterRoom(room.roomId);
    },
    async connectSignal() {
      let self = this;
      self.taoyao = new Taoyao({ ...this.config });
      await self.taoyao.connectSignal(self.callback);
      self.signalVisible = false;
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
          self.signalVisible = data.code !== "0000";
          return true;
        case "platform::error":
          if (error) {
            console.error("发生异常：", data, error);
          } else {
            console.warn("发生错误：", data);
          }
          ElMessage({
            showClose: true,
            message: data.message,
            type: "error",
          });
          break;
      }
      return false;
    },
  },
};
</script>
