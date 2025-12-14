package net.star.galgame.developer

import net.star.galgame.contentpack.ContentPack
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ResourceManager(private val contentPack: ContentPack) {
    private val resourceCache = ConcurrentHashMap<String, ResourceInfo>()
    
    data class ResourceInfo(
        val path: Path,
        val type: ResourceType,
        val size: Long,
        val lastModified: Long
    )
    
    enum class ResourceType {
        IMAGE,
        AUDIO,
        SCRIPT,
        DATA,
        UNKNOWN
    }
    
    fun scanResources(): Map<ResourceType, List<ResourceInfo>> {
        resourceCache.clear()
        val resources = mutableMapOf<ResourceType, MutableList<ResourceInfo>>()
        
        try {
            scanDirectory(contentPack.packPath, resources)
        } catch (e: Exception) {
            DevLogger.error("ResourceManager", "扫描资源失败", e)
        }
        
        return resources
    }
    
    private fun scanDirectory(dir: Path, resources: MutableMap<ResourceType, MutableList<ResourceInfo>>) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return
        
        Files.walk(dir).use { stream ->
            stream.filter { Files.isRegularFile(it) }.forEach { file ->
                val relativePath = contentPack.packPath.relativize(file)
                val type = detectResourceType(file)
                val info = ResourceInfo(
                    path = file,
                    type = type,
                    size = Files.size(file),
                    lastModified = Files.getLastModifiedTime(file).toMillis()
                )
                
                resourceCache[relativePath.toString()] = info
                resources.getOrPut(type) { mutableListOf() }.add(info)
            }
        }
    }
    
    private fun detectResourceType(path: Path): ResourceType {
        val fileName = path.fileName.toString().lowercase()
        val extension = fileName.substringAfterLast('.', "")
        
        return when (extension) {
            "png", "jpg", "jpeg", "webp", "gif" -> ResourceType.IMAGE
            "ogg", "mp3", "wav", "flac" -> ResourceType.AUDIO
            "json", "yaml", "yml", "dsl" -> ResourceType.SCRIPT
            "txt", "toml", "properties" -> ResourceType.DATA
            else -> ResourceType.UNKNOWN
        }
    }
    
    fun getResource(path: String): ResourceInfo? = resourceCache[path]
    
    fun getAllResources(): Map<String, ResourceInfo> = resourceCache.toMap()
    
    fun getResourcesByType(type: ResourceType): List<ResourceInfo> {
        return resourceCache.values.filter { it.type == type }
    }
    
    fun getResourceSize(type: ResourceType): Long {
        return getResourcesByType(type).sumOf { it.size }
    }
    
    fun validateResource(path: String): ValidationResult {
        val info = resourceCache[path] ?: return ValidationResult(false, "资源不存在")
        
        if (!Files.exists(info.path)) {
            return ValidationResult(false, "文件不存在")
        }
        
        if (Files.size(info.path) == 0L) {
            return ValidationResult(false, "文件为空")
        }
        
        return ValidationResult(true, null)
    }
    
    fun deleteResource(path: String): Boolean {
        val info = resourceCache[path] ?: return false
        
        return try {
            Files.deleteIfExists(info.path)
            resourceCache.remove(path)
            DevLogger.info("ResourceManager", "删除资源: $path")
            true
        } catch (e: Exception) {
            DevLogger.error("ResourceManager", "删除资源失败: $path", e)
            false
        }
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val error: String?
    )
}

