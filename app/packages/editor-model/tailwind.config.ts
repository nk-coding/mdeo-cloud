import type { Config } from "tailwindcss";

export default {
    content: ["./src/**/*.ts", "../editor-shared/src/**/*.ts"],
    important: ".editor-model"
} satisfies Config;
