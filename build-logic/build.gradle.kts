plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spotless.plugin.gradle)
    implementation(libs.errorprone.plugin.gradle)
}
