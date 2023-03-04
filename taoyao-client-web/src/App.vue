<!-- 桃夭 -->
<template>
  <!-- 信令 -->
  <el-dialog
    center
    width="30%"
    title="终端设置"
    :show-close="false"
    v-if="taoyao === null"
    v-model="signalVisible"
  >
    <el-form ref="SignalSetting">
      <el-form-item label="终端标识">
        <el-input v-model="config.clientId" placeholder="终端标识" />
      </el-form-item>
      <el-form-item label="终端名称">
        <el-input v-model="config.name" placeholder="终端名称" />
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
    @open="loadList"
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
            <el-select v-model="room.mediaClientId" placeholder="媒体服务标识">
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
      <el-button type="primary" @click="roomCreate" v-if="roomActive === 'create'">创建</el-button>
    </template>
  </el-dialog>

  <!-- 菜单 -->
  <div class="menus">
    <el-button type="primary" :disabled="taoyao !== null" @click="signalVisible = true">连接信令</el-button>
    <el-button type="primary" @click="roomActive = 'enter';roomVisible = true;">选择房间</el-button>
    <el-button type="primary" @click="roomActive = 'create';roomVisible = true;">创建房间</el-button>
    <el-button>邀请终端</el-button>
    <el-button>退出房间</el-button>
    <el-button @click="closeRoom()" type="danger">关闭房间</el-button>
  </div>

  <!-- 终端 -->
  <div class="clients">
    <LocalClient ref="local"></LocalClient>
    <RemoteClient :ref="'remote-' + kv[0]" v-for="(kv, index) in remoteClients" :key="index"></RemoteClient>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Taoyao } from "./components/Taoyao.js";
import LocalClient from './components/LocalClient.vue';
import RemoteClient from './components/RemoteClient.vue';

export default {
  name: "Taoyao",
  data() {
    return {
      room: {},
      rooms: null,
      medias: null,
      config: {
        clientId: "taoyao",
        name: "taoyao",
        host: "localhost",
        port: 8888,
        username: "taoyao",
        password: "taoyao",
      },
      taoyao: null,
      roomActive: "enter",
      roomVisible: false,
      signalVisible: false,
      remoteClients: new Map(),
    };
  },
  mounted() {
    console.info(`
      中庭地白树栖鸦，冷露无声湿桂花。
      今夜月明人尽望，不知秋思落谁家。
    `);
  },
  methods: {
    async connectSignal() {
      const me = this;
      me.taoyao = new Taoyao({ ...this.config });
      await me.taoyao.connectSignal(me.callback, me.callbackMedia);
      me.signalVisible = false;
      me.remoteClients = me.taoyao.remoteClients;
      // 全局绑定
      window.taoyao = me.taoyao;
    },
    async loadList() {
      this.rooms = await this.taoyao.roomList();
      this.medias = await this.taoyao.mediaList();
    },
    async enterRoom() {
      await this.taoyao.enterRoom(this.room.roomId, this.room.password);
      await this.taoyao.produceMedia();
      this.roomVisible = false;
    },
    async roomCreate() {
      const room = await this.taoyao.roomCreate(this.room);
      this.room.roomId = room.roomId;
      await this.enterRoom();
    },
    async closeRoom() {
      this.taoyao.closeRoom();
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
          return true;
      }
      return false;
    },
    /**
     * 媒体回调
     */
    callbackMedia(type, track, consumer) {
      const self = this;
      return new Promise((resolve, reject) => {
        if(type === 'local') {
          self.$refs.local.media(track);
        } else {
          this.$refs['remote-' + consumer.sourceId][0].media(track, consumer);
        }
        resolve();
      });
    },
  },
  components: {
    LocalClient,
    RemoteClient
  },
};
</script>

<style>
.menus{width:100%;top:1rem;left:0;text-align:center;position:fixed;z-index:1;}
.clients{width:100%;height:100%;top:0;left:0;position:fixed;}
.client{float:left;width:50vw;height:50vh;box-shadow:0 0 1px 0px rgba(0,0,0,0.4);}
.client .buttons{width:100%;bottom:1rem;left:0;text-align:center;position:absolute;padding:0.8rem 0;background:rgba(0,0,0,0.4);}
.client audio{display:none;}
.client video{width:100%;height:100%;}
</style>