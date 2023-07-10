import fs                     from "node:fs";
import vue                    from "@vitejs/plugin-vue";
import { defineConfig }       from "vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8443,
    host: "0.0.0.0",
    https: {
      key : fs.readFileSync("src/certs/server.key"),
      cert: fs.readFileSync("src/certs/server.crt"),
    },
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
