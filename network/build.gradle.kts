plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":transport"))
    implementation(libs.log)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
