package net.star.galgame.contentpack

import net.star.galgame.contentpack.script.ScriptParser
import net.star.galgame.contentpack.script.ScriptValidator
import java.nio.file.Files
import java.nio.file.Path

object ContentPackValidator {
    private val parser = ScriptParser()
    private val validator = ScriptValidator()
    
    fun validatePack(packPath: Path, manifest: ContentPackManifest): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        validateIntegrity(packPath, manifest, errors, warnings)
        validateResources(packPath, manifest, errors, warnings)
        validateScripts(packPath, manifest, errors, warnings)
        validateCompatibility(manifest, errors, warnings)
        
        return ValidationReport(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun validateIntegrity(
        packPath: Path,
        manifest: ContentPackManifest,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (!Files.exists(packPath)) {
            errors.add("内容包目录不存在")
            return
        }
        
        if (!Files.isDirectory(packPath)) {
            errors.add("内容包路径不是目录")
            return
        }
        
        val manifestPath = packPath.resolve("manifest.toml")
        if (!Files.exists(manifestPath)) {
            errors.add("清单文件不存在")
        }
        
        if (manifest.id.isBlank()) {
            errors.add("内容包ID为空")
        }
        
        if (manifest.name.isBlank()) {
            errors.add("内容包名称为空")
        }
        
        if (manifest.version.isBlank()) {
            errors.add("内容包版本为空")
        }
        
        val version = Version.parse(manifest.version)
        if (version == null) {
            warnings.add("版本格式可能无效: ${manifest.version}")
        }
    }
    
    private fun validateResources(
        packPath: Path,
        manifest: ContentPackManifest,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val resources = manifest.resources
        
        val requiredDirs = listOf(
            resources.scripts to "脚本",
            resources.images to "图片",
            resources.audio to "音频"
        )
        
        requiredDirs.forEach { (dir, name) ->
            val dirPath = packPath.resolve(dir)
            if (Files.exists(dirPath) && !Files.isDirectory(dirPath)) {
                errors.add("$name 路径不是目录: $dir")
            }
        }
        
        val scriptsPath = packPath.resolve(resources.scripts)
        if (Files.exists(scriptsPath)) {
            try {
                val scriptCount = Files.walk(scriptsPath)
                    .filter { Files.isRegularFile(it) }
                    .count()
                if (scriptCount == 0L) {
                    warnings.add("未找到脚本文件")
                }
            } catch (e: Exception) {
                warnings.add("无法扫描脚本目录: ${e.message}")
            }
        }
        
        val imagesPath = packPath.resolve(resources.images)
        if (Files.exists(imagesPath)) {
            try {
                val imageExtensions = setOf("png", "jpg", "jpeg", "webp")
                val hasImages = Files.walk(imagesPath)
                    .anyMatch { file ->
                        Files.isRegularFile(file) && imageExtensions.contains(
                            file.fileName.toString()
                                .substringAfterLast(".", "")
                                .lowercase()
                        )
                    }
                if (!hasImages) {
                    warnings.add("未找到图片文件")
                }
            } catch (e: Exception) {
                warnings.add("无法扫描图片目录: ${e.message}")
            }
        }
    }
    
    private fun validateScripts(
        packPath: Path,
        manifest: ContentPackManifest,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val scriptsPath = packPath.resolve(manifest.resources.scripts)
        if (!Files.exists(scriptsPath) || !Files.isDirectory(scriptsPath)) {
            return
        }
        
        try {
            Files.walk(scriptsPath).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { scriptFile ->
                        val format = detectFormat(scriptFile)
                        if (format != null) {
                            try {
                                val content = Files.readString(scriptFile)
                                val parseResult = parser.parse(content, format)
                                
                                if (parseResult.script == null) {
                                    errors.add("脚本解析失败: ${packPath.relativize(scriptFile)} - ${parseResult.errors.joinToString(", ")}")
                                } else {
                                    val validation = validator.validate(parseResult.script!!)
                                    if (!validation.isValid) {
                                        errors.addAll(validation.errors.map { "脚本验证失败: ${packPath.relativize(scriptFile)} - $it" })
                                    }
                                    if (validation.warnings.isNotEmpty()) {
                                        warnings.addAll(validation.warnings.map { "脚本警告: ${packPath.relativize(scriptFile)} - $it" })
                                    }
                                }
                            } catch (e: Exception) {
                                errors.add("读取脚本失败: ${packPath.relativize(scriptFile)} - ${e.message}")
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            errors.add("扫描脚本目录失败: ${e.message}")
        }
    }
    
    private fun validateCompatibility(
        manifest: ContentPackManifest,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        val frameworkVersion = Version.parse("1.21.10")
        val packFrameworkVersion = Version.parse(manifest.frameworkVersion)
        
        if (packFrameworkVersion == null) {
            warnings.add("无法解析框架版本: ${manifest.frameworkVersion}")
        } else if (frameworkVersion != null) {
            if (packFrameworkVersion.major != frameworkVersion.major) {
                errors.add("框架主版本不兼容: 需要 ${manifest.frameworkVersion}, 当前 1.21.10")
            } else if (packFrameworkVersion.minor > frameworkVersion.minor) {
                warnings.add("框架次版本可能不兼容: 内容包为 ${manifest.frameworkVersion}, 当前为 1.21.10")
            }
        }
        
        val minVersion = manifest.minFrameworkVersion?.let { Version.parse(it) }
        if (minVersion != null && frameworkVersion != null) {
            if (!frameworkVersion.isCompatibleWith(minVersion)) {
                errors.add("框架版本过低: 需要 >= $minVersion, 当前 $frameworkVersion")
            }
        }
        
        manifest.dependencies.forEach { dependency ->
            val depPack = ContentPackManager.getPack(dependency.packId)
            if (dependency.required && depPack == null) {
                errors.add("缺少必需依赖: ${dependency.packId}")
            } else if (depPack != null && dependency.version != null) {
                val requiredVersion = Version.parse(dependency.version)
                val packVersion = Version.parse(depPack.manifest.version)
                if (requiredVersion != null && packVersion != null && !packVersion.isCompatibleWith(requiredVersion)) {
                    errors.add("依赖版本不匹配: ${dependency.packId} 需要 ${dependency.version}, 当前 ${depPack.manifest.version}")
                }
            }
        }
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
}

data class ValidationReport(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

