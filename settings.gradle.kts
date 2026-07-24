plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("command-shared", "command-velocity", "command-bungeecord")

rootProject.name = "command-plugin"