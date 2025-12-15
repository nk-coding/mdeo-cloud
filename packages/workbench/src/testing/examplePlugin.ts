import type { Plugin } from "@/data/plugin/plugin";

export const examplePlugin: Plugin = {
    id: "example-plugin",
    fileTypes: [
        {
            id: "metamodel",
            extension: ".mm",
            name: "Metamodel"
        }
    ]
};
