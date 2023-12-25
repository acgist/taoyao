<!-- 远程终端 -->
<!--
注意：当生产者关闭以后不能操作消费该生产者的消费者
-->
<template>
  <div class="client">
    <audio ref="audio"></audio>
    <video ref="video"></video>
    <p class="title">{{ client.name || "未知终端" }}</p>
    <div class="buttons">
      <el-button @click="taoyao.mediaConsumerResume(audioConsumer.id)" v-show="audioConsumer &&  audioConsumer.paused" type="danger"  title="打开麦克风" :icon="Mute"       circle />
      <el-button @click="taoyao.mediaConsumerPause(audioConsumer.id)"  v-show="audioConsumer && !audioConsumer.paused" type="primary" title="关闭麦克风" :icon="Microphone" circle class="mic" :style="{'--volume': client.volume}" />
      <el-button @click="taoyao.mediaConsumerResume(videoConsumer.id)" v-show="videoConsumer &&  videoConsumer.paused" type="danger"  title="打开摄像头" :icon="VideoPlay"  circle />
      <el-button @click="taoyao.mediaConsumerPause(videoConsumer.id)"  v-show="videoConsumer && !videoConsumer.paused" type="primary" title="关闭摄像头" :icon="VideoPause" circle />
      <el-button @click="taoyao.controlPhotograph(client.clientId)"                                   :icon="Camera"       circle title="终端拍照" />
      <el-button @click="taoyao.controlClientRecord(client.clientId, (clientRecord = !clientRecord))" :icon="VideoCamera"  circle title="终端录像"   :type="clientRecord ? 'danger' : ''" />
      <el-button @click="taoyao.controlServerRecord(client.clientId, (serverRecord = !serverRecord))" :icon="MostlyCloudy" circle title="服务端录像" :type="serverRecord ? 'danger' : ''" />
      <el-button @click="taoyao.mediaConsumerStatus()"                                                :icon="InfoFilled"   circle title="媒体信息" />
      <el-popover placement="top" :width="240" trigger="hover">
        <template #reference>
          <el-button>视频质量</el-button>
        </template>
        <el-table @row-click="chooseRatio" :data="taoyao.options">
          <el-table-column width="100" property="label" label="标识" />
          <el-table-column width="100" property="value" label="宽高" />
        </el-table>
      </el-popover>
      <el-button @click="taoyao.roomExpel(client.clientId)" title="踢出" :icon="CircleClose" circle />
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
      dataConsumer : null,
      audioConsumer: null,
      videoConsumer: null,
    };
  },
  async mounted() {
    this.audio = this.$refs.audio;
    this.video = this.$refs.video;
    this.client.proxy = this;
    // 状态查询
    const status = await this.taoyao.clientStatus(this.client.clientId);
    this.clientRecord = status.clientRecording;
    this.serverRecord = status.serverRecording;
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
      this.taoyao.setVideoConfig(this.client.clientId, label);
    },
    media(track, consumer) {
      if(track.kind === 'audio') {
        this.audioConsumer = consumer;
        this.taoyao.closeMediaStream(this.audioStream);
        this.audioStream = new MediaStream();
        this.audioStream.addTrack(track);
        this.audio.srcObject = this.audioStream;
        this.audio.play().catch((error) => console.warn("音频播放失败", error));
      } else if(track.kind === 'video') {
        this.videoConsumer = consumer;
        this.taoyao.closeMediaStream(this.videoStream);
        this.videoStream = new MediaStream();
        this.videoStream.addTrack(track);
        this.video.srcObject = this.videoStream;
        this.video.play().catch((error) => console.warn("视频播放失败", error));
      } else {
        console.debug("不支持的媒体类型", track);
      }
    }
  }
};
</script>

<style scoped>
.client .mic{background:linear-gradient(to top, var(--el-color-primary) var(--volume, 100%), transparent 0%);}
</style>
