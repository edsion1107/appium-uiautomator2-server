plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.tools.common)
    compileOnly(libs.android.gradlePlugin)
//    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlinAndroid.gradlePlugin)
//    compileOnly(libs.compose.gradlePlugin)
//    compileOnly(libs.firebase.crashlytics.gradlePlugin)
//    compileOnly(libs.firebase.performance.gradlePlugin)

//    compileOnly(libs.ksp.gradlePlugin)
//    compileOnly(libs.room.gradlePlugin)
//    implementation(libs.truth)
//    lintChecks(libs.androidx.lint.gradle)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("AndroidConventionPlugin") {
            id = "AndroidConventionPlugin"
            implementationClass = "io.appium.uiautomator2.convention.AndroidConventionPlugin"
        }
    }
}
