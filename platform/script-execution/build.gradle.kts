val ktorVersion = "3.3.3"
val exposedVersion = "0.61.0"
val hikariVersion = "7.0.2"
val postgresVersion = "42.7.8"
val logbackVersion = "1.5.24"

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("com.gradleup.shadow") version "9.3.1"
}

application {
    mainClass.set("com.mdeo.scriptexecution.ApplicationKt")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":script"))
    
    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    
    // Ktor Client (for backend API calls)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    
    // JWT for service-to-service authentication
    implementation("com.auth0:java-jwt:4.5.0")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    
    // Kotlinx
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.shadowJar {
    archiveBaseName.set("script-execution")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}
