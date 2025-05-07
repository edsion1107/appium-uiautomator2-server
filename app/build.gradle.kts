import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import java.io.ByteArrayOutputStream

buildscript {
    dependencies {
        classpath(libs.unmockplugin) {
            exclude(group = "com.android.tools.build", module = "gradle")
        }
    }
}
// Apply UnMock plugin via legacy syntax because it's not properly published
apply(plugin = "de.mobilej.unmock")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.appium.uiautomator2.convention)
//    id("project-report")
}

base {
    archivesName = "appium-uiautomator2"
}

android {
    namespace = "io.appium.uiautomator2.server"
    testOptions {
        unitTests {
            all {
                it.jvmArgs(
                    listOf(
                        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                        "--add-opens", "java.base/java.time=ALL-UNNAMED",
                        "--add-opens", "java.base/java.time.format=ALL-UNNAMED",
                        "--add-opens", "java.base/java.util=ALL-UNNAMED",
                        "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED",
                        "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED",
                        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
                        "--add-opens", "java.base/java.io=ALL-UNNAMED",
                        "--add-opens", "java.base/java.net=ALL-UNNAMED",
                        "--add-opens", "java.base/sun.net.www.protocol.http=ALL-UNNAMED",
                        "--add-exports", "jdk.unsupported/sun.misc=ALL-UNNAMED"
                    )
                )
            }
        }
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/maven/com.google.guava/guava/pom.properties",
                "META-INF/maven/com.google.guava/guava/pom.xml"
            )
        }
    }
    lint {
        abortOnError = false
    }
}
//tasks.withType<JavaCompile> {
//    options.compilerArgs.add("-Xlint:deprecation")
//}
extensions.configure<de.mobilej.unmock.UnMockExtension>("unMock") {
    keepStartingWith("com.android.internal.util.")
    keepStartingWith("android.util.")
    keepStartingWith("android.view.")
    keepStartingWith("android.internal.")
}

dependencies {
    // Local JARs dependency
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))
    // Dependencies using the version catalog (libs)
    implementation(libs.bundles.androix.test)
    implementation(libs.uiautomator)
    implementation(libs.gson)
    implementation(libs.netty.all)
    implementation(libs.junidecode)
    // Dependencies required for XPath search
    implementation(libs.xercesimpl)
    implementation(libs.java.cup.runtime)
    implementation(libs.icu4j)
    // Test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.bundles.powermock)
    testImplementation(libs.robolectric)
    testImplementation(libs.javassist)
    // Android test dependencies
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.okhttp)
}

val installAUT by tasks.register("installAUT", Exec::class) {
    group = "install"
    description = "Install app under test (ApiDemos) using AGP's ADB."
    val extension = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
    // To avoid issues caused by incorrect configuration of the ANDROID_HOME environment variable
    // or version inconsistencies from multiple adb installations.
    val adbFileProvider: Provider<RegularFile> = extension.sdkComponents.adb
    val apkFile = project.file("../node_modules/android-apidemos/apks/ApiDemos-debug.apk")
    val targetSerial = System.getenv("ANDROID_SERIAL")
    inputs.file(apkFile)
        .withPathSensitivity(PathSensitivity.ABSOLUTE)
        .withPropertyName("autApkInput")
        .skipWhenEmpty(false)

    doFirst {
        if (!apkFile.exists()) {
            throw GradleException("Required AUT APK not found at: ${apkFile.absolutePath}")
        }
        executable = adbFileProvider.get().asFile.absolutePath
        val commandArgs = mutableListOf<String>()

        if (!targetSerial.isNullOrBlank()) {
            commandArgs.add("-s")
            commandArgs.add(targetSerial)
            logger.quiet("Installing to device: $targetSerial")
        }
        val apiLevel: Int = runCatching {
            val getPropCommand = mutableListOf<String>()
            getPropCommand.addFirst(adbFileProvider.get().asFile.absolutePath)
            getPropCommand.addAll(commandArgs)
            getPropCommand.addAll(listOf("shell", "getprop", "ro.build.version.sdk"))
            ProcessBuilder(getPropCommand)
                .start()
                .inputStream.bufferedReader().use { it.readText() }.trim().toIntOrNull()
        }.getOrNull() ?: 23

        commandArgs.add("install")
        if (apiLevel >= 23) {
            commandArgs.add("-g")
        }
        commandArgs.add("-r")
        commandArgs.addLast(apkFile.absolutePath)
        setArgs(commandArgs)
        isIgnoreExitValue = false
        errorOutput = ByteArrayOutputStream()
        standardOutput = ByteArrayOutputStream()
    }

    doLast {
        logger.info("exitValue: ${executionResult.get().exitValue},\nstandardOutput: $standardOutput,\nerrorOutput: $errorOutput")
    }
}
val uninstallAUT by tasks.register("uninstallAUT", Exec::class) {
    group = "install"
    description = "Uninstall app under test (ApiDemos) using AGP's ADB."
    val extension = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
    val adbFileProvider: Provider<RegularFile> = extension.sdkComponents.adb
    val targetSerial = System.getenv("ANDROID_SERIAL")
    doFirst {
        executable = adbFileProvider.get().asFile.absolutePath
        val commandArgs = mutableListOf<String>()
        if (!targetSerial.isNullOrBlank()) {
            commandArgs.add("-s")
            commandArgs.add(targetSerial)
            logger.quiet("Uninstalling to device: $targetSerial")
        }
        commandArgs.addAll(listOf("uninstall", "io.appium.android.apis"))
        setArgs(commandArgs)
        isIgnoreExitValue = true
    }
}

afterEvaluate {
//    tasks.named("connectedE2eTestDebugAndroidTest").configure {
//        dependsOn(installAUT)
//    }
    tasks.named("uninstallAll").configure {
        dependsOn(uninstallAUT)
    }

}
