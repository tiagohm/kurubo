plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":client"))
    implementation(libs.log)
    testImplementation(project(":serial"))
    testImplementation(project(":network"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
