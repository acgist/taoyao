<!-- 桃夭 -->
<template>
  <div id="taoyao">
    <!-- 终端设置 -->
    <el-dialog center width="30%" title="终端设置" v-model="signalVisible" :show-close="false">
      <el-form ref="SignalSetting">
        <el-form-item label="终端名称">
          <el-input v-model="config.name" placeholder="终端名称" />
        </el-form-item>
        <el-form-item label="终端标识">
          <el-input v-model="config.clientId" placeholder="终端标识" />
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

    <!-- 房间设置 -->
    <el-dialog center width="30%" title="房间设置" v-model="roomVisible" :show-close="false" @open="loadList">
      <el-form ref="RoomSetting">
        <el-tabs v-model="roomActive">
          <el-tab-pane label="监控终端" name="call">
            <el-form-item label="终端标识">
              <el-select v-model="room.callClientId" placeholder="监控终端标识">
                <el-option v-for="value in clients" :key="value.clientId" :label="value.name || value.clientId" :value="value.clientId" />
              </el-select>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="创建房间" name="create">
            <el-form-item label="媒体服务">
              <el-select v-model="room.mediaClientId" placeholder="媒体服务标识">
                <el-option v-for="value in medias" :key="value.clientId" :label="value.name || value.clientId" :value="value.clientId" />
              </el-select>
            </el-form-item>
            <el-form-item label="房间名称">
              <el-input v-model="room.name" placeholder="房间名称" />
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="选择房间" name="enter">
            <el-form-item label="房间标识">
              <el-select v-model="room.roomId" placeholder="进入房间标识">
                <el-option v-for="value in rooms" :key="value.roomId" :label="value.name || value.roomId" :value="value.roomId" />
              </el-select>
            </el-form-item>
          </el-tab-pane>
          <el-tab-pane label="邀请终端" name="invite">
            <el-form-item label="终端标识">
              <el-select v-model="room.inviteClientId" placeholder="邀请终端标识">
                <el-option v-for="value in clients" :key="value.clientId" :label="value.name || value.clientId" :value="value.clientId" />
              </el-select>
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
        <el-form-item label="房间密码" v-if="roomActive !== 'call'">
          <el-input v-model="room.password" placeholder="房间密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="sessionCall" v-if="roomActive === 'call'"  >监控</el-button>
        <el-button type="primary" @click="roomCreate"  v-if="roomActive === 'create'">创建</el-button>
        <el-button type="primary" @click="roomEnter"   v-if="roomActive === 'enter'" >进入</el-button>
        <el-button type="primary" @click="roomInvite"  v-if="roomActive === 'invite'">邀请</el-button>
      </template>
    </el-dialog>

    <!-- 菜单 -->
    <div class="menus">
      <el-button @click="signalVisible = true"                       :disabled="taoyao  && taoyao.connect"                     type="primary">连接信令</el-button>
      <el-button @click="roomActive = 'call';   roomVisible = true;" :disabled="!taoyao || !taoyao.connect"                                  >监控终端</el-button>
      <el-button @click="roomActive = 'create'; roomVisible = true;" :disabled="!taoyao || !taoyao.connect"                    type="primary">创建房间</el-button>
      <el-button @click="roomActive = 'enter';  roomVisible = true;" :disabled="!taoyao || !taoyao.connect"                    type="primary">选择房间</el-button>
      <el-button @click="roomActive = 'invite'; roomVisible = true;" :disabled="!taoyao || !taoyao.connect || !taoyao.roomId"                >邀请终端</el-button>
      <el-button @click="roomLeave"                                  :disabled="!taoyao || !taoyao.connect || !taoyao.roomId"                >离开房间</el-button>
      <el-button @click="roomClose"                                  :disabled="!taoyao || !taoyao.connect || !taoyao.roomId"  type="danger" >关闭房间</el-button>
    </div>

    <!-- 终端 -->
    <div class="clients">
      <!-- 本地终端 -->
      <LocalClient   v-if="taoyao && taoyao.roomId"                                 ref="local-client"              :client="taoyao" :taoyao="taoyao"></LocalClient>
      <!-- 远程终端 -->
      <RemoteClient  v-for="kv in remoteClients"  :key="'remote-client-'  + kv[0]" :ref="'remote-client-'  + kv[0]" :client="kv[1]"  :taoyao="taoyao"></RemoteClient>
      <!-- 远程会话 -->
      <SessionClient v-for="kv in sessionClients" :key="'session-client-' + kv[0]" :ref="'session-client-' + kv[0]" :client="kv[1]"  :taoyao="taoyao"></SessionClient>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus';
