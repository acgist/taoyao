import App           from "./App.vue";
import ElementPlus   from "element-plus";
import { createApp } from "vue";
import "./assets/main.css";
import "element-plus/dist/index.css";

const app = createApp(App);
app.use(ElementPlus);
app.mount("#app");

console.info(`
中庭地白树栖鸦，冷露无声湿桂花。
今夜月明人尽望，不知秋思落谁家。

:: https://gitee.com/acgist/taoyao
`);
