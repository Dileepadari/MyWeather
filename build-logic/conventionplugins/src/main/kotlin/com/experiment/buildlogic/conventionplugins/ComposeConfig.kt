package com.experiment.buildlogic.conventionplugins

import com.android.build.api.dsl.CommonExtension
import getLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.composeBuildConfiguration(
    commonExtension: CommonExtension,
) {
    commonExtension.buildFeatures.compose = true
    dependencies {
        val composeBom = getLibs.findLibrary("compose-bom").get()
        add("implementation", platform(composeBom))
        add("androidTestImplementation", platform(composeBom))
        // testImplementation too. core:testing exposes the Compose test
        // artifacts with api() and the catalogue gives them no version, because
        // the BOM is meant to supply it. Without the BOM on this configuration
        // any module whose unit tests depend on core:testing failed to resolve
        // with "Could not find androidx.compose.ui:ui-test-junit4:" - note the
        // empty version - so testDebugUnitTest had never run for this project.
        add("testImplementation", platform(composeBom))
    }
}