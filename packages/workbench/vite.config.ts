import { fileURLToPath, URL } from "node:url";
import { defineConfig, build as viteBuild } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";
import { readdirSync } from "node:fs";
import { join } from "node:path";

function buildModulesPlugin() {
    const modulesDir = fileURLToPath(new URL("./src/modules", import.meta.url));

    return {
        name: "build-modules",
        async closeBundle() {
            const moduleFiles = readdirSync(modulesDir).filter((file) => file.endsWith(".ts") || file.endsWith(".js"));

            for (const file of moduleFiles) {
                const modulePath = join(modulesDir, file);
                const moduleName = file.replace(/\.(ts|js)$/, "");

                await viteBuild({
                    configFile: false,
                    build: {
                        lib: {
                            entry: modulePath,
                            formats: ["es"],
                            fileName: () => `${moduleName}.js`
                        },
                        outDir: fileURLToPath(new URL("./dist/modules", import.meta.url)),
                        emptyOutDir: false,
                        rollupOptions: {
                            output: {
                                preserveModules: false,
                                inlineDynamicImports: true
                            }
                        }
                    }
                });
            }
        }
    };
}

export default defineConfig({
    plugins: [vue(), tailwindcss(), buildModulesPlugin()],
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url)),
            "/modules": fileURLToPath(new URL("./src/modules", import.meta.url))
        }
    },
    worker: {
        format: "es"
    },
    server: {
        port: 4242,
        host: "127.0.0.1"
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
