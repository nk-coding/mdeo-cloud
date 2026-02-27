package com.mdeo.optimizer.rulegen

/**
 * Internal repair specification types, one per edge repair operation.
 *
 * Equivalent to RepairSpecType in the original mde_optimiser rulegen library.
 *
 * - CREATE / CREATE_LB_REPAIR / CREATE_LB_REPAIR_MANY: create a node (with lower-bound repair variants)
 * - DELETE / DELETE_LB_REPAIR / DELETE_LB_REPAIR_MANY: delete a node (with lower-bound repair variants)
 * - ADD: add an edge between two existing nodes
 * - REMOVE: remove an existing edge
 * - CHANGE: redirect an edge from one target to another (for fixed-opposite multiplicity)
 * - SWAP: swap edge target (used when source multiplicity is fixed lower==upper)
 *
 * REPORT: The LB_REPAIR variants (CREATE_LB_REPAIR, CREATE_LB_REPAIR_MANY, DELETE_LB_REPAIR,
 * DELETE_LB_REPAIR_MANY) require additional lower-bound repair steps in the Henshin original.
 * In our DSL port these are not representable as a NAC/PAC chain; they are downgraded to the
 * base CREATE/DELETE variant with a comment in the generated rule name.
 */
enum class RepairSpecType {
    // Node operations
    CREATE,
    CREATE_LB_REPAIR,
    CREATE_LB_REPAIR_MANY,
    DELETE,
    DELETE_LB_REPAIR,
    DELETE_LB_REPAIR_MANY,

    // Edge operations
    ADD,
    REMOVE,
    CHANGE,
    SWAP
}
