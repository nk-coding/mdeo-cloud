# Config MDEO plugin

Contributes the `search`, `solver` and `runtime` sections: which moves the search may make, which
algorithm makes them, and what resources the run may use. It also owns the executable section, so this
is the plugin that actually starts an optimisation.

## At a glance

| | |
| --- | --- |
| **Plugin id** | `config-mdeo-service` |
| **Display name** | Config MDEO |
| **Description** | Language support for config MDEO sections (search and solver) |
| **Default URL** | `/plugin/config-mdeo` |
| **Source** | `app/packages/service-config-mdeo`, `app/packages/language-config-mdeo` |
| **Depends on** | The [Config](/plugins/config) and [Config Optimization](/plugins/config-optimization) plugins; executions go to `optimizer-execution` |

## Languages contributed

| Language id | Name | Extension | Textual editor | Graphical editor | Generated |
| --- | --- | --- | --- | --- | --- |
| `config-mdeo` | Config MDEO | — | ❌ | ❌ | ✅ |

### The `config-mdeo` language

A generated language with no extension and no editor, used the same way as
[`config-optimization`](/plugins/config-optimization#the-config-optimization-language): the config
service sends it the fragment of a `.config` file containing this plugin's sections, and it parses
them with a standalone grammar built from the same rules.

## Contribution plugins contributed

| Target language | Sections | Executable |
| --- | --- | --- |
| `config` | `search`, `solver`, `runtime` | `solver` |

| | |
| --- | --- |
| **Contribution plugin id** | `config-mdeo` |
| **Short name** | `mdeo` |
| **Grammar dependencies** | `config-optimization` |
| **Section dependencies** | `problem` from `optimization` |

The section dependency matters: before this plugin can resolve `create Annotation` or
`mutate Canvas.layers` it has to know which metamodel the problem uses. The config service therefore
computes the `problem` section first and passes its result along.

### The `search` section

The mutation operators available to the search.

```mdeo-config
search {
    mutations {
        using "./match.mt"

        create Annotation
        delete Annotation
        mutate Circle

        add Layer.shapes
        remove Layer.shapes
        mutate Canvas.layers
    }
}
```

| Entry | Meaning |
| --- | --- |
| `using "path.mt"` | Use a hand-written model transformation as a mutation operator |
| `create Class` | Generate operators that add an object of the class, or of each concrete subclass |
| `delete Class` | Generate an operator that removes an object of the class or of a subclass |
| `add Class.edge` | Generate operators that link objects through the association end |
| `remove Class.edge` | Generate operators that unlink them |
| `mutate Class.edge` | Both of the above |
| `mutate Class` | `create`, `delete` and `mutate Class.edge` for every association end of the class |

`edge` is an association end of the class, declared on it or inherited; attributes are never changed.
Entries may be mixed and repeated in any order. Generated operators are written into the result tree
as `.mt_gen` files, so you can read exactly what the search was allowed to do.

#### Generated operators

Generated operators never break a multiplicity: applied to a model in which every object has an
allowed number of links through each association end, they produce a model in which it still does.
Refinements from the [goal section](/plugins/config-optimization#the-goal-section) count as part of
the metamodel, so the operators keep the refined bounds too.

Which operators an entry produces depends on the multiplicities at both ends of the association:

| Operator | Generated for | What it does |
| --- | --- | --- |
| `ADD_A_e`, `REMOVE_A_e` | ends where neither side is fixed | Link or unlink an `A` and a `B` |
| `CHANGE_A_e` | a fixed end, or a fixed opposite | Move a link to another partner |
| `SWAP_A_e` | a fixed end, or ends whose bounds can all be reached at once | Two `A`s exchange a partner |
| `CREATE_A_…` | every container and combination of repairs | Add an object and link it as its bounds require |
| `DELETE_A` | the class | Remove an object, moving partners that need a fixed number of links |

[Generated mutation operators](/guide/generated-operators) explains each operator, the cases in which
none can exist, and how the generator differs from the papers it is based on. Operators that could not
be generated, or are limited, are listed in `generated-mutations/warnings.md` in the results.

### The `solver` section

The search algorithm and when to stop. This is the **executable** section: a config file containing it
gets a run action.

```mdeo-config
solver {
    algorithm = NSGAII

    parameters {
        population = 40
        variation = mutation

        mutation {
            step = interval(1, 5)
            strategy = random
        }
    }

    termination {
        evolutions = 500
    }

    batches = 3
}
```

| Key | Values | Meaning |
| --- | --- | --- |
| `algorithm` | `NSGAII`, `IBEA`, `SPEA2`, `SMSMOEA`, `VEGA`, `PESA2`, `PAES` | The multi-objective algorithm |
| `batches` | integer | Run the same configuration this many times independently |

#### `parameters`

| Key | Values | Meaning |
| --- | --- | --- |
| `population` | integer | Population size |
| `variation` | `mutation`, `genetic`, `probabilistic` | How offspring are produced |
| `bisections` | integer | PESA2 / PAES only |
| `mutation { … }` | block | Mutation tuning, see below |
| `archive { size = n }` | block | PESA2 / PAES only |

#### `mutation`

| Key | Values | Meaning |
| --- | --- | --- |
| `step` | `n`, `fixed`, `fixed(n)`, `interval(lo, hi)` | How many operators to apply per mutation |
| `strategy` | `random`, `repetitive` | How operators are chosen |
| `selection` | `random` | How matches are selected |
| `application` | `random` | How an operator is applied |
| `credit` | `random` | Operator credit assignment |
| `repair` | `default` | Repair strategy after a mutation |

`step = interval(1, 5)` draws uniformly from `[1, 5)` on every call; `fixed` with no argument means
one.

#### `termination`

| Key | Meaning |
| --- | --- |
| `evolutions` | Stop after this many generations |
| `time` | Stop after this many seconds |
| `delta` | Stop when the improvement stays below this threshold |
| `iterations` | Number of non-improving iterations tolerated before stopping |

Several conditions combine with OR — the first one to trigger ends the run.

### The `runtime` section

Execution limits.

```mdeo-config
runtime {
    timeout {
        script = 1000
        transformation = 1000
    }

    backend = MDEO

    resources {
        threads = 10
        nodes = 4
        threadsPerNode = 3
    }
}
```

| Key | Meaning |
| --- | --- |
| `timeout.script` | Milliseconds a single objective or constraint evaluation may take |
| `timeout.transformation` | Milliseconds a single transformation application may take |
| `backend` | `MDEO` (default) or `Tinker` — the graph representation used during search |
| `resources.threads` | Upper bound on total worker threads |
| `resources.nodes` | Upper bound on execution nodes taking part |
| `resources.threadsPerNode` | Upper bound on threads per node |

All fields are optional and act as upper bounds against what the deployment actually offers.

### Full example

<<< @/../samples/language-tour/full.config{mdeo-config}

## Server-side capabilities

| Key | Kind | Contents |
| --- | --- | --- |
| `ast` | file data | The serialised AST of the standalone language |
| `config` | request | Computes the section data for `search`, `solver` and `runtime` |
| `config-execution` | request | Starts an optimisation run |
| `config-execution-get-summary` | request | The markdown summary of a run |
| `config-execution-get-file-tree` | request | The result file tree |
| `config-execution-get-file` / `-get-files` | request | Result file contents |
| `config-execution-cancel` | request | Cancel a running optimisation |
| `config-execution-delete` | request | Delete a finished run and its results |

Execution requests are forwarded to the `optimizer-execution` service, which is the component that
scales across nodes. See [Reading the results](/guide/results) for what comes back.
