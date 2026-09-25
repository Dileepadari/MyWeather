import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.weather.android.library)
    alias(libs.plugins.weather.android.compose.library)
}

android {
    namespace = "com.weather.core.testing"
    buildTypes {

    }
    kotlin.compilerOptions.jvmTarget= JvmTarget.JVM_17
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:design"))

    // The BOM travels with the api() dependencies below, because it is what
    // gives them a version - the catalogue entries for the Compose test
    // artifacts deliberately have none.
    //
    // It has to be here rather than only in the compose convention plugin. That
    // plugin adds the BOM to modules that apply it, but the modules consuming
    // this one on their *unit test* classpath (core:common, feature:*) apply
    // the plain library plugin, so they never saw it: every one of them failed
    // to resolve with "Could not find androidx.compose.ui:ui-test-junit4:", an
    // empty version, and testDebugUnitTest had never run for this project.
    api(platform(libs.compose.bom))

    api(libs.compose.ui.test)
    api(libs.compose.ui.testManifest)
    debugApi(libs.androidx.navigation.testing)
    api(libs.androidx.test.core)
    api(libs.kotlix.coroutinesTest)
    api(libs.androidx.test.junit)
    api(libs.junit)
    api(libs.androidx.test.espressoCore)
    api(libs.hilt.androidTesting)

}