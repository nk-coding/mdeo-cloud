import { createApp } from "vue";
import App from "./App.vue";
import "./index.css";
import { monacoPlugin } from "./plugins/monacoPlugin";

createApp(App).use(monacoPlugin).mount("#app");
