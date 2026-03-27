/**
 * Injection token for {@link IssueMarkerUIExtension}.
 *
 * Using a Symbol avoids magic string keys and gives a unique, comparably typed
 * identifier that can be shared between the feature module and any class that
 * needs the extension injected.
 */
export const ISSUE_MARKER_UI_EXTENSION_TOKEN = Symbol("IssueMarkerUIExtension");
