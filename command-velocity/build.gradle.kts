dependencies {
    api(project(":command-shared"))
    api(libs.velocity.api)
    api(libs.cloud.velocity)

    kapt(libs.velocity.api)
}