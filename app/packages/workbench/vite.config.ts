import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";

// Polyfill path for util/types (required by typir)
// See: https://github.com/TypeFox/typir/issues/96
const utilTypesPolyfillPath = fileURLToPath(new URL("./src/lib/utilTypesPolyfill.ts", import.meta.url));

export default defineConfig({
    plugins: [vue(), tailwindcss()],
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url)),
            "util/types": utilTypesPolyfillPath
        }
    },
    worker: {
        format: "es"
    },
    server: {
        port: 4242,
        host: "127.0.0.1",
        proxy: {
            "/plugin/metamodel": {
                target: "http://localhost:3000",
                changeOrigin: true,
                secure: false,
                rewrite: (path) => path.replace(/^\/plugin\/metamodel/, "")
            },
            "/plugin/model": {
                target: "http://localhost:3001",
                changeOrigin: true,
                secure: false,
                rewrite: (path) => path.replace(/^\/plugin\/model/, "")
            },
            "/plugin/script": {
                target: "http://localhost:3002",
                changeOrigin: true,
                secure: false,
                rewrite: (path) => path.replace(/^\/plugin\/script/, "")
            },
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: true,
                secure: false
            }
        }
    },
    build: {
        rollupOptions: {
            output: {
                format: "es",
                manualChunks: undefined
            }
        }
    }
});
