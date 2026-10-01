package com.mdeo.optimizer.rulegen

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.statements.TypedForMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedIfMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement

/**
 * Turns an [OperatorSpec] into the transformation that performs it.
 *
 * Every operator keeps a model whose links respect the metamodel's (refined) multiplicities
 * consistent. Upper bounds are checked with `size() < upper` before a link is added, lower bounds
 * with `size() > lower` before one is removed, and a link that already exists is never added
 * twice. An object added to a whole must not be contained by another one yet.
 */
object MutationAstBuilder {

    private const val TOP = 1
    private const val TOP_BLOCK = 2
    private const val FOR_PATTERN = 2
    private const val IF_PATTERN = 4

    /**
     * Builds the transformation for [spec].
     *
     * @param warnings Receives notes on what the operator cannot do, e.g. attributes it leaves unset.
     */
    fun build(spec: OperatorSpec, info: MetamodelInfo, warnings: MutableList<String> = mutableListOf()): TypedAst {
        val w = RuleAstWriter(info)
        val statements = when (spec) {
            is CreateSpec -> listOf(match(create(spec, info, w, warnings)))
            is DeleteSpec -> delete(spec, info, w)
            is EdgeSpec -> listOf(match(edge(spec, info, w)))
        }
        return TypedAst(types = w.types(), metamodelPath = info.metamodelPath, statements = statements)
    }

    private fun match(elements: List<TypedPatternElement>) = TypedMatchStatement(pattern = TypedPattern(elements = elements))

    /**
     * Blocks rejecting the match when [part] already has a container through a composition other
     * than [except], so that linking it to a new whole keeps it in at most one.
     */
    private fun otherContainers(
        part: String,
        partClass: String,
        except: Int,
        info: MetamodelInfo,
        w: RuleAstWriter
    ): List<TypedPatternElement> =
        info.containerEndsCompatibleWith(partClass).filter { it.association != except }.mapIndexed { i, containerEnd ->
            val whole = "${part}_container_$i"
            w.forbid("${part}_uncontained_$i", listOf(w.obj(whole, containerEnd.target), w.link(containerEnd, part, whole)))
        }

    // =========================================================================
    // Edges
    // =========================================================================

    private fun edge(spec: EdgeSpec, info: MetamodelInfo, w: RuleAstWriter): List<TypedPatternElement> {
        val a = spec.className
        val e = spec.end
        val o = e.opposite
        val b = e.target
        val out = mutableListOf<TypedPatternElement>()
        when (spec.kind) {
            EdgeKind.ADD -> {
                out += w.obj("source", a)
                out += w.obj("target", b)
                out += w.forbid("alreadyLinked", listOf(w.link(e, "source", "target")))
                out += w.link(e, "source", "target", "create")
                if (e.isBounded) out += w.where(w.size("source", a, TOP, e, "<", e.upper))
                if (o.name != null && o.isBounded) out += w.where(w.size("target", b, TOP, o, "<", o.upper))
                if (e.isContainment) out += otherContainers("target", b, e.association, info, w)
                if (e.isContainer) out += otherContainers("source", a, e.association, info, w)
            }
            EdgeKind.REMOVE -> {
                out += w.obj("source", a)
                out += w.obj("target", b)
                out += w.link(e, "source", "target", "delete")
                if (e.lower > 0) out += w.where(w.size("source", a, TOP, e, ">", e.lower))
                if (o.name != null && o.lower > 0) out += w.where(w.size("target", b, TOP, o, ">", o.lower))
            }
            EdgeKind.CHANGE_SOURCE -> {
                out += w.obj("moved", b)
                out += w.obj("oldSource", a)
                out += w.obj("newSource", a)
                out += w.link(e, "oldSource", "moved", "delete")
                out += w.forbid("alreadyLinked", listOf(w.link(e, "newSource", "moved")))
                out += w.link(e, "newSource", "moved", "create")
                if (e.lower > 0) out += w.where(w.size("oldSource", a, TOP, e, ">", e.lower))
                if (e.isBounded) out += w.where(w.size("newSource", a, TOP, e, "<", e.upper))
                if (e.isContainer) out += otherContainers("newSource", a, e.association, info, w)
            }
            EdgeKind.CHANGE_TARGET -> {
                out += w.obj("source", a)
                out += w.obj("oldTarget", b)
                out += w.obj("newTarget", b)
                out += w.link(e, "source", "oldTarget", "delete")
                out += w.forbid("alreadyLinked", listOf(w.link(e, "source", "newTarget")))
                out += w.link(e, "source", "newTarget", "create")
                if (o.name != null && o.lower > 0) out += w.where(w.size("oldTarget", b, TOP, o, ">", o.lower))
                if (o.name != null && o.isBounded) out += w.where(w.size("newTarget", b, TOP, o, "<", o.upper))
                if (e.isContainment) out += otherContainers("newTarget", b, e.association, info, w)
            }
            EdgeKind.SWAP -> {
                out += w.obj("source", a)
                out += w.obj("otherSource", a)
                out += w.obj("target", b)
                out += w.obj("otherTarget", b)
                out += w.link(e, "source", "target", "delete")
                out += w.link(e, "otherSource", "otherTarget", "delete")
                out += w.forbid("alreadyLinked", listOf(w.link(e, "source", "otherTarget")))
                out += w.forbid("otherAlreadyLinked", listOf(w.link(e, "otherSource", "target")))
                out += w.link(e, "source", "otherTarget", "create")
                out += w.link(e, "otherSource", "target", "create")
            }
        }
        return out
    }

