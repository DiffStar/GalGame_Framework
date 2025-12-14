pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    
    val props = java.util.Properties().apply {
        java.io.File(rootDir, "gradle.properties").inputStream().use { load(it) }
    }
    extra["neoforgeModdevVersion"] = props.getProperty("neoforge_moddev_version") ?: "2.0.124"
    extra["kotlinVersion"] = props.getProperty("kotlin_version") ?: "2.1.10"
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

