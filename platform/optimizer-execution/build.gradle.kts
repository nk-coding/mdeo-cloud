plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("com.mdeo.optimizerexecution.ApplicationKt")

    applicationDefaultJvmArgs = listOf(
        "--add-opens", "java.xml/org.xml.sax.helpers=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
    )
}

dependencies {
    implementation(project(":common"))
    implementation(project(":expression"))
    implementation(project(":metamodel"))
    implementation(project(":model-transformation"))
    implementation(project(":script"))
    implementation(project(":optimizer"))
    implementation(project(":execution-common"))
    implementation(project(":model-transformation-execution"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.websockets)
    implementation(libs.auth0.jwt)
    implementation(libs.auth0.jwks)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.postgresql.jdbc)
    implementation(libs.hikari)
    implementation(libs.gremlin.core)
    implementation(libs.tinkergraph.gremlin)
    implementation(libs.moeaframework)
    implementation(libs.logback)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.cbor)
    implementation(libs.ktor.serialization.kotlinx.cbor)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
}

tasks.shadowJar {
    archiveBaseName.set("optimizer-execution")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}
