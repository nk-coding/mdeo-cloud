import type { InferenceProblem, Type, TypirSpecifics } from "typir";

/**
 * A scope in the Typir type inference system.
 */
export interface Scope<Specifics extends TypirSpecifics> {
    /**
     * Gets the scope entry with the given name at the given position.
     * Utilizes lexical scoping rules.
     *
     * @param name the name of the scope entry
     * @param position the position at which to look for the scope entry
     * @returns the scope entry, or undefined if not found
     */
    getEntry(name: string, position: number): ScopeEntry<Specifics> | undefined;

    /**
     * Checks if the given scope entry is initialized at the given position.
     *
     * @param entry the scope entry
     * @param position the position to check
     * @returns true if the entry is initialized at the position, false otherwise
     */
    isEntryInitialized(entry: ScopeEntry<Specifics>, position: number): boolean;

    /**
     * Gets all scope entries visible at the given position.
     *
     * @param position the position to get entries for
     * @returns the list of scope entries
     */
    getEntries(position: number): ScopeEntry<Specifics>[];

    /**
     * Get all entries that are newly initialized in this scope.
     * Excludes entries initialized in parent scopes, and entries defined in this scope.
     *
     * @returns the set of newly initialized entries
     */
    getInitializedEntries(): Set<ScopeEntry<Specifics>>;
}

/**
 * A scope that is bound to a specific position.
 */
export interface BoundScope<Specifics extends TypirSpecifics> extends Scope<Specifics> {
    /**
     * Gets the scope entry with the given name at the given position.
     * Utilizes lexical scoping rules.
     *
     * @param name the name of the scope entry
     * @returns the scope entry, or undefined if not found
     */
    getEntry(name: string): ScopeEntry<Specifics> | undefined;

    /**
     * Checks if the given scope entry is initialized at the given position.
     *
     * @param entry the scope entry
     * @returns true if the entry is initialized at the position, false otherwise
     */
    isEntryInitialized(entry: ScopeEntry<Specifics>): boolean;

    /**
     * Gets all scope entries visible at the given position.
     *
     * @returns the list of scope entries
     */
    getEntries(): ScopeEntry<Specifics>[];

    /**
     * Get all entries that are newly initialized in this scope.
     * Excludes entries initialized in parent scopes, and entries defined in this scope.
     *
     * @returns the set of newly initialized entries
     */
    getInitializedEntries(): Set<ScopeEntry<Specifics>>;
}

/**
 * An entry in a scope.
 */
export interface ScopeEntry<Specifics extends TypirSpecifics> {
    /**
     * The name of the scope entry.
     */
    name: string;

    /**
     * The scope in which the entry is defined.
     */
    definingScope: Scope<Specifics>;

    /**
     * The position at which the entry is defined in the defining scope.
     */
    position: number;

    /**
     * Infers the type of the scope entry.
     */
    inferType(): Type | Array<InferenceProblem<Specifics>>;

    /**
     * The language AST node that defines this scope entry.
     */
    languageNode?: Specifics["LanguageType"];

    /**
     * Whether this property is readonly (only applicable for properties)
     */
    readonly?: boolean;
}

/**
 * Initialization information for a scope entry in a local scope.
 */
export interface ScopeLocalInitialization {
    /**
     * The name of the scope entry being initialized.
     */
    name: string;
    /**
     * The position at which the entry is initialized.
     */
    position: number;
}

/**
 * The default implementation of a scope.
 */
export class DefaultScope<Specifics extends TypirSpecifics> implements Scope<Specifics> {
    /**
     * A lookup for the initialization positions of entries.
     * This can include entries defined in parent scopes.
     * A number represents the position at which the entry is initialized, with -1 indicating it is initialized at the start.
     * A value of true indicates that the entry is initialized by a parent scope before entering this scope.
     * A value of false indicates that the entry is not initialized in this scope or by any parent scope before entering this scope.
     */
    private readonly initializationLookup: Map<ScopeEntry<Specifics>, number | boolean> = new Map();

    /**
     * A lookup for local entries by name.
     */
    private readonly localEntryLookup: Map<string, ScopeEntry<Specifics>> = new Map();

    /**
     * The index until which the sequential child scopes have been initialized.
     */
    private sequentialChildScopesInitializedUntil = -1;

    /**
     * Lazy cache for sequential child scopes returned by {@link sequencialChildScopesProvider}.
     */
    private _sequencialChildScopes: (Scope<Specifics>[] | undefined)[] = [];

    /**
     * Lazily resolves and returns the sequential child scopes.
     */
    private get sequencialChildScopes(): (Scope<Specifics>[] | undefined)[] {
        if (this._sequencialChildScopes.length === 0) {
            this._sequencialChildScopes = this.sequencialChildScopesProvider(this);
        }
        return this._sequencialChildScopes;
    }

    /**
     * Creates a new DefaultScope.
     *
     * @param parent the optional parent scope
     * @param entriesProvider provider for the local scope entries
     * @param sequencialChildScopesProvider provider for child scopes which are executed sequencially, used for initialization checks, may not include all child scopes
     * @param localInitializedEntries a map of entry names to the position at which they are initialized in this scope
     */
    constructor(
        private readonly parent: BoundScope<Specifics> | undefined,
        private readonly entriesProvider: (scope: Scope<Specifics>) => ScopeEntry<Specifics>[],
        private readonly sequencialChildScopesProvider: (scope: Scope<Specifics>) => (Scope<Specifics>[] | undefined)[],
        localInitializations: ScopeLocalInitialization[]
    ) {
        for (const entry of entriesProvider(this)) {
            this.localEntryLookup.set(entry.name, entry);
        }
        for (const init of localInitializations) {
            const entry = this.localEntryLookup.get(init.name);
            if (entry != undefined) {
                this.updateInitializationLookup(entry, init.position);
            }
        }
    }

