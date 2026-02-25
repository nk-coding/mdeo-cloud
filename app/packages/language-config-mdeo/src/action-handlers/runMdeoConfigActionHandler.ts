import type {
    ActionExecutionRequest,
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    FileMenuActionData
} from "@mdeo/language-common";
import { FileCategory, parseUri } from "@mdeo/language-common";
import { sharedImport, type ActionHandler } from "@mdeo/language-shared";

const { URI } = sharedImport("langium");

/**
 * Run action handler for executable MDEO config sections.
 * Produces a direct execution request for the originating .config file.
 */
export class RunMdeoConfigActionHandler implements ActionHandler {
    async startAction(params: ActionStartParams): Promise<ActionStartResponse> {
        const executionRequest = this.createExecutionRequest(params.data);
        if (executionRequest == undefined) {
            return {
                kind: "error",
                message: "Invalid config file URI for run action"
            };
        }

        return {
            kind: "completion",
            executions: [executionRequest]
        };
    }

    async submitAction(_params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        return {
            kind: "error",
            message: "Run action does not support additional submit steps"
        };
    }

    private createExecutionRequest(data: unknown): ActionExecutionRequest | undefined {
        const typedData = data as Partial<FileMenuActionData>;
        if (typeof typedData.uri !== "string") {
            return undefined;
        }

        const parsedUri = parseUri(URI.parse(typedData.uri));
        if (parsedUri.category !== FileCategory.RegularFile) {
            return undefined;
        }

        return {
            filePath: parsedUri.path,
            data: {}
        };
    }
}
