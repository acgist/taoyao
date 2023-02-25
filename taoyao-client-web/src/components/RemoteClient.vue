<!-- 远程终端 -->
<template>
  <video></video>
  <audio></audio>
  <div>
    <el-button type="primary" title="关闭麦克风" :icon="Edit" circle />
  </div>
</template>

<script>
export default {
  name: "RemoteClient",
}
</script>