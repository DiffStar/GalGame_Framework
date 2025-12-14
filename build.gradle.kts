buildscript {
    val props = java.util.Properties().apply {
        file("gradle.properties").inputStream().use { load(it) }
    }
    extra["neoforgeModdevVersion"] = props.getProperty("neoforge_moddev_version") ?: "2.0.124"
    extra["kotlinVersion"] = props.getProperty("kotlin_version") ?: "2.1.10"
}

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev") version (extra["neoforgeModdevVersion"] as String)
    id("org.jetbrains.kotlin.jvm") version (extra["kotlinVersion"] as String)
    idea
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

version = project.findProperty("mod_version") as String
group = project.findProperty("mod_group_id") as String

repositories {
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

base {
    archivesName.set(project.findProperty("mod_id") as String)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

neoForge {
    version = project.findProperty("neo_version") as String

    parchment {
        mappingsVersion = project.findProperty("parchment_mappings_version") as String
        minecraftVersion = project.findProperty("parchment_minecraft_version") as String
    }

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", project.findProperty("mod_id") as String)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", project.findProperty("mod_id") as String)
        }

        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", project.findProperty("mod_id") as String)
        }

        create("data") {
            clientData()
            programArguments.addAll(
                "--mod", project.findProperty("mod_id") as String,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
            systemProperty("jdk.module.illegalAccess", "permit")
            systemProperty("jdk.reflect.useDirectMethodHandle", "false")
        }
    }

    mods {
        register(project.findProperty("mod_id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.main.get().resources.srcDir("src/generated/resources")

val localRuntime by configurations.creating {
    description = "Runtime dependencies that are optional and won't be pulled by dependents"
}

configurations {
    runtimeClasspath.get().extendsFrom(localRuntime)
}

dependencies {
    implementation("thedarkcolour:kotlinforforge:${project.findProperty("kff_version")}")
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to project.findProperty("minecraft_version"),
        "minecraft_version_range" to project.findProperty("minecraft_version_range"),
        "neo_version" to project.findProperty("neo_version"),
        "mod_id" to project.findProperty("mod_id"),
        "mod_name" to project.findProperty("mod_name"),
        "mod_license" to project.findProperty("mod_license"),
        "mod_version" to project.findProperty("mod_version"),
        "mod_authors" to project.findProperty("mod_authors"),
        "mod_description" to project.findProperty("mod_description")
    )
    inputs.properties(replaceProperties)
    filesMatching("**/*.toml") {
        expand(replaceProperties)
    }
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(file("${project.projectDir}/repo").toURI())
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

