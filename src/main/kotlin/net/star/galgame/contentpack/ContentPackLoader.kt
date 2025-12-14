package net.star.galgame.contentpack

import net.star.galgame.contentpack.script.ScriptParser
import net.star.galgame.contentpack.script.ScriptValidator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ContentPackLoader {
    private val parser = ScriptParser()
    private val validator = ScriptValidator()
    
    fun loadPack(packPath: Path): ContentPack {
        val manifestPath = packPath.resolve("manifest.toml")
        if (!Files.exists(manifestPath)) {
            return ContentPack(
                manifest = createDefaultManifest(),
                packPath = packPath,
                loaded = false,
                loadErrors = listOf("清单文件不存在: manifest.toml")
            )
        }
        
        val manifest = loadManifest(manifestPath)
        val scripts = loadScripts(packPath, manifest.resources.scripts)
        
        val allErrors = mutableListOf<String>()
        scripts.values.forEach { scriptData ->
            if (scriptData.parseErrors.isNotEmpty()) {
                allErrors.addAll(scriptData.parseErrors)
            }
        }
        
        return ContentPack(
            manifest = manifest,
            packPath = packPath,
            scripts = scripts,
            loaded = allErrors.isEmpty(),
            loadErrors = allErrors
        )
    }
    
    private fun loadManifest(manifestPath: Path): ContentPackManifest {
        val lines = Files.readAllLines(manifestPath)
        val manifest = mutableMapOf<String, String>()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            
            val equalIndex = trimmed.indexOf('=')
            if (equalIndex > 0) {
                val key = trimmed.substring(0, equalIndex).trim()
                var value = trimmed.substring(equalIndex + 1).trim()
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length - 1)
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length - 1)
                }
                manifest[key] = value
            }
        }
        
        val id = manifest["id"] ?: "unknown"
        val name = manifest["name"] ?: "Unknown Pack"
        val version = manifest["version"] ?: "1.0.0"
        val author = manifest["author"]
        val description = manifest["description"]
        val frameworkVersion = manifest["frameworkVersion"] ?: "1.0.0"
        val minFrameworkVersion = manifest["minFrameworkVersion"]
        
        val dependencies = mutableListOf<PackDependency>()
        var inDependencies = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[dependencies]") || trimmed.startsWith("[[dependencies]]")) {
                inDependencies = true
                continue
            }
            if (inDependencies && trimmed.startsWith("[")) {
                break
            }
            if (inDependencies && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val depId = parts[0].trim()
                    val depValue = parts[1].trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
                    dependencies.add(PackDependency(depId, depValue, true))
                }
            }
        }
        
        val resources = ResourceStructure(
            scripts = manifest["scripts"] ?: "scripts",
            images = manifest["images"] ?: "images",
            audio = manifest["audio"] ?: "audio",
            characters = manifest["characters"] ?: "characters",
            backgrounds = manifest["backgrounds"] ?: "backgrounds",
            voices = manifest["voices"] ?: "voices",
            bgm = manifest["bgm"] ?: "bgm",
            se = manifest["se"] ?: "se"
        )
        
        return ContentPackManifest(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            frameworkVersion = frameworkVersion,
            minFrameworkVersion = minFrameworkVersion,
            dependencies = dependencies,
            resources = resources
        )
    }
    
    private fun loadScripts(packPath: Path, scriptsDir: String): Map<String, ScriptData> {
        val scripts = mutableMapOf<String, ScriptData>()
        val scriptsPath = packPath.resolve(scriptsDir)
        
        if (!Files.exists(scriptsPath) || !Files.isDirectory(scriptsPath)) {
            return scripts
        }
        
        Files.walk(scriptsPath).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .forEach { file ->
                    val format = detectFormat(file)
                    if (format != null) {
                        try {
                            val content = Files.readString(file)
                            val parseResult = parser.parse(content, format)
                            
                            val scriptId = file.fileName.toString().substringBeforeLast(".")
                            scripts[scriptId] = ScriptData(
                                id = scriptId,
                                path = file,
                                format = format,
                                content = content,
                                parsed = parseResult.script != null,
                                parseErrors = parseResult.errors
                            )
                        } catch (e: Exception) {
                            val scriptId = file.fileName.toString().substringBeforeLast(".")
                            scripts[scriptId] = ScriptData(
                                id = scriptId,
                                path = file,
                                format = format,
                                content = "",
                                parsed = false,
                                parseErrors = listOf("读取文件错误: ${e.message}")
                            )
                        }
                    }
                }
        }
        
        return scripts
    }
    
    private fun detectFormat(file: Path): ScriptFormat? {
        val fileName = file.fileName.toString().lowercase()
        return when {
            fileName.endsWith(".json") -> ScriptFormat.JSON
            fileName.endsWith(".yaml") || fileName.endsWith(".yml") -> ScriptFormat.YAML
            fileName.endsWith(".dsl") || fileName.endsWith(".gal") -> ScriptFormat.DSL
            else -> null
        }
    }
    
    private fun createDefaultManifest(): ContentPackManifest {
        return ContentPackManifest(
            id = "unknown",
            name = "Unknown Pack",
            version = "1.0.0",
            frameworkVersion = "1.0.0",
            resources = ResourceStructure()
        )
    }
}

