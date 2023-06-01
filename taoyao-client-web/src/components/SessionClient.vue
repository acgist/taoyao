<!-- 会话终端 -->
<template>
  <div class="client">
    <audio ref="audio"></audio>
    <video ref="video"></video>
    <p class="title">{{ client.name || "" }}</p>
    <div class="buttons">
      <el-button @click="client.pause('audio')"  v-show="client.localAudioEnabled"  type="primary" title="关闭本地麦克风" :icon="Mic"  circle />
      <el-button @click="client.resume('audio')" v-show="!client.localAudioEnabled" type="danger"  title="打开本地麦克风" :icon="Mic"  circle />
      <el-button @click="client.pause('video')"  v-show="client.localVideoEnabled"  type="primary" title="关闭本地摄像头" :icon="Film" circle />
      <el-button @click="client.resume('video')" v-show="!client.localVideoEnabled" type="danger"  title="打开本地摄像头" :icon="Film" circle />
      <el-button @click="taoyao.sessionResume(client.id, 'audio')" v-show="audioStream && !client.remoteAudioEnabled" type="danger"  title="打开远程麦克风" :icon="Mute"       circle />
      <el-button @click="taoyao.sessionPause(client.id, 'audio')"  v-show="audioStream &&  client.remoteAudioEnabled" type="primary" title="关闭远程麦克风" :icon="Microphone" circle class="mic" :style="{'--volume': client.volume}" />
      <el-button @click="taoyao.sessionResume(client.id, 'video')" v-show="videoStream && !client.remoteVideoEnabled" type="danger"  title="打开远程摄像头" :icon="VideoPlay"  circle />
      <el-button @click="taoyao.sessionPause(client.id, 'video')"  v-show="videoStream &&  client.remoteVideoEnabled" type="primary" title="关闭远程摄像头" :icon="VideoPause" circle />
      <el-button @click="taoyao.controlPhotograph(client.clientId)"                                   :icon="Camera"      circle title="拍照" />
      <el-button @click="taoyao.controlClientRecord(client.clientId, (clientRecord = !clientRecord))" :icon="VideoCamera" circle title="录像" :type="clientRecord ? 'danger' : ''" />
      <el-popover placement="top" :width="240" trigger="hover">
        <template #reference>
          <el-button>视频质量</el-button>
        </template>
        <el-table :data="taoyao.options">
          <el-table-column width="100" property="value" label="标识" />
          <el-table-column width="100" property="label" label="名称" />
        </el-table>
      </el-popover>
      <el-button @click="close" title="踢出" :icon="CircleClose" circle />
    </div>
  </div>
</template>

<script>
import {
  Mic,
  Film,
  Mute,
  Camera,
  Refresh,
  VideoPlay,
  VideoPause,
  InfoFilled,
  Microphone,
  VideoCamera,
  CircleClose,
} from "@element-plus/icons-vue";
export default {
  name: "SessionClient",
  setup() {
    return {
      Mic,
      Film,
      Mute,
      Camera,
      Refresh,
      VideoPlay,
      VideoPause,
      InfoFilled,
      Microphone,
      VideoCamera,
      CircleClose,
    };
  },
  data() {
    return {
      audio: null,
      video: null,
      clientRecord: false,
      serverRecord: false,
      audioStream: null,
      videoStream: null,
    };
  },
  mounted() {
    this.audio = this.$refs.audio;
    this.video = this.$refs.video;
    this.client.proxy = this;
  },
  props: {
    "client": {
      type: Object
    },
    "taoyao": {
      type: Object
    }
  },
  methods: {
    close() {
      this.taoyao.sessionClose(this.client.id);
    },
    media(track) {
      if(track.kind === 'audio') {
        if (this.audioStream) {
          // TODO：资源释放
        } else {
          this.audioStream = new MediaStream();
          this.audioStream.addTrack(track);
          this.audio.srcObject = this.audioStream;
        }
        this.audio.play().catch((error) => console.warn("视频播放失败", error));
      } else if(track.kind === 'video') {
        if (this.videoStream) {
          // TODO：资源释放
        } else {
          this.videoStream = new MediaStream();
          this.videoStream.addTrack(track);
          this.video.srcObject = this.videoStream;
        }
        this.video.play().catch((error) => console.warn("视频播放失败", error));
      } else {

      }
    }
  }
};
</script>
<style scoped>
.client .mic{background:linear-gradient(to top, var(--el-color-primary) var(--volume, 100%), transparent 0%);}
</style>
