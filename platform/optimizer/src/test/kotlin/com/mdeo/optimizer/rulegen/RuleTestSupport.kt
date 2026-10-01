package com.mdeo.optimizer.rulegen

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.AssociationData
import com.mdeo.metamodel.data.AssociationEndData
import com.mdeo.metamodel.data.ClassData
import com.mdeo.metamodel.data.EnumData
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.metamodel.data.ModelDataInstance
import com.mdeo.metamodel.data.ModelDataLink
import com.mdeo.metamodel.data.ModelDataPropertyValue
import com.mdeo.metamodel.data.MultiplicityData
import com.mdeo.metamodel.data.PropertyData
import com.mdeo.modeltransformation.graph.mdeo.MdeoModelGraph
import com.mdeo.optimizer.config.RefinementConfig
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import kotlin.random.Random

/**
 * Small metamodels written the way `.mm` files write them.
 *
 * ```
 * mm("A", "B") { assoc("A.e[1..*]", "<-->", "B.f[0..1]") }
 * ```
 * An end is `Class`, `Class.name` (exactly one) or `Class.name[bounds]` with bounds `n`, `n..m`,
 * `n..*`, `*`, `+` or `?`. A class written `abstract X`, or `X : P, Q` extends `P` and `Q`.
 */
class MetamodelBuilder(private val path: String) {
    private val classes = mutableListOf<ClassData>()
    private val associations = mutableListOf<AssociationData>()
    private val enums = mutableListOf<EnumData>()

    fun cls(spec: String, vararg properties: PropertyData) {
        val abstract = spec.startsWith("abstract ")
        val rest = spec.removePrefix("abstract ")
        val name = rest.substringBefore(":").trim()
        val parents = if (":" in rest) rest.substringAfter(":").split(",").map { it.trim() } else emptyList()
        classes += ClassData(name, abstract, parents, properties.toList())
    }

    fun enum(name: String, vararg entries: String) {
        enums += EnumData(name, entries.toList())
    }

    fun assoc(source: String, operator: String, target: String) {
        associations += AssociationData(end(source), operator, end(target))
    }

    private fun end(spec: String): AssociationEndData {
        val className = spec.substringBefore(".").substringBefore("[")
        if ("." !in spec) return AssociationEndData(className, null, MultiplicityData(1, 1))
        val name = spec.substringAfter(".").substringBefore("[")
        val bounds = if ("[" in spec) spec.substringAfter("[").removeSuffix("]") else "1"
        return AssociationEndData(className, name, multiplicity(bounds))
    }

    fun build() = MetamodelData(path = path, classes = classes, enums = enums, associations = associations)

    companion object {
        fun multiplicity(bounds: String): MultiplicityData = when (bounds) {
            "*" -> MultiplicityData(0, -1)
            "+" -> MultiplicityData(1, -1)
            "?" -> MultiplicityData(0, 1)
            else -> if (".." in bounds) {
                val (lo, hi) = bounds.split("..")
                MultiplicityData(lo.toInt(), if (hi == "*") -1 else hi.toInt())
            } else MultiplicityData(bounds.toInt(), bounds.toInt())
        }
    }
}

fun mm(vararg classes: String, path: String = "/test.mm", body: MetamodelBuilder.() -> Unit): MetamodelData =
    MetamodelBuilder(path).apply { classes.forEach { cls(it) }; body() }.build()

fun refine(spec: String): RefinementConfig {
    val className = spec.substringBefore(".")
    val name = spec.substringAfter(".").substringBefore("[")
    val m = MetamodelBuilder.multiplicity(spec.substringAfter("[").removeSuffix("]"))
    return RefinementConfig(className, name, m.lower, m.upper)
}

fun spec(text: String): MutationRuleSpec {
    val keyword = text.substringBefore(" ")
    val target = text.substringAfter(" ")
    val action = if (keyword == "mutate") MutationAction.ALL else MutationAction.valueOf(keyword.uppercase())
    return MutationRuleSpec(target.substringBefore("."), target.substringAfter(".", "").ifEmpty { null }, action)
}

/**
 * Generates random models that respect the multiplicities of an [info], by repeated random
 * construction until one is consistent.
 */
class RandomModels(private val info: MetamodelInfo, private val metamodelData: MetamodelData, seed: Int) {
    private val random = Random(seed)

    /**
     * Returns a consistent model with up to [maxPerClass] objects per concrete class, or `null`
     * when none was found in [attempts] tries.
     */
    fun next(maxPerClass: Int = 7, attempts: Int = 5000): ModelData? {
        repeat(attempts) {
            val model = attempt(maxPerClass)
            if (MultiplicityChecker.violations(model, info).isEmpty()) return model
        }
        return null
    }

