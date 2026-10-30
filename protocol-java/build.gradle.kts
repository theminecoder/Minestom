plugins {
    id("minestom.java-library")
    id("minestom.publishing")
}

dependencies {
    api(rootProject)
    implementation(libs.jcTools)
}

java {
    modularity.inferModulePath = true
}
