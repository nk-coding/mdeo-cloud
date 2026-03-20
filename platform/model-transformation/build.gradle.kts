plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":expression"))
    implementation(project(":metamodel"))
    implementation(libs.kotlinx.serialization.json)
    
    // Apache TinkerPop Gremlin
    implementation(libs.gremlin.core)
    implementation(libs.tinkergraph.gremlin)
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
}
