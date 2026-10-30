rootProject.name = "spring-opentelemetry-poc"

include("gateway", "orders")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
