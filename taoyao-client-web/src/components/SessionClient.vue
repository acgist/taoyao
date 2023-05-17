<!-- 会话终端 -->
<template>
  <div class="client">
    <audio ref="audio"></audio>
    <video ref="video"></video>
    <p class="title">{{ client?.name || "" }}</p>
    <div class="buttons">
      <el-button @click="taoyao.sessionResume(client.id, 'audio')" v-show="audioStream && !client.remoteAudioEnabled" type="danger"  title="打开麦克风" :icon="Mute"       circle />
      <el-button @click="taoyao.sessionPause(client.id, 'audio')"  v-show="audioStream &&  client.remoteAudioEnabled" type="primary" title="关闭麦克风" :icon="Microphone" circle class="mic" :style="{'--volume': client?.volume}" />
      <el-button @click="taoyao.sessionResume(client.id, 'video')" v-show="videoStream && !client.remoteVideoEnabled" type="danger"  title="打开摄像头" :icon="VideoPlay"  circle />
      <el-button @click="taoyao.sessionPause(client.id, 'video')"  v-show="videoStream &&  client.remoteVideoEnabled" type="primary" title="关闭摄像头" :icon="VideoPause" circle />
      <el-button @click="taoyao.controlPhotograph(client.clientId)"                 :icon="Camera"      circle title="拍照" />
      <el-button @click="taoyao.controlRecord(client.clientId, (record = !record))" :icon="VideoCamera" circle title="录像" :type="record ? 'danger' : ''" />
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
      record: false,
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
.client .mic{background:linear-gradient(to top, var(--el-color-primary) var(--volume), transparent 0%);}
</style>
