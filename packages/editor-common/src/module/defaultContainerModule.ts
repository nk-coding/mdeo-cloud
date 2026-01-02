import { bindOrRebind, ConsoleLogger, TYPES } from "@eclipse-glsp/sprotty";
import { ContainerModule } from "inversify";

/**
 * The default container module for the editor, handling
 * - logging
 */
export const defaultContainerModule = new ContainerModule((bind, unbind, isBound, rebind) => {
    const context = { bind, unbind, isBound, rebind };
    bindOrRebind(context, TYPES.ILogger).to(ConsoleLogger).inSingletonScope();
})