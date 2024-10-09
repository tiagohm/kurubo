plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kurubo"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("serial", "com.fazecast:jSerialComm:2.11.0")
            library("ws", "org.java-websocket:Java-WebSocket:1.5.7")
            library("jackson", "com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
            library("airline", "com.github.rvesse:airline:3.0.0")
            library("log", "ch.qos.logback:logback-classic:1.5.8")
        }
    }
}

include("app")
include("client")
include("hardware")
include("hub")
include("network")
include("serial")
include("transport")
