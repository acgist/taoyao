<!-- 本地终端 -->
<template>
  <div class="client">
    <audio ref="audio"></audio>
    <video ref="video"></video>
    <p class="title">{{ client.name || "未知终端" }}</p>
    <div class="buttons">
      <el-button @click="taoyao.mediaProducerResume(audioProducer.id)" v-show="audioProducer &&  audioProducer.paused" type="danger"  title="打开麦克风" :icon="Mute"       circle />
      <el-button @click="taoyao.mediaProducerPause(audioProducer.id)"  v-show="audioProducer && !audioProducer.paused" type="primary" title="关闭麦克风" :icon="Microphone" circle class="mic" :style="{'--volume': client.volume}" />
      <el-button @click="taoyao.mediaProducerResume(videoProducer.id)" v-show="videoProducer &&  videoProducer.paused" type="danger"  title="打开摄像头" :icon="VideoPlay"  circle />
      <el-button @click="taoyao.mediaProducerPause(videoProducer.id)"  v-show="videoProducer && !videoProducer.paused" type="primary" title="关闭摄像头" :icon="VideoPause" circle />
      <el-button @click="taoyao.exchangeVideoSource()"                                                       :icon="Refresh"      circle title="交换媒体" />
      <el-button @click="taoyao.localPhotograph(video)"                                                      :icon="Camera"       circle title="本地拍照" />
      <el-button @click="taoyao.localClientRecord(audioStream, videoStream, (clientRecord = !clientRecord))" :icon="VideoCamera"  circle title="本地录像"   :type="clientRecord ? 'danger' : ''" />
      <el-button @click="taoyao.controlServerRecord(client.clientId, (serverRecord = !serverRecord))"        :icon="MostlyCloudy" circle title="服务端录像" :type="serverRecord ? 'danger' : ''" />
      <el-button @click="taoyao.mediaProducerStatus()"                                                       :icon="InfoFilled"   circle title="媒体信息" />
      <el-popover placement="top" :width="240" trigger="hover">
        <template #reference>
          <el-button>视频质量</el-button>
        </template>
        <el-table @cell-click="chooseRatio" :data="taoyao.options">
          <el-table-column width="100" property="label" label="标识" />
          <el-table-column width="100" property="value" label="高宽" />
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
  MostlyCloudy,
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
      MostlyCloudy,
    };
  },
  data() {
    return {
      audio        : null,
      video        : null,
      audioStream  : null,
      videoStream  : null,
      clientRecord : false,
      serverRecord : false,
      dataProducer : null,
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
    chooseRatio(row) {
      const { label, value } = row;
      this.taoyao.setLocalVideoConfig(label);
    },
    media(track, producer) {
      if(track.kind === "audio") {
        this.audioProducer = producer;
        this.taoyao.closeMediaStream(this.audioStream);
        this.audioStream = new MediaStream();
        this.audioStream.addTrack(track);
        // 不用加载音频
      } else if (track.kind === "video") {
        this.videoProducer = producer;
        this.taoyao.closeMediaStream(this.videoStream);
        this.videoStream = new MediaStream();
        this.videoStream.addTrack(track);
        this.video.srcObject = this.videoStream;
        this.video.play().catch((error) => console.warn("视频播放失败", error));
      } else {
        console.debug("不支持的媒体类型", track);
      }
    },
  },
};
</script>

<style scoped>
.client .mic{background:linear-gradient(to top, var(--el-color-primary) var(--volume, 100%), transparent 0%);}
</style>
