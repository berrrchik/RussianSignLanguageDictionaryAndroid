import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

val composeBomVersion = "2024.12.01"
val coroutinesVersion = "1.8.1"
val mockkVersion = "1.13.12"
val turbineVersion = "1.1.0"
val okhttpVersion = "4.12.0"
val robolectricVersion = "4.14.1"
val androidxTestCoreVersion = "1.6.1"
val androidxTestRunnerVersion = "1.6.2"
val androidxTestRulesVersion = "1.6.1"

val coverageExclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/*_Factory.class",
    "**/*_Factory$*.class",
    "**/*_Provide*Factory*.*",
    "**/*_MembersInjector.class",
    "**/Dagger*.*",
    "**/Hilt_*.*",
    "**/*Hilt*.*",
    "**/*_HiltModules*.*",
    "**/*_ComponentTreeDeps*.*",
    "**/*_GeneratedInjector*.*",
    "**/*_AggregatedDeps*.*",
    "**/dagger/hilt/internal/aggregatedroot/codegen/**",
    "**/hilt_aggregated_deps/**",
    "**/*ComposableSingletons*.*",
    "**/*Preview*.*",
    "**/*\$inlined\$*.*",
    "**/*\$serializer*.*",
    "**/services/cache/VideoCacheDownloader*.*",
    "**/repositories/endpoints/SyncEndpoints*.*",
    "**/utilities/network/NetworkAddressValidator*.*",
    "**/ui/components/SynonymListView*.*"
)

android {
    namespace = "com.rsl.dictionary"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rsl.dictionary"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.rsl.dictionary.testing.HiltTestRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("test") {
            java.srcDir("src/sharedTest/kotlin")
        }
        getByName("androidTest") {
            java.srcDir("src/sharedTest/kotlin")
        }
    }

    testOptions {
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.core:core-splashscreen:1.0.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:$androidxTestCoreVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("app.cash.turbine:turbine:$turbineVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
    testImplementation("org.robolectric:robolectric:$robolectricVersion")
    testImplementation("com.google.dagger:hilt-android-testing:${libs.versions.hilt.get()}")
    kspTest(libs.hilt.compiler)

    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core-ktx:$androidxTestCoreVersion")
    androidTestImplementation("androidx.test:runner:$androidxTestRunnerVersion")
    androidTestImplementation("androidx.test:rules:$androidxTestRulesVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    androidTestImplementation("app.cash.turbine:turbine:$turbineVersion")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:$okhttpVersion")
    androidTestImplementation("com.google.dagger:hilt-android-testing:${libs.versions.hilt.get()}")
    kspAndroidTest(libs.hilt.compiler)
}

tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class.java) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("unitTestCoverageReport") {
    dependsOn("testDebugUnitTest")

    group = "verification"
    description = "Generates a JaCoCo coverage report for debug unit tests only (no device required)."

    val buildDirFile = layout.buildDirectory.get().asFile

    classDirectories.setFrom(
        files(
            fileTree("${buildDirFile}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
                exclude(coverageExclusions)
            },
            fileTree("${buildDirFile}/intermediates/javac/debug/classes") {
                exclude(coverageExclusions)
            },
            fileTree("${buildDirFile}/tmp/kotlin-classes/debug") {
                exclude(coverageExclusions)
            }
        )
    )

    sourceDirectories.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin"
        )
    )

    executionData.setFrom(
        fileTree(buildDirFile) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
            )
        }
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    doFirst {
        layout.buildDirectory
            .dir("reports/jacoco/unitTestCoverageReport")
            .get()
            .asFile
            .deleteRecursively()
    }
}

tasks.register<JacocoReport>("mergedDebugCoverageReport") {
    dependsOn("testDebugUnitTest", "connectedDebugAndroidTest")

    group = "verification"
    description = "Generates a merged JaCoCo coverage report for debug unit and android tests."

    val buildDirFile = layout.buildDirectory.get().asFile

    classDirectories.setFrom(
        files(
            fileTree("${buildDirFile}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
                exclude(coverageExclusions)
            },
            fileTree("${buildDirFile}/intermediates/javac/debug/classes") {
                exclude(coverageExclusions)
            },
            fileTree("${buildDirFile}/tmp/kotlin-classes/debug") {
                exclude(coverageExclusions)
            }
        )
    )

    sourceDirectories.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin"
        )
    )

    executionData.setFrom(
        fileTree(buildDirFile) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec"
            )
        }
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    doFirst {
        layout.buildDirectory
            .dir("reports/jacoco/mergedDebugCoverageReport")
            .get()
            .asFile
            .deleteRecursively()
    }
}
