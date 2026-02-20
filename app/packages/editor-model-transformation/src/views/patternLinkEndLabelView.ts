import { GLabelView, sharedImport } from "@mdeo/editor-shared";

const { injectable } = sharedImport("inversify");

@injectable()
export class GPatternLinkEndLabelView extends GLabelView {}
