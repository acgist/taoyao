<!-- 本地终端 -->
<template></template>

<script>
import { defineComponent } from "@vue/composition-api";

export default defineComponent({
  setup() {
    // 本地视频
    this._externalVideo = document.createElement("video");

    this._externalVideo.controls = true;
    this._externalVideo.muted = true;
    this._externalVideo.loop = true;
    this._externalVideo.setAttribute("playsinline", "");
    this._externalVideo.src = EXTERNAL_VIDEO_SRC;

    // TODO：关闭摄像头、视频、音频
    this._externalVideo
      .play()
      .catch((error) => console.warn("externalVideo.play() failed:%o", error));
  },
});
</script>