import { Taoyao }    from './components/Taoyao.js';
import LocalClient   from './components/LocalClient.vue';
import RemoteClient  from './components/RemoteClient.vue';
import SessionClient from './components/SessionClient.vue';

export default {
  name: "Taoyao",
  data() {
    return {
      room: {
        // 房间名称
        name          : null,
        // 房间ID
        roomId        : null,
        // 房间密码
        password      : null,
        // 监控终端ID
        callClientId  : null,
        // 媒体终端ID
        mediaClientId : null,
        // 邀请终端ID
        inviteClientId: null,
      },
      // 房间列表
      rooms  : null,
      // 媒体服务列表
      medias : null,
      // 终端列表
      clients: null,
      config: {
        name    : "桃夭",
        clientId: "taoyao",
        host    : "localhost",
        port    : 8888,
        username: "taoyao",
        password: "taoyao",
      },
      // 信令
      taoyao        : null,
      roomActive    : "call",
      roomVisible   : false,
      signalVisible : false,
      // 会议终端
      remoteClients : new Map(),
      // 监控终端
      sessionClients: new Map(),
    };
  },
  mounted() {
  },
  methods: {
    async connectSignal() {
      this.taoyao = new Taoyao({ ...this.config });
      // this.taoyao = new Taoyao({ ...this.config, fileVideo: video对象, videoSource: "file" });
      await this.taoyao.connectSignal(this.callback);
      this.signalVisible  = false;
      this.remoteClients  = this.taoyao.remoteClients;
      this.sessionClients = this.taoyao.sessionClients;
      // 全局绑定
      window.taoyao = this.taoyao;
    },
    async loadList() {
      this.rooms   = await this.taoyao.roomList();
      this.medias  = await this.taoyao.mediaServerList();
      this.clients = await this.taoyao.mediaClientList();
    },
    async sessionCall() {
      this.taoyao.sessionCall(this.room.callClientId);
      this.roomVisible = false;
    },
    async roomCreate() {
      const room = await this.taoyao.roomCreate(this.room);
      this.room.roomId = room.roomId;
      await this.roomEnter();
    },
    async roomEnter() {
      const response = await this.taoyao.roomEnter(this.room.roomId, this.room.password);
      const { code, message } = response;
      if(code !== '0000') {
        return;
      }
      await this.taoyao.mediaProduce();
      this.roomVisible = false;
    },
    async roomInvite() {
      this.taoyao.roomInvite(this.room.inviteClientId);
      this.roomVisible = false;
    },
    async roomLeave() {
      this.taoyao.roomLeave();
    },
    async roomClose() {
      this.taoyao.roomClose();
    },
    /**
     * 信令回调
     *
     * @param {*} response 回调
     * @param {*} error    异常
     *
     * @return 是否执行完成
     */
    async callback(response, error) {
      const {
        code,
        message,
        header,
        body
      } = response;
      const { signal } = header;
      switch (signal) {
        case "media::track"   :
          const { clientId, track } = body;
          console.debug("新增媒体轨道", clientId, track);
          break;
        case "client::config" :
          this.roomVisible = true;
          break;
        case "platform::error":
          if (error) {
            console.error("发生异常", response, error);
          } else {
            console.warn("发生错误", response);
          }
          ElMessage({
            type   : "error",
            message: message,
          });
          return true;
      }
      return false;
    },
  },
  components: {
    LocalClient,
    RemoteClient,
    SessionClient,
  },
};
</script>

<style>
.menus{width:100%;top:1rem;left:0;text-align:center;position:fixed;z-index:1;}
.clients{width:100%;height:100%;top:0;left:0;position:fixed;}
.client{float:left;width:50vw;height:50vh;box-shadow:0 0 1px 0px rgba(0,0,0,0.4);}
.client audio{display:none;}
.client video{width:100%;height:100%;}
.client .mic{background:linear-gradient(to top, var(--el-color-primary) 100%, transparent 0%);}
.client .title{position:absolute;top:0;left:0;text-align:center;width:100%;}
.client .buttons{width:100%;bottom:0;left:0;text-align:center;position:absolute;padding:0.8rem 0;background:rgba(0,0,0,0.4);}
.client .buttons .el-button{margin:0 6px;}
</style>
