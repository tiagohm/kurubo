plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":transport"))
    api(libs.serial)
    implementation(libs.log)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
