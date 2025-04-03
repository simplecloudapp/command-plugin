dependencies {
    compileOnly(libs.simplecloud.controller)
    api(libs.cloud.core)
    api(libs.adventure.api)
    api(libs.adventure.text.minimessage)

    implementation(rootProject.libs.configurate.yaml)
    implementation(rootProject.libs.configurate.kotlin) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }
}