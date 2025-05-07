apply(from = "./gradle/settings/convention.settings.gradle.kts")
rootProject.name = "appium-uiautomator2-server"

includeBuild("build-logic")
include(":app")
include(":e2etest")
