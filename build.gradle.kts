plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
}

tasks.register("clean", Delete::class).configure {
    group = "build"
    description = "Delete the build directory"
    delete(
        rootProject.layout.buildDirectory,
        rootProject.layout.projectDirectory.file("apks")
    )
}