import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
import io.appium.uiautomator2.convention.InstallTask
import io.appium.uiautomator2.convention.UninstallTask

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.appium.uiautomator2.convention)
    alias(libs.plugins.adb.plugin)
//    alias(libs.plugins.project.report)
}

project.base.archivesName = "appium-uiautomator2-e2etest" // apk filename
android {
    namespace = "io.appium.uiautomator2.e2etest"
    targetProjectPath = projects.app.path
}
dependencies {
    implementation(libs.netty.all)
    implementation(libs.okhttp)
    implementation(libs.bundles.androix.test)
    implementation(libs.uiautomator)
}
afterEvaluate {
    tasks.withType(DeviceProviderInstrumentTestTask::class.java) {
        dependsOn(tasks.withType(InstallTask::class.java))
    }
    tasks.named("uninstallAll").configure {
        dependsOn(tasks.withType(UninstallTask::class.java))
    }
}