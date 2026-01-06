import { createApp } from "vue";
import "./index.css";
import "./sprotty.css";
import { monacoPlugin } from "./lib/monacoPlugin";
import { editorPlugin } from "./lib/editorPlugin";

createApp((await import("./App.vue")).default)
    .use(monacoPlugin)
    .use(editorPlugin)
    .mount("#app");
