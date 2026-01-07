import type { Config } from "tailwindcss";

export default {
    content: ["./src/**/*.ts", "../editor-shared/src/**/*.ts", "../language-metamodel/src/**/*.ts"],
    important: ".editor-metamodel"
} satisfies Config;