    private fun attempt(maxPerClass: Int): ModelData {
        val instances = metamodelData.classes.filter { !it.isAbstract }.flatMap { c ->
            (1..random.nextInt(1, maxPerClass + 1)).map { i ->
                ModelDataInstance("${c.name.lowercase()}$i", c.name, attributes(c.name))
            }
        }
        val classOf = instances.associate { it.name to it.className }
        val links = mutableListOf<ModelDataLink>()
        val count = HashMap<Pair<String, AssociationEnd>, Int>()
        val contained = HashSet<String>()
        val order = instances.withIndex().associate { it.value.name to it.index }

        fun objectsOf(className: String) = instances.filter { className in info.supertypesOf(it.className) }.map { it.name }
        fun room(obj: String, end: AssociationEnd) = !end.isBounded || (count[obj to end] ?: 0) < end.upper
        fun linked(a: String, end: AssociationEnd, b: String) = links.any {
            if (end.isSourceSide) it.sourceName == a && it.targetName == b && it.sourceProperty == end.name && it.targetProperty == end.opposite.name
            else it.sourceName == b && it.targetName == a && it.sourceProperty == end.opposite.name && it.targetProperty == end.name
        }
        fun canLink(a: String, end: AssociationEnd, b: String): Boolean {
            if (a == b && info.compatible(end.owner, end.target)) return false
            if (!room(a, end) || !room(b, end.opposite) || linked(a, end, b)) return false
            val part = when {
                end.isContainer -> a
                end.opposite.isContainer -> b
                else -> return true
            }
            val whole = if (part == a) b else a
            return part !in contained && order.getValue(whole) < order.getValue(part)
        }
        fun addLink(a: String, end: AssociationEnd, b: String) {
            links += if (end.isSourceSide) ModelDataLink(a, end.name, b, end.opposite.name)
            else ModelDataLink(b, end.opposite.name, a, end.name)
            count.merge(a to end, 1, Int::plus)
            count.merge(b to end.opposite, 1, Int::plus)
            if (end.isContainer) contained += a
            if (end.opposite.isContainer) contained += b
        }

        val ends = info.allEnds.filter { it.isSourceSide }.shuffled(random)
        for (end in ends) {
            for (side in listOf(end, end.opposite)) {
                if (side.name == null) continue
                for (a in objectsOf(side.owner).shuffled(random)) {
                    val want = random.nextInt(side.lower, (if (side.isBounded) side.upper else side.lower + 3) + 1)
                    for (b in objectsOf(side.target).shuffled(random)) {
                        if ((count[a to side] ?: 0) >= want) break
                        if (canLink(a, side, b)) addLink(a, side, b)
                    }
                }
            }
        }
        return ModelData(metamodelData.path, instances, links).also { require(classOf.isNotEmpty() || instances.isEmpty()) }
    }

    private fun attributes(className: String): Map<String, ModelDataPropertyValue> =
        info.propertiesOf(className).filter { it.multiplicity.lower > 0 && it.multiplicity.upper == 1 }.associate { p ->
            p.name to when {
                p.enumType != null -> ModelDataPropertyValue.EnumValue(info.enumEntries(p.enumType!!).first())
                p.primitiveType == "string" -> ModelDataPropertyValue.StringValue("x")
                p.primitiveType == "boolean" -> ModelDataPropertyValue.BooleanValue(true)
                else -> ModelDataPropertyValue.NumberValue(1.0)
            }
        }
}

/** The outcome of applying one operator once. */
sealed interface Applied {
    data class Yes(val model: ModelData) : Applied
    data object No : Applied
}

/**
 * Applies generated operators to models through the real transformation engine. Engine errors
 * are thrown, not reported as an operator that did not apply.
 */
class RuleRunner(metamodelData: MetamodelData) {
    private val metamodel = Metamodel.compile(metamodelData)

    /** Applies [mutation] to [model] once, picking a random match as the optimiser does. */
    fun apply(mutation: GeneratedMutation, model: ModelData): Applied {
        val graph = MdeoModelGraph.create(model, metamodel)
        try {
            return when (val result = TransformationEngine.create(graph, mutation.typedAst, deterministic = false).execute()) {
                is TransformationExecutionResult.Success -> Applied.Yes(graph.toModelData())
                is TransformationExecutionResult.Failure -> Applied.No
                is TransformationExecutionResult.Stopped ->
                    if (result.isNormalStop) Applied.Yes(graph.toModelData()) else Applied.No
            }
        } finally {
            graph.close()
        }
    }
}

/** A readable one-line rendering of a model, for failure messages. */
fun ModelData.render(): String =
    instances.joinToString(" ") { it.name } + " | " +
        links.joinToString(" ") { "${it.sourceName}.${it.sourceProperty ?: ""}--${it.targetName}.${it.targetProperty ?: ""}" }
