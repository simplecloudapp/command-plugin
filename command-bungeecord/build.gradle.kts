plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    api(project(":command-shared"))
    api(libs.bungeecord.api)
    api(libs.adveture.platform.bungeecord)
    api(libs.cloud.bungeecord)
}

modrinth {
    token.set(project.findProperty("modrinthToken") as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set("qZsNJQis")
    versionNumber.set(rootProject.version.toString())
    versionType.set("beta")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.3",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.2",
        "1.21.3",
        "1.21.4",
        "1.21.5",
    )
    loaders.add("bungeecord")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}