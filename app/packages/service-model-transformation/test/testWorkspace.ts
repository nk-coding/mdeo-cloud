import type { LangiumDocument } from "langium";
import { initializePluginContext, LangiumInstancePool, astHandler, collectDiagnostics } from "@mdeo/service-common";

initializePluginContext();

const { URI } = await import("langium");
const { metamodelPluginProvider } = await import("@mdeo/language-metamodel");
const { modelTransformationPluginProvider } = await import("@mdeo/language-model-transformation");
const { configMdeoPluginProvider } = await import("@mdeo/language-config-mdeo");

type AnyServices = any;

/**
 * A language instance as the services use it: its services and the build of a document.
 */
export interface TestInstance {
    services: AnyServices;
    buildDocument(uri: unknown): Promise<LangiumDocument>;
}

/**
 * The result of running a file data handler on one file of a {@link TestWorkspace}.
 */
export interface HandlerRun<T> {
    data: T;
    document: LangiumDocument<any>;
    instance: TestInstance;
    errors: string[];
}

/**
 * A virtual project for language tests, without a backend: every language instance reads the
 * project's files from memory, and the ASTs of imported metamodels are computed by a metamodel
 * instance, as the metamodel service serves them (the same set-up as the headless harness of
 * the Palladio projects).
 */
export class TestWorkspace {
    private readonly files: Map<string, string>;
    private readonly astCache = new Map<string, unknown>();

    /**
     * @param files The project's files, by project path (e.g. `/metamodel/base.mm`)
     */
    constructor(files: Record<string, string>) {
        this.files = new Map(Object.entries(files));
    }

    /**
     * Creates a fresh language instance for a file extension.
     */
    private newInstance(extension: ".mm" | ".mt" | ".config", serviceModule?: unknown): TestInstance {
        const [provider, languageId] =
            extension === ".mm"
                ? [metamodelPluginProvider, "metamodel"]
                : extension === ".mt"
                  ? [modelTransformationPluginProvider, "model-transformation"]
                  : [configMdeoPluginProvider, "config-mdeo"];
        const pool = new LangiumInstancePool({
            maxInstances: 1,
            languagePluginProvider: provider,
            serviceModule,
            languageId,
            extension,
            backendUrl: "http://localhost:1"
        } as unknown as ConstructorParameters<typeof LangiumInstancePool>[0]);
        const instance = (
            pool as unknown as { createInstance: (plugins: unknown[], key: string) => TestInstance }
        ).createInstance([], "[]");
        this.patchServerApi(instance);
        return instance;
    }

    /**
     * Answers file reads and AST requests from the virtual project.
     */
    private patchServerApi(instance: TestInstance): void {
        const api = instance.services.shared.ServerApi;
        api.readFile = async (path: string) => {
            const content = this.files.get(path);
            if (content == undefined) {
                throw new Error(`no file ${path}`);
            }
            api.trackedFileDependencies.push({ path, version: 1 });
            return { content, version: 1 };
        };
        api.getFileData = async (path: string, key: string) => {
            if (key !== "ast") {
                throw new Error(`unexpected file data ${key} for ${path}`);
            }
            return api.remember(path, key, { data: await this.astOf(path), version: 1 });
        };
    }

    /**
     * Computes the serialised AST of a metamodel, as the metamodel service's AST handler does.
     */
    private async astOf(path: string): Promise<unknown> {
        if (!this.astCache.has(path)) {
            const extension = path.endsWith(".mm") ? ".mm" : path.endsWith(".mt") ? ".mt" : undefined;
            if (extension == undefined) {
                throw new Error(`only metamodels and transformations are loaded as external ASTs, not ${path}`);
            }
            const result = await this.run(astHandler, path, extension);
            this.astCache.set(path, result.data);
        }
        return structuredClone(this.astCache.get(path));
    }

    /**
     * Validates the MDEO sections (search, solver, runtime) of a configuration, as the config
     * service's MDEO plugin does: the transformations it uses are loaded as external ASTs.
     *
     * @param handler The MDEO diagnostics request handler
     * @param resolverModule The service module with the problem metamodel resolver
     * @param path The configuration's project path
     * @param text The MDEO sections of the configuration
     * @param problemMetamodel The project path of the problem's metamodel
     * @returns The diagnostics' messages
     */
    async configDiagnostics(
        handler: (context: any) => Promise<any>,
        resolverModule: unknown,
        path: string,
        text: string,
        problemMetamodel: string
    ): Promise<string[]> {
        const instance = this.newInstance(".config", resolverModule);
        const result = await handler({
            body: {
                text,
                configFileUri: URI.file(path).toString(),
                dependencyData: { optimization: { problem: { metamodelPath: problemMetamodel } } }
            },
            instance,
            services: instance.services,
            serverApi: instance.services.shared.ServerApi,
            jwt: "",
            contributionPlugins: []
        });
        return (result.diagnostics as { message: string; severity?: number }[])
            .filter((d) => d.severity === 1)
            .map((d) => d.message);
    }

    /**
     * Runs a file data handler on a file, in a fresh instance of the file's language.
     *
     * @param handler The handler
     * @param path The project path of the file
     * @returns The handler's data, the built document and its errors
     */
    async run(handler: (context: any) => Promise<any>, path: string, extension: ".mm" | ".mt" = ".mt") {
        const instance = this.newInstance(extension);
        const uri = URI.file(path);
        const content = this.files.get(path);
        if (content == undefined) {
            throw new Error(`no file ${path}`);
        }
        instance.services.shared.workspace.LangiumDocuments.createDocument(uri, content);
        const result = await handler({
            fileInfo: { uri, version: 1 },
            instance,
            services: instance.services,
            serverApi: instance.services.shared.ServerApi,
            contributionPlugins: [],
            signal: new AbortController().signal
        });
        const document = instance.services.shared.workspace.LangiumDocuments.getDocument(uri) as LangiumDocument<any>;
        return { data: result.data, document, instance, errors: errorsOf(document) } satisfies HandlerRun<any>;
    }

    /**
     * Builds and validates a transformation, with everything it imports.
     *
     * @param path The project path of the transformation
     * @returns The document, its instance and its errors
     */
    async build(path: string) {
        const instance = this.newInstance(".mt");
        const uri = URI.file(path);
        instance.services.shared.workspace.LangiumDocuments.createDocument(uri, this.files.get(path) ?? "");
        const document = (await instance.buildDocument(uri)) as LangiumDocument<any>;
        return { document, instance, errors: errorsOf(document) };
    }
}

/**
 * Returns the error messages of a document (lexer, parser, linking and validation errors).
 *
 * @param document The document
 * @returns The messages
 */
export function errorsOf(document: LangiumDocument | undefined): string[] {
    if (document == undefined) {
        return ["document not found"];
    }
    return collectDiagnostics(document)
        .filter((d: { severity?: number }) => d.severity === 1)
        .map((d: { message: string }) => d.message);
}
