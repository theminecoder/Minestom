plugins {
    id("minestom.java-library")
    id("minestom.publishing")
}

dependencies {
    api(rootProject)
}

java {
    modularity.inferModulePath = true
}
