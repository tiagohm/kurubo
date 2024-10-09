plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":hardware"))
    api(project(":serial"))
    api(project(":network"))
    api(libs.ws)
    api(libs.jackson)
    implementation(libs.log)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
