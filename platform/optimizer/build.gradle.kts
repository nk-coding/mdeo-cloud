plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":expression"))
    implementation(project(":model-transformation"))
    implementation(project(":script"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Apache TinkerPop Gremlin
    implementation(libs.gremlin.core)
    implementation(libs.tinkergraph.gremlin)

    // MOEA Framework
    implementation(libs.moeaframework)

    // Logging
    implementation(libs.logback)

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
}

tasks.test {
    // When asyncProfilerLib is set (e.g. -PasyncProfilerLib=.../libasyncProfiler.so),
    // attach the agent at JVM startup and dump collapsed stacks to asyncProfilerOutput.
    // The shell script converts the .collapsed file to an interactive HTML flame graph.
    val profilerLib = findProperty("asyncProfilerLib") as? String
    if (profilerLib != null) {
        val outputFile = (findProperty("asyncProfilerOutput") as? String)
            ?: "${rootProject.rootDir}/tools/profile.collapsed"
        jvmArgs("-agentpath:$profilerLib=start,event=cpu,file=$outputFile")
    }
}
