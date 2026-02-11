dependencies {
    compileOnly(libs.simplecloud.api)
    api(libs.cloud.core)
    api(libs.adventure.api)
    api(libs.adventure.text.minimessage)

    implementation(libs.bundles.logging)
    implementation(libs.bundles.configurate)
}