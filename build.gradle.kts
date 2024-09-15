plugins {
    kotlin("jvm") version "2.0.20"
}

allprojects {
    group = "br.tiagohm.kurubo"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

kotlin {
    jvmToolchain(17)
}
