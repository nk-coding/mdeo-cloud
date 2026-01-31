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
    implementation(libs.kotlinx.serialization.json)
    
    // Apache TinkerPop Gremlin
    implementation("org.apache.tinkerpop:gremlin-core:3.8.0")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation("org.apache.tinkerpop:tinkergraph-gremlin:3.8.0")
}
