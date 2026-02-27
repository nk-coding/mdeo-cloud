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
