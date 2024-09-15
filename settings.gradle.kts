plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kurubo"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("serial", "com.fazecast:jSerialComm:2.11.0")
            library("log", "ch.qos.logback:logback-classic:1.5.8")
        }
    }
}

include("client")
include("hardware")
include("network")
include("serial")
include("transport")
