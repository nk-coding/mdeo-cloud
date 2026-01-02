import type * as glspClient from "@eclipse-glsp/client";
import type * as glspSprotty from "@eclipse-glsp/sprotty";
import type * as inversify from "inversify";

/**
 * Context provided to plugins when they are initialized
 */
export interface PluginContext {
    "@eclipse-glsp/client": typeof glspClient;
    "@eclipse-glsp/sprotty": typeof glspSprotty;
    inversify: typeof inversify;
}