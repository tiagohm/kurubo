import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.3"
}

dependencies {
    implementation(project(":hub"))
    implementation(libs.airline)
    implementation(libs.log)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    isZip64 = true

    archiveFileName.set("kurubo.jar")
    destinationDirectory.set(file("../"))

    manifest {
        attributes["Main-Class"] = "br.tiagohm.kurubo.app.MainKt"
    }
}

