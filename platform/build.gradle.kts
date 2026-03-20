plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "com.mdeo"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
            freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        }
    }

    tasks.withType<Test> {
        // Tests tagged "performance" run only when -Pperformance is passed on the Gradle CLI.
        // This keeps the normal `./gradlew test` fast while still allowing the full suite
        // (including long-running optimization benchmarks) to run on demand.
        useJUnitPlatform {
            if (!project.hasProperty("performance")) {
                excludeTags("performance")
            }
        }

        // MOEA Framework's Instrumenter uses deep reflection; open JDK internal modules
        jvmArgs(
            "--add-opens", "java.xml/org.xml.sax.helpers=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
        )
    }
}
