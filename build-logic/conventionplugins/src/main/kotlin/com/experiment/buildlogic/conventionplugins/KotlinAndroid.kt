package com.experiment.buildlogic.conventionplugins

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension


internal fun Project.configureAndroid(
    extension: CommonExtension,
) {
    extension.compileSdk = 36
    extension.defaultConfig.minSdk = 26
    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_17

    configure<KotlinTopLevelExtension> {
        jvmToolchain(17)
    }
}
