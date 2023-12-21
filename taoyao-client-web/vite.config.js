import fs                     from "node:fs";
import vue                    from "@vitejs/plugin-vue";
import { defineConfig }       from "vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  server: {
    port : 8443,
    host : "0.0.0.0",
    https: {
      key : fs.readFileSync("src/certs/server.key"),
      cert: fs.readFileSync("src/certs/server.crt"),
    },
  },
  plugins: [ vue() ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});

console.info(`
中庭地白树栖鸦，冷露无声湿桂花。
今夜月明人尽望，不知秋思落谁家。

:: https://gitee.com/acgist/taoyao
`);
