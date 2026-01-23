plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    
    // Apache Commons Collections for Bag implementation
    implementation("org.apache.commons:commons-collections4:4.4")
    
    // ASM for bytecode generation
    implementation("org.ow2.asm:asm:9.6")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.2")
}
