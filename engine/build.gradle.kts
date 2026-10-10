plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

// Pure Kotlin/JVM engine. Must stay free of any android.* imports.
kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
}

// Jacoco uses Gradle's bundled tool; no third-party coverage dependency (plan P0.9 AC >= 85% lines).
tasks.test {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
    }
}
