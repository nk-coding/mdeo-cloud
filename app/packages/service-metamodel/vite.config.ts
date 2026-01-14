import { defineConfig } from "vite";
import { resolve } from "path";

export default defineConfig({
    build: {
        lib: {
            entry: {
                language: resolve(__dirname, "src/served/language.ts"),
                editor: resolve(__dirname, "src/served/editor.ts")
            },
            formats: ["es"],
            cssFileName: "styles"
        },
        outDir: "static",
        emptyOutDir: true,
        sourcemap: false
    }
});
