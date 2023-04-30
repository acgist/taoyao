<!-- 本地终端 -->
<template>
  <div class="client">
    <audio ref="audio"></audio>
    <video ref="video"></video>
    <p class="title">{{ client?.name || "" }}</p>
    <div class="buttons" :style="{'--volume': client?.volume}">
      <el-button @click="taoyao.mediaProducerResume(audioProducer.id)" v-show="audioProducer &&  audioProducer.paused" type="primary" title="打开麦克风" :icon="Microphone" circle />
      <el-button @click="taoyao.mediaProducerPause(audioProducer.id)"  v-show="audioProducer && !audioProducer.paused" type="danger"  title="关闭麦克风" :icon="Mute"       circle />
      <el-button @click="taoyao.mediaProducerResume(videoProducer.id)" v-show="videoProducer &&  videoProducer.paused" type="primary" title="打开摄像头" :icon="VideoPlay"  circle />
      <el-button @click="taoyao.mediaProducerPause(videoProducer.id)"  v-show="videoProducer && !videoProducer.paused" type="danger"  title="关闭摄像头" :icon="VideoPause" circle />
      <el-button @click="exchangeVideoSource" :icon="Refresh" circle title="交换媒体" />
      <el-button :icon="Camera"      circle title="拍照" />
      <el-button :icon="VideoCamera" circle title="录像" />
      <el-button @click="taoyao.mediaConsumerStatus()" :icon="InfoFilled"  circle title="媒体信息" />
      <el-popover placement="top" :width="240" trigger="hover">
        <template #reference>
          <el-button>视频质量</el-button>
        </template>
        <el-table :data="taoyao.options">
          <el-table-column width="100" property="value" label="标识" />
          <el-table-column width="100" property="label" label="名称" />
        </el-table>
      </el-popover>
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
  name: "LocalClient",
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
      dataProducer: null,
      audioProducer: null,
      videoProducer: null,
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
    exchangeVideoSource() {
      // TODO：文件支持
      this.taoyao.videoSource = this.taoyao.videoSource === "camera" ? "screen" : "camera";
      this.taoyao.updateVideoProducer();
    },
    media(track, producer) {
      if(track.kind === "audio") {
        // 不用加载音频
        this.audioProducer = producer;
      } else if (track.kind === "video") {
        this.videoProducer = producer;
        if (this.videoStream) {
          this.videoStream.getVideoTracks().forEach(oldTrack => {
            console.debug("关闭旧的媒体：", oldTrack);
            oldTrack.stop();
          });
        }
        this.videoStream = new MediaStream();
        this.videoStream.addTrack(track);
        this.video.srcObject = this.videoStream;
        this.video.play().catch((error) => console.warn("视频播放失败", error));
      } else {
        console.debug("本地不支持的媒体类型：", track);
      }
    },
  },
};
</script>
<style scoped>
.client .buttons:after{width:var(--volume);}
</style>