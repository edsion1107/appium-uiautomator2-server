package io.appium.uiautomator2.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.TestPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import javax.inject.Inject


abstract class AndroidConventionPlugin : BasePlugin() {

    @get:Inject
    abstract val project: Project

    private val versionNameProperty: String by lazy {
        project.findProperty("versionName")?.toString()?.trim {
            it.isWhitespace() || it == '\"' || it == '\''
        } ?: "1.0.0-SNAPSHOT"
    }.also {
        project.logger.info("versionName: {}", this)
    }

    private val versionCodeProperty: Int by lazy {
        project.findProperty("versionCode")?.toString()?.trim()?.toInt() ?: 1
    }.also {
        project.logger.info("versionCode: {}", this)
    }
    private val warningsAsErrorsProperty by lazy {
        project.providers.gradleProperty("warningsAsErrors").map {
            it.toBoolean()
        }.orElse(false)
    }.also {
        project.logger.info("warningsAsErrors: {}", this)
    }


    override fun apply(project: Project) {
        with(project) {
            configJavaPlugin()
            configApplication()
            configAndroidTest()
            plugins.apply("org.jetbrains.kotlin.android")
            configureKotlin()
        }
    }

    /**
     * Converts a [JavaVersion] to a [JvmTarget] or [JavaLanguageVersion].
     * Note: May throw [IllegalArgumentException] for unsupported JDK versions like 1.7.
     */
    private inline fun <reified T> JavaVersion.toVersion(): T {
        return when (T::class) {
            JvmTarget::class -> if (isJava8) {
                JvmTarget.JVM_1_8
            } else {
                JvmTarget.fromTarget(majorVersion)
            }

            JavaLanguageVersion::class -> JavaLanguageVersion.of(majorVersion)
            else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
        } as T
    }

    private fun Project.configJavaPlugin() {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                // The IDE typically provides GRADLE_LOCAL_JAVA_HOME (currently JBR 21). The javaToolchains plugin detects this JDK only when configured to use it.
                // Recommended: Set Gradle JDK to GRADLE_LOCAL_JAVA_HOME in Settings -> Build, Execution, Deployment -> Build Tools -> Gradle.
                // The toolchain plugin configures the JDK for local and CI builds. Note: build-logic can omit toolchain configuration to prevent inconsistencies from multiple configurations.

                languageVersion.set(project.javaLanguageVersion.toVersion<JavaLanguageVersion>())
//                vendor.set(JvmVendorSpec.JETBRAINS)
            }
        }
    }

    /**
     * Configuration for projects applying the `com.android.application` plugin.
     */
    private fun Project.configApplication() {
        plugins.findPlugin(AppPlugin::class.java)?.apply {
            extensions.configure<ApplicationExtension> {
                configAndroidCommon(this).also {
                    defaultConfig {
                        targetSdk = project.targetSdk
                        versionCode = versionCodeProperty
                        versionName = versionNameProperty
                    }
                }
            }

            extensions.configure<ApplicationAndroidComponentsExtension> {
                configAndroidComponentsExtension(this).also {
                    onVariants { variant ->
                        variant.outputs.forEach {
                            if (it is VariantOutputImpl) {
                                val fileNameWithVersion = it.outputFileName.get()
                                    .replace(".apk", "-v${it.versionName.get()}.apk")
                                it.outputFileName.set(fileNameWithVersion)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Project.configAndroidTest() {
        plugins.findPlugin(TestPlugin::class.java)?.apply {
            extensions.configure<TestExtension> {
                configAndroidCommon(this).also {
                    defaultConfig {
                        targetSdk = project.targetSdk
                    }
                }
            }
            extensions.configure<TestAndroidComponentsExtension> {
                configAndroidComponentsExtension(this).also {
                    onVariants { _ ->
//                        variant.instrumentationRunnerArguments.set(
//                            mapOf(
//                                "notAnnotation" to "androidx.test.filters.FlakyTest"
//                            )
//                        )
                    }
                }
            }
        }
    }

    //    private fun Project.configKotlinAndroid() {
//        if (!project.plugins.hasPlugin("org.jetbrains.kotlin.android")) {
//            return
//        }
//        extensions.configure<KotlinAndroidProjectExtension> {
//            compilerOptions {
//                jvmTarget = getVersion<JvmTarget>()
//                allWarningsAsErrors = warningsAsErrors
//                freeCompilerArgs.add(
//                    // Enable experimental coroutines APIs, including Flow
//                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
//                )
//            }
//        }
//    }
    private fun Project.configAndroidCommon(commonExtension: CommonExtension<*, *, *, *, *, *>) =
        commonExtension.apply {
            defaultConfig {
                minSdk = project.minSdk
                compileSdk = project.compileSdk
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions {
                    unitTests {
                        isIncludeAndroidResources = true
                        isReturnDefaultValues = true
                    }
                }
            }
            buildTypes.forEach {
                // add git revision to `META-INF/version-control-info.textproto`,not support for AndroidTest apk
                it.vcsInfo {
                    include = true
                }
            }
            buildFeatures {
                buildConfig = true
            }

        }

    private fun Project.configAndroidComponentsExtension(androidComponentsExtension: AndroidComponentsExtension<*, *, *>) =
        androidComponentsExtension.apply {
            onVariants { variant ->
                variant.buildConfigFields.put(
                    "BUILD_TIME",
                    BuildConfigField("String", "\"${project.buildTime}\"", "build time(UTC)")
                )
            }
        }

    private fun Project.configureKotlin() {
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(project.javaLanguageVersion.toVersion<JvmTarget>())
                languageVersion.set(project.kotlinLanguageVersion)
                allWarningsAsErrors.set(warningsAsErrorsProperty)
            }
        }
    }
}
