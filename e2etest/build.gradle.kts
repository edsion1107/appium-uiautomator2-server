import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.appium.uiautomator2.convention)
    alias(libs.plugins.project.report)
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
        doFirst {
            val deviceProvider =
                deviceProviderFactory.getDeviceProvider(androidComponents.sdkComponents.adb, null)
            deviceProvider.init()
            deviceProvider.devices.forEach { device ->
                logger.quiet("device: $device,${device.apiLevel},${device.serialNumber}")

                val options = if (device.apiLevel >= 23) {
                    listOf("-d", "-t", "-g")
                } else {
                    listOf("-d", "-t")
                }
                device.installPackage(
                    file("../node_modules/android-apidemos/apks/ApiDemos-debug.apk"),
                    options,
                    10_000,
                    LoggerWrapper.getLogger(DeviceProviderInstrumentTestTask::class.java)
                )
            }
        }
    }
}
