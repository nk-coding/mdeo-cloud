import { createConnection, BrowserMessageReader, BrowserMessageWriter } from "vscode-languageserver/browser.js";
import { startLanguageServer } from "langium/lsp";
import { createMetamodelServices } from "@mdeo/language-metamodel";
import { EmptyFileSystem } from "langium";

const messageReader = new BrowserMessageReader(self);
const messageWriter = new BrowserMessageWriter(self);

const connection = createConnection(messageReader, messageWriter);

const { shared } = createMetamodelServices({ connection, ...EmptyFileSystem });

startLanguageServer(shared);