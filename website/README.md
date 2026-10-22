# MDEO Cloud documentation

The source of the documentation site published at
<https://mde-optimiser.github.io/mdeo-cloud/>.

```
website/
├── docs/                    VitePress site
│   ├── .vitepress/
│   │   ├── config.ts        Site configuration, navigation, base path
│   │   └── languages.ts     TextMate grammars for the five MDEO languages
│   ├── guide/               Introduction and user documentation
│   ├── plugins/             One page per bundled plugin
│   └── develop/             How to write plugins and languages
├── samples/                 DSL sample projects, embedded into the docs
└── scripts/
    └── validate-samples.mjs Parses and validates every sample
```

## Commands

```bash
npm install
npm run validate   # parse and validate every sample under samples/
npm run dev        # http://localhost:5173/mdeo-cloud/
npm run build      # validate, then build the static site into docs/.vitepress/dist
npm run preview    # serve the built site
```

## Samples are the single source of truth

Code examples in the documentation are **not** written inline. They live as real files under
`samples/` and are embedded with VitePress' snippet syntax:

```md
<<< @/../samples/task-allocation/tasks.mm{mm}
```

`npm run validate` loads the built language packages from `../app/packages/*/dist`, creates one shared
Langium environment containing every bundled language — exactly as the workbench does — adds every
file of a sample project as a document, and runs a full build with validation. Parser errors, lexer
errors and validation diagnostics all fail the build.

This means a documented example cannot drift away from the language it documents: if a grammar or a
validation rule changes, the docs build breaks.

Run `npm run build:packages` in `../app` first if the language packages have changed.

Each direct subdirectory of `samples/` is validated as a self-contained project, so files in different
sample projects cannot see each other.

## Syntax highlighting

Shiki does not know the MDEO languages, so `docs/.vitepress/languages.ts` generates a TextMate grammar
for each of them from a keyword list, mirroring the Monarch tokenizers the plugins ship to Monaco.

| Grammar | Aliases | Files |
| --- | --- | --- |
| `mdeo-metamodel` | `mm` | `.mm` |
| `mdeo-model` | `m`, `m_gen` | `.m`, `.m_gen` |
| `mdeo-model-transformation` | `mt`, `mt_gen` | `.mt`, `.mt_gen` |
| `mdeo-script` | `fn` | `.fn` |
| `mdeo-config` | `config` | `.config` |

When a language gains a keyword, add it there too.

## Deployment

`.github/workflows/docs.yml` builds the site on every push to `main` that touches `website/` or
`app/packages/`, and publishes it to GitHub Pages. The `base` in `config.ts` defaults to `/mdeo-cloud/`; the workflow sets it (and the repository link) from the repository it runs in, `/<repository name>/`.
