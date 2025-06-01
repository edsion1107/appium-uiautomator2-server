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
    alias(libs.plugins.project.report)
}

base {
    archivesName = "appium-uiautomator2-server"
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
