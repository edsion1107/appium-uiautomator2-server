package io.appium.uiautomator2.convention

import com.android.build.api.variant.AndroidComponents
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
import com.android.build.gradle.internal.tasks.NonIncrementalTask
import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.android.ddmlib.InstallReceiver
import com.android.ddmlib.TimeoutRemainder
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A Gradle plugin that provides tasks for interacting with Android Debug Bridge (ADB).
 */
abstract class ADBPlugin : BasePlugin() {

    override fun apply(target: Project) {
        configLogLevel(target)
        with(target) {
            val androidComponents = project.extensions.findByType(AndroidComponents::class.java)
                ?: throw IllegalStateException("This plugin requires an Android plugin (e.g., com.android.application)")
            tasks.register("installAUT", InstallTask::class.java) {
                group = "install"
                description = "Install app under test (ApiDemos) using AGP's ADB."
                apkFilePath.set(file("../node_modules/android-apidemos/apks/ApiDemos-debug.apk"))
                adbExecutable.set(androidComponents.sdkComponents.adb.map { it.asFile })
            }
            tasks.register("uninstallAUT", UninstallTask::class.java) {
                group = "install"
                description = "Uninstall app under test (ApiDemos) using AGP's ADB."
                packageName.set("io.appium.android.apis")
                adbExecutable.set(androidComponents.sdkComponents.adb.map { it.asFile })
            }

        }
    }

    /**
     * Configures the log level for `ddmlib` based on the Gradle project's logger debug level.
     *
     * @param project The Gradle project.
     */
    private fun configLogLevel(project: Project) {
        with(project.logger) {
            if (isDebugEnabled) {
                DdmPreferences.setLogLevel("debug")
            } else if (isInfoEnabled) {
                DdmPreferences.setLogLevel("info")
            } else if (isWarnEnabled) {
                DdmPreferences.setLogLevel("warn")
            } else {
                DdmPreferences.setLogLevel("error")
            }
        }
    }
}

/**
 * Base abstract class for ADB-related Gradle tasks.
 * This class handles the initialization and termination of `AndroidDebugBridge`.
 */
abstract class ADBBaseTask : NonIncrementalTask() {
    init {
        /**
         * Ensures that all tasks are always executed, regardless of incremental builds,
         * build cache, or configuration cache.
         */
        outputs.cacheIf { false }
        outputs.upToDateWhen { false }
    }

    /**
     * The path to the ADB executable.
     * It is generally recommended to use `androidComponents.sdkComponents.adb` to avoid incompatibility issues caused by multiple ADB versions configured in the `PATH` during local development.
     */
    @get:Input
    abstract val adbExecutable: Property<File>
    
    /**
     * Lazily initialized `AndroidDebugBridge` instance.
     * It creates a bridge using the provided ADB executable path.
     */
    @get:Internal
    val bridge: AndroidDebugBridge by lazy {
        runCatching {
            // Initialization must happen before createBridge; placing it in the doFirst phase might not be effective during cold starts.
            // Also, be careful not to call AndroidDebugBridge.terminate() in custom tasks, as this could cause errors in AGP's internal ddmlib calls.
            AndroidDebugBridge.init(false)
        }.onFailure {
            // If run in the root module, duplicate initialization might occur.
            logger.warn(it.message, it)
        }.getOrNull()
        requireNotNull(runCatching {
            AndroidDebugBridge.createBridge(
                adbExecutable.get().absolutePath,
                false,
                10,
                TimeUnit.SECONDS
            )
        }
            .onSuccess {
                logger.info("bridge: $it")
            }
            .onFailure {
                logger.warn(it.message, it)
            }.getOrElse {
                AndroidDebugBridge.createBridge(
                    adbExecutable.get().absolutePath,
                    true,
                    10,
                    TimeUnit.SECONDS
                )
            }) {
            "Failed to create AndroidDebugBridge"
        }
    }


    /**
     * Finds and returns a list of online Android devices.
     * It waits for an initial device list from the bridge with a timeout.
     *
     * @return A list of `IDevice` objects that are currently online.
     */
    fun findOnlineDevices(): List<IDevice> {
        val timeout = TimeoutRemainder(10, TimeUnit.SECONDS)

        while (timeout.remainingNanos > 0) {
            if (!bridge.hasInitialDeviceList()) {
                logger.info("[${timeout.remainingNanos}]hasInitialDeviceList: ${bridge.hasInitialDeviceList()}")
                Thread.sleep(100)
            } else {
                return bridge.devices.map { device ->
                    logger.info("serialNumber: ${device.serialNumber}, apiLevel: ${device.version.apiLevel}, state: ${device.state}")
                    device
                }.filter { it.isOnline }
            }
        }
        return emptyList()
    }

}

/**
 * A Gradle task for installing an APK on connected Android devices.
 * This task uses `ddmlib` to perform the installation.
 */
abstract class InstallTask() : ADBBaseTask() {

    /**
     * The path to the APK file to be installed.
     */
    @get:InputFile
    abstract val apkFilePath: RegularFileProperty

    /**
     * The action performed by this task.
     * It finds online devices and installs the APK on each of them.
     * Handles different API levels for installation flags.
     * Asserts the success of the installation and logs install metrics.
     */
    @TaskAction
    override fun doTaskAction() {
        findOnlineDevices().forEach { device ->
            val result = InstallReceiver()
            if (device.version.apiLevel >= 23) {
                device.installPackage(
                    apkFilePath.get().asFile.absolutePath,
                    true,
                    result,
                    "-d", "-g"
                )
            } else {
                device.installPackage(
                    apkFilePath.get().asFile.absolutePath,
                    true,
                    result,
                    "-d",
                )
            }
            assert(result.isSuccessfullyCompleted) {
                result.errorMessage
            }
            logger.info(
                """[installMetrics]
                |installStartNs: ${device.lastInstallMetrics.installStartNs},
                |installFinishNs: ${device.lastInstallMetrics.installFinishNs},
                |uploadStartNs: ${device.lastInstallMetrics.uploadStartNs},
                |uploadFinishNs: ${device.lastInstallMetrics.uploadFinishNs}""".trimMargin()
            )
        }
    }
}

/**
 * A Gradle task for uninstalling an application package from connected Android devices.
 * This task uses `ddmlib` to perform the uninstallation.
 */
abstract class UninstallTask() : ADBBaseTask() {
    /**
     * The package name of the application to be uninstalled.
     */
    @get:Input
    abstract val packageName: Property<String>

    /**
     * The action performed by this task.
     * It finds online devices and uninstalls the specified package from each of them.
     * Logs the uninstallation result.
     */
    @TaskAction
    override fun doTaskAction() {
        findOnlineDevices().forEach { device ->
            val result = device.uninstallPackage(packageName.get())
            logger.info("uninstall `${packageName.get()}`: $result")
        }
        logger.info("uninstall `${packageName.get()}`")
    }
}
