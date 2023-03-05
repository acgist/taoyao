<!-- 远程终端 -->
<template>
  <div class="client">
    <audio ref="audio"></audio>
    <video ref="video"></video>
    <p class="title">{{ client?.name || "" }}</p>
    <div class="buttons" :style="{'--volume': client?.volume}">
      <el-button type="danger" title="打开麦克风" :icon="Mute" circle />
      <el-button type="primary" title="关闭麦克风" :icon="Microphone" circle />
      <el-button type="danger" title="打开摄像头" :icon="VideoPause" circle />
      <el-button type="primary" title="关闭摄像头" :icon="VideoPlay" circle />
      <el-button title="拍照" :icon="Camera" circle />
      <el-button title="录像" :icon="VideoCamera" circle />
      <el-button title="媒体信息" :icon="InfoFilled" circle />
      <el-button title="踢出" :icon="CircleClose" circle />
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
} from "@element-plus/icons";
export default {
  name: "RemoteClient",
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
      audioStream: null,
      videoStream: null,
      dataConsumer: null,
      audioConsumer: null,
      videoConsumer: null,
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
    media(track, consumer) {
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
.client .buttons:after{width:var(--volume);}
</style>