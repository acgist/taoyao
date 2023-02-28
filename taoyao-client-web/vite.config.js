import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";
import fs from "node:fs";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8443,
    host: "0.0.0.0",
    https: {
      cert: fs.readFileSync("src/certs/server.crt"),
      key: fs.readFileSync("src/certs/server.key"),
    },
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