    getEntry(name: string, position: number): ScopeEntry<Specifics> | undefined {
        const localEntry = this.localEntryLookup.get(name);
        if (localEntry != undefined && localEntry.position <= position) {
            return localEntry;
        }
        if (this.parent != undefined) {
            return this.parent.getEntry(name);
        }
        return undefined;
    }

    isEntryInitialized(entry: ScopeEntry<Specifics>, position: number): boolean {
        const cached = this.initializationLookup.get(entry);
        if (cached != undefined) {
            return this.isInitializedAt(position, cached);
        }
        if (this.initializeSequentialChildScopesUntil(position)) {
            const updated = this.initializationLookup.get(entry);
            if (updated != undefined) {
                return this.isInitializedAt(position, updated);
            }
        }
        if (entry.definingScope === this) {
            return false;
        }
        if (this.parent != undefined) {
            const initializedByParent = this.parent.isEntryInitialized(entry);
            this.updateInitializationLookup(entry, initializedByParent);
            return initializedByParent;
        }
        return false;
    }

    getEntries(position: number): ScopeEntry<Specifics>[] {
        const localEntries = [...this.localEntryLookup.values()].filter((entry) => entry.position <= position);
        if (this.parent != undefined) {
            const parentEntries = this.parent.getEntries();
            return [...localEntries, ...parentEntries];
        } else {
            return localEntries;
        }
    }

    getInitializedEntries(): Set<ScopeEntry<Specifics>> {
        this.initializeSequentialChildScopesUntil(Number.POSITIVE_INFINITY);
        const initializedEntries: Set<ScopeEntry<Specifics>> = new Set();
        for (const [entry, init] of this.initializationLookup.entries()) {
            if (typeof init === "number" && entry.definingScope !== this) {
                initializedEntries.add(entry);
            }
        }
        return initializedEntries;
    }

    /**
     * Initializes the initialization lookup for sequential child scopes until the given position.
     *
     * @param position the position until which to initialize
     * @return true if any new initializations were made, false otherwise
     */
    private initializeSequentialChildScopesUntil(position: number): boolean {
        const actualPosition = Math.min(position, this.sequencialChildScopes.length - 1);
        if (this.sequentialChildScopesInitializedUntil >= actualPosition) {
            return false;
        }
        for (let i = this.sequentialChildScopesInitializedUntil + 1; i <= actualPosition; i++) {
            const childScopes = this.sequencialChildScopes[i];
            if (childScopes == undefined) {
                continue;
            }
            const childInitializations = childScopes
                .map((scope) => scope.getInitializedEntries())
                .reduce<Set<ScopeEntry<Specifics>> | undefined>((previous, current) => {
                    if (previous == undefined) {
                        return current;
                    } else {
                        return current.intersection(previous);
                    }
                }, undefined);
            for (const entry of childInitializations ?? []) {
                this.updateInitializationLookup(entry, i);
            }
        }
        this.sequentialChildScopesInitializedUntil = actualPosition;
        return true;
    }

    /**
     * Updates the initialization position of the given entry in a consistent manner.
     * Throws an error if the update is inconsistent.
     *
     * @param entry the scope entry to update
     * @param position the new initialization value
     */
    private updateInitializationLookup(entry: ScopeEntry<Specifics>, position: number | boolean): void {
        const current = this.initializationLookup.get(entry);
        if (current == undefined) {
            this.initializationLookup.set(entry, position);
        } else if (position === false) {
            if (current !== false) {
                throw new Error("Cannot update initialization to false if it is already initialized.");
            }
        } else if (current === false) {
            throw new Error("Cannot update initialization from false to a position.");
        } else if (current === true) {
            // nothing to do here
        } else if (position === true) {
            this.initializationLookup.set(entry, true);
        } else if (position < current) {
            this.initializationLookup.set(entry, position);
        }
    }

    /**
     * Helper method for checking if an entry is initialized at a given position.
     *
     * @param position the position to check
     * @param init the initialization value from {@link initializationLookup}
     * @returns true if initialized, false otherwise
     */
    private isInitializedAt(position: number, init: number | boolean): boolean {
        if (init === true) {
            return true;
        } else if (init === false) {
            return false;
        } else {
            return init <= position;
        }
    }
}

/**
 * The default implementation of a bound scope.
 */
export class DefaultBoundScope<Specifics extends TypirSpecifics> implements BoundScope<Specifics> {
    /**
     * Creates a new DefaultBoundScope.
     *
     * @param scope the underlying scope
     * @param position the position to bind the scope to
     */
    constructor(
        private readonly scope: Scope<Specifics>,
        private readonly position: number
    ) {}

    getEntry(name: string): ScopeEntry<Specifics> | undefined {
        return this.scope.getEntry(name, this.position);
    }

    isEntryInitialized(entry: ScopeEntry<Specifics>): boolean {
        return this.scope.isEntryInitialized(entry, this.position);
    }

    getEntries(): ScopeEntry<Specifics>[] {
        return this.scope.getEntries(this.position);
    }

    getInitializedEntries(): Set<ScopeEntry<Specifics>> {
        return this.scope.getInitializedEntries();
    }
}
