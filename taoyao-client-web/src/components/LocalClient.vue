<!-- 本地终端 -->
<template>
  <div class="client">
    <audio ref="audio"></audio>
    <video ref="video"></video>
    <div class="buttons">
      <el-button type="danger" title="打开麦克风" :icon="Mute" circle />
      <el-button type="primary" title="关闭麦克风" :icon="Microphone" circle />
      <el-button type="danger" title="打开摄像头" :icon="VideoPause" circle />
      <el-button type="primary" title="关闭摄像头" :icon="VideoPlay" circle />
      <el-button title="交换媒体" :icon="Refresh" circle />
      <el-button title="拍照" :icon="Camera" circle />
      <el-button title="录像" :icon="VideoCamera" circle />
      <el-button title="媒体信息" :icon="InfoFilled" circle />
      <el-select placeholder="视频质量">
        <el-option
          v-for="option in options"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
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
      taoyao: null,
      audio: null,
      video: null,
      audioStream: null,
      videoStream: null,
      dataProducer: null,
      audioProducer: null,
      videoProducer: null,
      options: [
        {
          value: "HD",
          label: "高清",
        },
        {
          value: "SD",
          label: "标签",
        },
        {
          value: "FD",
          label: "超清",
        },
        {
          value: "BD",
          label: "蓝光",
        },
        {
          value: "QD",
          label: "2K",
        },
        {
          value: "UD",
          label: "4K",
        },
      ],
    };
  },
  mounted() {
    this.audio = this.$refs.audio;
    this.video = this.$refs.video;
  },
  methods: {
    media(track) {
      if (track.kind === "video") {
        if (this.videoStream) {
          // TODO：资源释放
        } else {
          this.videoStream = new MediaStream();
          this.videoStream.addTrack(track);
          this.video.srcObject = this.videoStream;
        }
        this.video.play().catch((error) => console.warn("视频播放失败", error));
      } else {
        console.debug("本地不支持的媒体类型：", track);
      }
    },
  },
};
</script>