    // =========================================================================
    // Create
    // =========================================================================

    private fun create(spec: CreateSpec, info: MetamodelInfo, w: RuleAstWriter, warnings: MutableList<String>): List<TypedPatternElement> {
        val out = mutableListOf<TypedPatternElement>()
        val skipped = mutableListOf<com.mdeo.metamodel.data.PropertyData>()
        out += w.obj("newNode", spec.className, "create", w.defaultAttributes(spec.className, skipped))
        if (skipped.isNotEmpty()) {
            warnings += "'${spec.name}' leaves the required attributes ${skipped.joinToString { "'${it.name}'" }} " +
                "of '${spec.className}' without a value."
        }

        val ctx = spec.context
        if (ctx != null) {
            val containment = ctx.containmentEnd
            out += w.obj("container", ctx.wholeClass)
            out += w.link(containment, "container", "newNode", "create")
            if (containment.name != null && containment.isBounded) {
                out += w.where(w.size("container", ctx.wholeClass, TOP, containment, "<", containment.upper))
            }
        }

        for (requirement in spec.requirements) {
            val e = requirement.end
            val o = e.opposite
            val n = e.lower
            val targets = (1..n).map { "${e.name}_$it" }
            when (requirement.repair) {
                CreateRepair.CONNECT -> for (t in targets) {
                    out += w.obj(t, e.target)
                    out += w.link(e, "newNode", t, "create")
                    if (o.name != null && o.isBounded) out += w.where(w.size(t, e.target, TOP, o, "<", o.upper))
                    if (e.isContainment) out += otherContainers(t, e.target, e.association, info, w)
                }
                CreateRepair.LB_REPAIR -> {
                    val donor = "${e.name}_donor"
                    out += w.obj(donor, e.owner)
                    out += w.where(w.size(donor, e.owner, TOP, e, ">", 2 * n - 1))
                    for (t in targets) {
                        out += w.obj(t, e.target)
                        out += w.link(e, donor, t, "delete")
                        out += w.link(e, "newNode", t, "create")
                    }
                }
                CreateRepair.LB_REPAIR_MULTI -> targets.forEachIndexed { i, t ->
                    val donor = "${e.name}_donor_${i + 1}"
                    out += w.obj(donor, e.owner)
                    out += w.where(w.size(donor, e.owner, TOP, e, ">", n))
                    out += w.obj(t, e.target)
                    out += w.link(e, donor, t, "delete")
                    out += w.link(e, "newNode", t, "create")
                }
            }
        }
        return out
    }

    // =========================================================================
    // Delete
    // =========================================================================

    private fun delete(spec: DeleteSpec, info: MetamodelInfo, w: RuleAstWriter): List<TypedTransformationStatement> {
        val c = spec.className
        val guards = spec.guarded.mapIndexed { i, end ->
            val neighbour = "neighbour_$i"
            w.forbid(
                "${end.name}_keepsLowerBound_$i",
                listOf(
                    w.obj(neighbour, end.owner),
                    w.link(end, neighbour, "node"),
                    w.where(w.size(neighbour, end.owner, TOP_BLOCK, end, "<=", end.lower))
                )
            )
        }
        if (spec.rehomed.isEmpty()) {
            return listOf(match(listOf(w.obj("node", c, "delete")) + guards))
        }

        val statements = mutableListOf<TypedTransformationStatement>()
        statements += match(listOf(w.obj("node", c)) + guards)
        spec.rehomed.forEachIndexed { i, end ->
            val nodeSide = end.opposite
            val neighbour = "moved_$i"
            val other = "newPartner_$i"
            val place = mutableListOf<TypedPatternElement>(
                w.reference(neighbour),
                w.obj(other, end.target),
                w.where(w.distinct(other, end.target, IF_PATTERN, "node", c, TOP)),
                w.forbid("alreadyLinked_$i", listOf(w.link(end, neighbour, other))),
                w.link(end, neighbour, other, "create")
            )
            if (nodeSide.name != null && nodeSide.isBounded) {
                place += w.where(w.size(other, end.target, IF_PATTERN, nodeSide, "<", nodeSide.upper))
            }
            if (end.isContainment) place += otherContainers(other, end.target, end.association, info, w)
            statements += TypedForMatchStatement(
                pattern = TypedPattern(elements = listOf(w.reference("node"), w.obj(neighbour, end.owner), w.link(end, neighbour, "node", "delete"))),
                doBlock = listOf(
                    TypedIfMatchStatement(
                        pattern = TypedPattern(elements = place),
                        thenBlock = emptyList(),
                        elseBlock = listOf(TypedStopStatement(keyword = "kill"))
                    )
                )
            )
        }
        statements += match(listOf(w.deleteBound("node")))
        return statements
    }
}
