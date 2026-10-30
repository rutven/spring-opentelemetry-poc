plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
}

subprojects {
    group = "dev.poc.otel"
    version = "0.0.1-SNAPSHOT"
}
