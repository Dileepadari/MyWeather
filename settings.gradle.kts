pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// Lets Gradle download the JDK the project asks for rather than requiring one
// to be installed already.
//
// The daemon JVM was already auto-provisioned through
// gradle/gradle-daemon-jvm.properties, but the *project* toolchain - jvmToolchain(17),
// set in build-logic - had no download repository, so on a machine without a
// JDK 17 the build started happily and then failed at
// :app:compileDebugJavaWithJavac with "Toolchain download repositories have not
// been configured".
//
// pluginManagement must come first in this file, then plugins, then
// dependencyResolutionManagement. Gradle rejects any other order.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "weather"
include(":app")
include(":benchmark")

include(":core:database")
include(":core:model")
include(":core:network")
include(":core:repository")
include(":core:design")
include(":feature:search")
include(":core:datastore")
include(":core:common")
include(":core:testing")

include(":sync:work")

include(":feature:forecast")
include(":feature:settings")
include(":feature:managelocations")
