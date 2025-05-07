plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.appium.uiautomator2.convention)
}
//java {
//    // Ensures JDK 22+ Adoptium consistency across CI environments and avoids vendor-specific build issues
//    toolchain {
//        languageVersion = JavaLanguageVersion.of(22)
//        vendor = JvmVendorSpec.ADOPTIUM
//    }
//}
//val buildTime = BuildConfigField(
//    "String", "\"${System.currentTimeMillis()}\"", "build timestamp"
//)
project.base.archivesName = "appium-uiautomator2-e2etest" // apk filename
android {
    namespace = "io.appium.uiautomator2.e2etest"
    targetProjectPath = projects.app.path
//    defaultConfig {
//        compileSdk = 34
//        minSdk = 21
//        targetSdk = 34
////        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
////        buildFeatures {
////            buildConfig = true
////        }
//    }
//    androidComponents {
//        onVariants { variant ->
//            variant.buildConfigFields.put("BUILD_TIME", buildTime)
//        }
//    }

//    buildTypes{
//        getByName("debug"){
//            isDebuggable = true
//            vcsInfo {
//                include = true
//            }
//        }
//    }
    kotlin {

    }
}
//
dependencies {
    implementation(libs.netty.all)
    implementation(libs.okhttp)
    implementation(libs.bundles.androix.test)
    implementation(libs.uiautomator)
}