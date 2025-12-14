import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.moddev") version "2.0.124"
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    idea
}

val neoforgeModdevVersionFromProps = project.findProperty("neoforge_moddev_version") as String? ?: "2.0.124"
val kotlinVersionFromProps = project.findProperty("kotlin_version") as String? ?: "2.1.10"


tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

val modId = project.findProperty("mod_id") as String? ?: "galgame"
val modVersion = project.findProperty("mod_version") as String? ?: "1.0.0"
val modGroupId = project.findProperty("mod_group_id") as String? ?: "net.star.galgame"

version = modVersion
group = modGroupId

repositories {
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

base {
    archivesName.set(modId)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

neoForge {
    version = project.findProperty("neo_version") as String

    parchment {
        mappingsVersion = project.findProperty("parchment_mappings_version") as String
        minecraftVersion = project.findProperty("parchment_minecraft_version") as String
    }

    runs {
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        register("data") {
            clientData()
            programArguments.addAll(
                "--mod", modId,
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
        register(modId) {
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

tasks.processResources {
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
}

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
        jvmTarget.set(JvmTarget.JVM_21)
        incremental.and(false) // 禁用增量编译以减少会话文件生成
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

// 清理 Kotlin 编译器会话文件的任务
tasks.register("cleanKotlinSessions", Delete::class) {
    description = "清理 Kotlin 编译器生成的会话文件"
    delete(".kotlin")
}

