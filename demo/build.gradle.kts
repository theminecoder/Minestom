import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("minestom.java-binary")
}

dependencies {
    implementation(rootProject)
    implementation(project(":protocol-java"))
    implementation(project(":protocol-bedrock"))

    runtimeOnly(libs.bundles.logback)
}

application {
    mainClass.set("net.minestom.demo.Main")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("minestom-demo.jar")
}
