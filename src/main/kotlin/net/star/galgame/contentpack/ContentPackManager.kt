package net.star.galgame.contentpack

import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.contentpack.script.ScriptParser
import net.star.galgame.contentpack.script.ScriptValidator
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object ContentPackManager {
    private val loadedPacks = ConcurrentHashMap<String, ContentPack>()
    private val packPaths = ConcurrentHashMap<String, Path>()
    private val enabledPacks = ConcurrentHashMap<String, Boolean>()
    private val loadingQueue = mutableListOf<Path>()
    private val loader = ContentPackLoader()
    private val parser = ScriptParser()
    private val validator = ScriptValidator()
    private val lock = ReentrantReadWriteLock()
    
    fun scanAndLoadPacks(packsDirectory: Path) {
        if (!Files.exists(packsDirectory) || !Files.isDirectory(packsDirectory)) {
            return
        }
        
        val discoveredPacks = mutableListOf<DiscoveredPack>()
        Files.walk(packsDirectory, 1).use { stream ->
            stream.filter { Files.isDirectory(it) && it != packsDirectory }
                .forEach { packDir ->
                    val manifestPath = packDir.resolve("manifest.toml")
                    if (Files.exists(manifestPath)) {
                        val pack = loader.loadPack(packDir)
                        if (pack.loaded) {
                            discoveredPacks.add(DiscoveredPack(packDir, pack))
                        }
                    }
                }
        }
        
        val sortedPacks = resolveLoadOrder(discoveredPacks)
        sortedPacks.forEach { (pack, _) ->
            loadPack(pack.packPath)
        }
    }
    
    fun loadPack(packPath: Path): ContentPack? {
        return lock.write {
            val pack = loader.loadPack(packPath)
            
            if (!pack.loaded) {
                return@write pack
            }
            
            val conflictCheck = checkConflicts(pack.manifest)
            if (!conflictCheck.noConflicts) {
                return@write pack.copy(loadErrors = conflictCheck.conflicts)
            }
            
            val versionCheck = checkVersion(pack.manifest)
            if (!versionCheck.isCompatible) {
                return@write pack.copy(loadErrors = listOf("版本不兼容: ${versionCheck.reason}"))
            }
            
            val dependencyCheck = checkDependencies(pack.manifest)
            if (!dependencyCheck.allSatisfied) {
                return@write pack.copy(loadErrors = dependencyCheck.missingDependencies.map { "缺少依赖: $it" })
            }
            
            if (isPackEnabled(pack.manifest.id)) {
                initializePack(pack)
            }
            
            loadedPacks[pack.manifest.id] = pack
            packPaths[pack.manifest.id] = packPath
            enabledPacks[pack.manifest.id] = true
            pack
        }
    }
    
    fun unloadPack(packId: String) {
        lock.write {
            ResourceLoader.unregisterPackResources(packId)
            loadedPacks.remove(packId)
            packPaths.remove(packId)
        }
    }
    
    fun getPack(packId: String): ContentPack? {
        return lock.read {
            loadedPacks[packId]
        }
    }
    
    fun getAllPacks(): Map<String, ContentPack> {
        return lock.read {
            loadedPacks.toMap()
        }
    }
    
    fun isPackEnabled(packId: String): Boolean {
        return lock.read {
            enabledPacks[packId] ?: true
        }
    }
    
    fun setPackEnabled(packId: String, enabled: Boolean) {
        lock.write {
            enabledPacks[packId] = enabled
            val pack = loadedPacks[packId] ?: return@write
            if (enabled) {
                initializePack(pack)
            } else {
                ResourceLoader.unregisterPackResources(packId)
                pack.scripts.values.forEach { scriptData ->
                    if (scriptData.parsed) {
                        val parseResult = parser.parse(scriptData.content, scriptData.format)
                        parseResult.script?.let { script ->
                            DialogueManager.unregisterScript(script.id)
                        }
                    }
                }
            }
        }
    }
    
    fun getAllInstalledPacks(packsDirectory: Path): List<InstalledPackInfo> {
        return discoverPacks(packsDirectory).map { discovered ->
            val pack = discovered.pack
            val isLoaded = loadedPacks.containsKey(pack.manifest.id)
            val isEnabled = if (isLoaded) isPackEnabled(pack.manifest.id) else false
            val loadedPack = if (isLoaded) loadedPacks[pack.manifest.id] else null
            InstalledPackInfo(
                packId = pack.manifest.id,
                name = pack.manifest.name,
                version = pack.manifest.version,
                author = pack.manifest.author,
                description = pack.manifest.description,
                packPath = discovered.packPath,
                isLoaded = isLoaded,
                isEnabled = isEnabled,
                loadErrors = loadedPack?.loadErrors ?: emptyList()
            )
        }
    }
    
    fun deletePack(packId: String, packsDirectory: Path): Boolean {
        return lock.write {
            val packPath = packPaths[packId] ?: packsDirectory.resolve(packId)
            if (Files.exists(packPath)) {
                unloadPack(packId)
                enabledPacks.remove(packId)
                try {
                    Files.walk(packPath).use { stream ->
                        stream.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }
    
    fun importPack(sourcePath: Path, packsDirectory: Path): ContentPack? {
        if (!Files.exists(sourcePath)) {
            return null
        }
        
        val pack = loader.loadPack(sourcePath)
        if (!pack.loaded) {
            return pack
        }
        
        val targetPath = packsDirectory.resolve(pack.manifest.id)
        try {
            if (Files.exists(targetPath)) {
                Files.walk(targetPath).use { stream ->
                    stream.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
                }
            }
            Files.createDirectories(targetPath)
            Files.walk(sourcePath).use { stream ->
                stream.forEach { source ->
                    if (source != sourcePath) {
                        val target = targetPath.resolve(sourcePath.relativize(source))
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target)
                        } else {
                            Files.copy(source, target)
                        }
                    }
                }
            }
            return loadPack(targetPath)
        } catch (e: Exception) {
            return ContentPack(
                manifest = pack.manifest,
                packPath = targetPath,
                loaded = false,
                loadErrors = listOf("导入失败: ${e.message}")
            )
        }
    }
    
    fun reloadPack(packId: String): ContentPack? {
        return lock.write {
            val packPath = packPaths[packId] ?: return@write null
            unloadPack(packId)
            loadPack(packPath)
        }
    }
    
    fun discoverPacks(packsDirectory: Path): List<DiscoveredPack> {
        if (!Files.exists(packsDirectory) || !Files.isDirectory(packsDirectory)) {
            return emptyList()
        }
        
        val discovered = mutableListOf<DiscoveredPack>()
        Files.walk(packsDirectory, 1).use { stream ->
            stream.filter { Files.isDirectory(it) && it != packsDirectory }
                .forEach { packDir ->
                    val manifestPath = packDir.resolve("manifest.toml")
                    if (Files.exists(manifestPath)) {
                        val pack = loader.loadPack(packDir)
                        if (pack.loaded) {
                            discovered.add(DiscoveredPack(packDir, pack))
                        }
                    }
                }
        }
        return discovered
    }
    
    private fun resolveLoadOrder(packs: List<DiscoveredPack>): List<Pair<DiscoveredPack, Int>> {
        val sorted = mutableListOf<Pair<DiscoveredPack, Int>>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val packMap = packs.associateBy { it.pack.manifest.id }
        
        fun visit(packId: String, depth: Int) {
            if (packId in visiting) {
                return
            }
            if (packId in visited) {
                return
            }
            
            val pack = packMap[packId] ?: return
            visiting.add(packId)
            
            pack.pack.manifest.dependencies.forEach { dep ->
                if (dep.required) {
                    visit(dep.packId, depth + 1)
                }
            }
            
            visiting.remove(packId)
            visited.add(packId)
            sorted.add(pack to depth)
        }
        
        packs.forEach { visit(it.pack.manifest.id, 0) }
        return sorted.sortedBy { it.second }
    }
    
    private fun checkConflicts(manifest: ContentPackManifest): ConflictCheckResult {
        val conflicts = mutableListOf<String>()
        
        lock.read {
            loadedPacks.values.forEach { loadedPack ->
                if (loadedPack.manifest.id == manifest.id) {
                    val loadedVersion = Version.parse(loadedPack.manifest.version)
                    val newVersion = Version.parse(manifest.version)
                    if (loadedVersion != null && newVersion != null) {
                        if (newVersion.isCompatibleWith(loadedVersion) && loadedVersion.isCompatibleWith(newVersion)) {
                            conflicts.add("内容包 ${manifest.id} 已加载相同版本")
                        } else {
                            conflicts.add("内容包 ${manifest.id} 版本冲突: 已加载 ${loadedPack.manifest.version}, 尝试加载 ${manifest.version}")
                        }
                    } else {
                        conflicts.add("内容包 ${manifest.id} 已加载")
                    }
                }
            }
        }
        
        return ConflictCheckResult(
            noConflicts = conflicts.isEmpty(),
            conflicts = conflicts
        )
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
        
        lock.read {
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
        }
        
        return DependencyCheckResult(
            allSatisfied = missing.isEmpty(),
            missingDependencies = missing
        )
    }
    
    private fun initializePack(pack: ContentPack) {
        ResourceLoader.registerPackResources(pack.manifest.id, pack.packPath, pack.manifest.resources)
        
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
    }
    
    private fun getFrameworkVersion(): Version? {
        return Version.parse("1.21.10")
    }
}

data class DiscoveredPack(
    val packPath: Path,
    val pack: ContentPack
)

data class ConflictCheckResult(
    val noConflicts: Boolean,
    val conflicts: List<String>
)

data class VersionCheckResult(
    val isCompatible: Boolean,
    val reason: String?
)

data class DependencyCheckResult(
    val allSatisfied: Boolean,
    val missingDependencies: List<String>
)

data class InstalledPackInfo(
    val packId: String,
    val name: String,
    val version: String,
    val author: String?,
    val description: String?,
    val packPath: Path,
    val isLoaded: Boolean,
    val isEnabled: Boolean,
    val loadErrors: List<String>
)

