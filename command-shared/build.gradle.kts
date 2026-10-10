dependencies {
    compileOnly(libs.simplecloud.api)
    compileOnly(libs.slf4j.api)
    implementation(libs.adventure.api)
    implementation(libs.adventure.text.minimessage)
    implementation(libs.simplecloud.plugin)
    implementation(libs.bundles.configurate)
    implementation(libs.bundles.cloud.command)
}