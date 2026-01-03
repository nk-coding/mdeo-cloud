import { createApp } from "vue";
import "./index.css";
import { monacoPlugin } from "./lib/monacoPlugin";
import { initializeEditorPluginContext } from "@mdeo/editor-common";
import * as inversify from "inversify";
import * as glspClient from "@eclipse-glsp/client";
import * as glspSprotty from "@eclipse-glsp/sprotty";

initializeEditorPluginContext({
    inversify,
    "@eclipse-glsp/client": glspClient,
    "@eclipse-glsp/sprotty": glspSprotty
});

createApp((await import("./App.vue")).default)
    .use(monacoPlugin)
    .mount("#app");
