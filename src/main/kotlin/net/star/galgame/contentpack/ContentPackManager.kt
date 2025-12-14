package net.star.galgame.contentpack

import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.contentpack.script.ScriptParser
import net.star.galgame.contentpack.script.ScriptValidator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

object ContentPackManager {
    private val loadedPacks = ConcurrentHashMap<String, ContentPack>()
    private val loader = ContentPackLoader()
    private val parser = ScriptParser()
    private val validator = ScriptValidator()
    
    fun loadPack(packPath: Path): ContentPack? {
        val pack = loader.loadPack(packPath)
        
        if (!pack.loaded) {
            return pack
        }
        
        val versionCheck = checkVersion(pack.manifest)
        if (!versionCheck.isCompatible) {
            return pack.copy(loadErrors = listOf("版本不兼容: ${versionCheck.reason}"))
        }
        
        val dependencyCheck = checkDependencies(pack.manifest)
        if (!dependencyCheck.allSatisfied) {
            return pack.copy(loadErrors = dependencyCheck.missingDependencies.map { "缺少依赖: $it" })
        }
        
        pack.scripts.values.forEach { scriptData ->
            if (scriptData.parsed) {
                val parseResult = parser.parse(scriptData.content, scriptData.format)
                parseResult.script?.let { script ->
                    val validation = validator.validate(script)
                    if (validation.isValid) {
                        DialogueManager.registerScript(script)
                    }
                }
            }
        }
        
        loadedPacks[pack.manifest.id] = pack
        return pack
    }
    
    fun unloadPack(packId: String) {
        loadedPacks.remove(packId)
    }
    
    fun getPack(packId: String): ContentPack? {
        return loadedPacks[packId]
    }
    
    fun getAllPacks(): Map<String, ContentPack> {
        return loadedPacks.toMap()
    }
    
    fun reloadPack(packId: String): ContentPack? {
        val pack = loadedPacks[packId] ?: return null
        unloadPack(packId)
        return loadPack(pack.packPath)
    }
    
    fun scanAndLoadPacks(packsDirectory: Path) {
        if (!Files.exists(packsDirectory) || !Files.isDirectory(packsDirectory)) {
            return
        }
        
        Files.list(packsDirectory).use { stream ->
            stream.filter { Files.isDirectory(it) }
                .forEach { packDir ->
                    val manifestPath = packDir.resolve("manifest.toml")
                    if (Files.exists(manifestPath)) {
                        loadPack(packDir)
                    }
                }
        }
    }
    
    private fun checkVersion(manifest: ContentPackManifest): VersionCheckResult {
        val currentVersion = Version.parse(manifest.frameworkVersion) ?: return VersionCheckResult(
            isCompatible = false,
            reason = "无法解析框架版本: ${manifest.frameworkVersion}"
        )
        
        val minVersion = manifest.minFrameworkVersion?.let { Version.parse(it) }
        if (minVersion != null) {
            val frameworkVersion = getFrameworkVersion()
            if (frameworkVersion != null && !frameworkVersion.isCompatibleWith(minVersion)) {
                return VersionCheckResult(
                    isCompatible = false,
                    reason = "需要框架版本 >= $minVersion，当前版本: $frameworkVersion"
                )
            }
        }
        
        return VersionCheckResult(isCompatible = true, reason = null)
    }
    
    private fun checkDependencies(manifest: ContentPackManifest): DependencyCheckResult {
        val missing = mutableListOf<String>()
        
        manifest.dependencies.forEach { dependency ->
            if (dependency.required) {
                val depPack = loadedPacks[dependency.packId]
                if (depPack == null) {
                    missing.add("${dependency.packId} (必需)")
                } else if (dependency.version != null) {
                    val requiredVersion = Version.parse(dependency.version)
                    val packVersion = Version.parse(depPack.manifest.version)
                    if (requiredVersion != null && packVersion != null && !packVersion.isCompatibleWith(requiredVersion)) {
                        missing.add("${dependency.packId} 版本 ${dependency.version} (当前: ${depPack.manifest.version})")
                    }
                }
            }
        }
        
        return DependencyCheckResult(
            allSatisfied = missing.isEmpty(),
            missingDependencies = missing
        )
    }
    
    private fun getFrameworkVersion(): Version? {
        return Version.parse("1.21.10")
    }
}

data class VersionCheckResult(
    val isCompatible: Boolean,
    val reason: String?
)

data class DependencyCheckResult(
    val allSatisfied: Boolean,
    val missingDependencies: List<String>
)

