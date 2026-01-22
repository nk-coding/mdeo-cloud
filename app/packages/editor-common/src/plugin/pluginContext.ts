import type * as glspClient from "@eclipse-glsp/client";
import type * as glspSprotty from "@eclipse-glsp/sprotty";
import type * as glspProtocol from "@eclipse-glsp/protocol";
import type * as inversify from "inversify";
import type * as miniSearch from "minisearch";
import type * as lucide from "lucide";

/**
 * Context provided to plugins when they are initialized
 */
export interface PluginContext {
    "@eclipse-glsp/client": typeof glspClient;
    "@eclipse-glsp/sprotty": typeof glspSprotty;
    "@eclipse-glsp/protocol": typeof glspProtocol;
    inversify: typeof inversify;
    minisearch: typeof miniSearch;
    lucide: typeof lucide;
}
