# not_for_you.md

A working log for MyWeather. The reasoning that did not earn a place in DEVDOC.

---

## The one that shows on screen

`searchLocation` in `WeatherRepositoryImpl`:

```kotlin
flow {
    val remoteData = remoteWeather.directGeocode(cityName = cityName)
    if (remoteData.isNotEmpty())
        emit(remoteData)
}.catch {
    Timber.e("search error: ${it.message}")
}
```

Two ways to produce no emission at all: an empty result, and an error.

That matters because of how it is consumed. `SearchViewModel` does
`searchQuery.debounce(500).flatMapLatest { searchLocation(it) } ... .stateIn(...)`. A `stateIn`
holds its last value. So a flow that never emits leaves the previous query's results sitting
there.

Type "London", get London. Type "zzzzqqq", get **London**, still. Nothing says the second search
happened, nothing says it found nothing, and `showPlaceholder` is only ever set to false by an
emission, so the empty-state placeholder never returns either. A network failure looks identical,
because `directGeocode` catches and returns an empty list.

The fix is to emit unconditionally, and to emit an empty list from the `catch` too. "Found
nothing" is a result and the UI is entitled to hear it.

Underneath it, in `directGeocode`:

```kotlin
.getOrNull()!!
```

`getOrNull()` exists to hand back null instead of throwing. `!!` throws on null. Together they
ask for a null and then throw on the one they asked for, and the only reason the app did not
crash is that a broad `catch (e: Exception)` was wrapped around the whole thing and turned it
into `emptyList()`. It works by accident. `getOrElse { emptyList() }` says the same thing on
purpose.

## The test I decided not to write

I wrote one. Then I read the flow again:

```kotlin
.flatMapLatest { ... }
.flowOn(Dispatchers.IO)
```

`Dispatchers.IO` is a real dispatcher, not the test scheduler, so `advanceTimeBy` and
`advanceUntilIdle` do not wait for it. Any assertion after them is a race. And I cannot run the
test locally to find out how often it loses - there is no Android SDK on this machine, so the
module will not even build here.

Shipping a test I cannot run, that is racy by construction, into a CI job that will then flake
for someone else, is worse than shipping no test. I reverted it and wrote the gap into DEVDOC
instead, with the reason.

The honest fix is to hoist the dispatcher out of the flow and inject it, which is a change to the
ViewModel's shape rather than a test. Worth doing; not worth doing blind.

What I did keep: `FakeWeatherRepository.searchLocation` was `TODO()`, meaning any test that
touched it threw. It now returns a settable list, so the next person has something to drive.

## Why this repo builds here and Foodie does not

Both are Android, both need a JDK newer than the Java 8 on this machine. MyWeather gets much
further, and the difference is worth writing down because it is the same lesson twice.

Foodie hard-coded `org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64` - one machine's
filesystem. MyWeather ships `gradle/gradle-daemon-jvm.properties` with foojay URLs per platform,
so **Gradle downloads its own daemon JVM 21**. `./gradlew --version` works here on a Java 8
launcher, which is a genuinely nice piece of setup.

But the build still failed:

```
Cannot find a Java installation on your machine matching: {languageVersion=17, ...}
Toolchain download repositories have not been configured.
```

The *daemon* JVM is provisioned. The *project* toolchain - `jvmToolchain(17)`, set in
`build-logic` - is a separate mechanism and had no download repository. So the build configured
cleanly, resolved dependencies, and died at the first `compileDebugJavaWithJavac` on any machine
without a JDK 17 lying around.

One plugin fixes it:

```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

and then the build got past the toolchain entirely and failed on "SDK location not found", which
is an honest environmental gap rather than a project one.

**That is as far as verification goes here.** I cannot assemble the APK; CI does. But the failure
moving from "cannot find a JDK" to "cannot find the Android SDK" is real evidence the fix works,
and it is more than I could say for Foodie.

Two things bit me putting it in: `settings.gradle.kts` requires `pluginManagement`, then
`plugins`, then `dependencyResolutionManagement`, in that order, and this file had
`dependencyResolutionManagement` first. My initial edit put the `plugins` block at the top and
would not have configured at all. And while I was in there, `include(":feature:settings")`
appeared twice - harmless, Gradle dedupes, but it is still a line that says something untrue.

## Two things only CI could tell me

I could not build this locally, so the first push was also the first compile. Both of these came
back from CI, and both were worth having.

**`testDebugUnitTest` had never run.** Not "was failing" - had never run:

```
Could not find androidx.compose.ui:ui-test-junit4:.
```

Note the empty version. `core:testing` exposes the Compose test artifacts with `api()` and the
catalogue gives them no version, because the Compose BOM supplies it. But the convention plugin
added the BOM to `implementation` and `androidTestImplementation` only, and the modules pulling
`core:testing` onto their *unit test* classpath - `core:common`, the features - apply the plain
library plugin, which never runs that code.

My first fix added `testImplementation` to the convention plugin. It did not work, for exactly
the reason above: `core:common` does not apply the compose plugin at all. The fix that works puts
`api(platform(libs.compose.bom))` in `core:testing`, next to the dependencies it versions, so the
constraint travels with them to any consumer on any configuration. Seven tests across five
modules run now.

**A lint error had been sitting in `feature/settings`.** `LocalContextGetResourceValueCall`:
reading a string through `LocalContext.current.getString` rather than `stringResource`. That one
is a real defect and not just a style rule - a string read that way does not follow a locale
change, so the temperature and wind-speed symbols would keep the old language until the screen
was rebuilt. `stringResource` is composable and `LaunchedEffect`'s body is not, so the values are
resolved just above the effect and used inside it.

## The README, again

Same call as Foodie: the badge block is good and stays, the emoji headings and the arrow glyph go,
because the no-emoji rule applies to every file and every other repo in this job has had them
removed. Structure and wording are otherwise the author's.

## Decisions I am not relitigating

- **The licence stays Apache-2.0.** The job's default is MIT, but relicensing is the author's call
  and nobody has made it. Leaving it is the option that needs no answer; the badge says Apache-2.0
  so at least the README and the LICENSE file agree now.
- **Detekt and Kotlinter stay out of CI for now.** Both are configured. Adding `./gradlew detekt`
  to a build I cannot run locally means discovering the size of the existing baseline through a
  red CI run, which is a poor way to find out. Written into Known gaps instead.
- **The `catch (e: Exception)` blocks stay broad.** There are three, all of them logging through
  Timber and degrading to something sensible. Narrowing them is a real improvement and a separate
  pass; the one I touched now emits rather than silently swallowing, which was the actual defect.
