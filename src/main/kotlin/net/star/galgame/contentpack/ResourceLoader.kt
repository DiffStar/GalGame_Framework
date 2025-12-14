package net.star.galgame.contentpack

import net.minecraft.resources.ResourceLocation
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ResourceLoader {
    private val imageCache = ConcurrentHashMap<String, BufferedImage>()
    private val audioCache = ConcurrentHashMap<String, ResourceLocation>()
    private val resourcePaths = ConcurrentHashMap<String, Path>()
    private val preloadQueue = mutableListOf<PreloadTask>()
    private val preloadPriority = ConcurrentHashMap<String, Int>()
    private val preloadProgress = ConcurrentHashMap<String, Boolean>()
    private val lock = ReentrantReadWriteLock()
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private var isPreloading = false
    private var batchSize = 10
    
    private val imageExtensions = setOf("png", "jpg", "jpeg", "webp")
    private val audioExtensions = setOf("ogg", "mp3", "wav")
    
    private data class PreloadTask(
        val key: String,
        val priority: Int,
        val resourceType: String
    )
    
    fun registerPackResources(packId: String, packPath: Path, resources: ResourceStructure) {
        lock.write {
            val imagesPath = packPath.resolve(resources.images)
            val audioPath = packPath.resolve(resources.audio)
            val charactersPath = packPath.resolve(resources.characters)
            val backgroundsPath = packPath.resolve(resources.backgrounds)
            val voicesPath = packPath.resolve(resources.voices)
            val bgmPath = packPath.resolve(resources.bgm)
            val sePath = packPath.resolve(resources.se)
            
            scanResources(imagesPath, packId, "image")
            scanResources(charactersPath, packId, "character")
            scanResources(backgroundsPath, packId, "background")
            scanResources(audioPath, packId, "audio")
            scanResources(voicesPath, packId, "voice")
            scanResources(bgmPath, packId, "bgm")
            scanResources(sePath, packId, "se")
        }
    }
    
    fun unregisterPackResources(packId: String) {
        lock.write {
            resourcePaths.keys.removeAll { it.startsWith("$packId:") }
            imageCache.keys.removeAll { it.startsWith("$packId:") }
            audioCache.keys.removeAll { it.startsWith("$packId:") }
            preloadProgress.keys.removeAll { it.startsWith("$packId:") }
            preloadPriority.keys.removeAll { it.startsWith("$packId:") }
            preloadQueue.removeAll { it.key.startsWith("$packId:") }
        }
    }
    
    fun loadImage(packId: String, resourcePath: String): BufferedImage? {
        val key = "$packId:$resourcePath"
        
        return lock.read {
            imageCache[key]
        } ?: lock.write {
            imageCache[key] ?: run {
                val filePath = resourcePaths[key] ?: return@run null
                try {
                    val image = ImageIO.read(filePath.toFile())
                    imageCache[key] = image
                    image
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    fun getImageResourceLocation(packId: String, resourcePath: String): ResourceLocation? {
        val key = "$packId:$resourcePath"
        val filePath = resourcePaths[key] ?: return null
        
        return try {
            ResourceLocation.parse("${packId}:${resourcePath.replace("\\", "/")}")
        } catch (e: Exception) {
            null
        }
    }
    
    fun loadAudio(packId: String, resourcePath: String): ResourceLocation? {
        val key = "$packId:$resourcePath"
        
        return lock.read {
            audioCache[key]
        } ?: lock.write {
            audioCache[key] ?: run {
                val filePath = resourcePaths[key] ?: return@run null
                try {
                    val location = ResourceLocation.parse("${packId}:${resourcePath.replace("\\", "/")}")
                    audioCache[key] = location
                    location
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    fun preloadResources(packId: String, resourceType: String? = null, priority: Int = 0) {
        lock.write {
            val keys = if (resourceType != null) {
                resourcePaths.keys.filter { it.startsWith("$packId:") && it.contains("/$resourceType/") }
            } else {
                resourcePaths.keys.filter { it.startsWith("$packId:") }
            }
            
            keys.forEach { key ->
                if (!preloadProgress.containsKey(key)) {
                    val type = when {
                        imageExtensions.any { key.endsWith(".$it", ignoreCase = true) } -> "image"
                        audioExtensions.any { key.endsWith(".$it", ignoreCase = true) } -> "audio"
                        else -> "unknown"
                    }
                    preloadQueue.add(PreloadTask(key, priority, type))
                    preloadPriority[key] = priority
                }
            }
            
            preloadQueue.sortByDescending { it.priority }
        }
        
        if (!isPreloading) {
            executor.submit {
                processPreloadQueue()
            }
        }
    }
    
    fun preloadResource(packId: String, resourcePath: String, priority: Int = 10) {
        val key = "$packId:$resourcePath"
        lock.write {
            if (resourcePaths.containsKey(key) && !preloadProgress.containsKey(key)) {
                val type = when {
                    imageExtensions.any { resourcePath.endsWith(".$it", ignoreCase = true) } -> "image"
                    audioExtensions.any { resourcePath.endsWith(".$it", ignoreCase = true) } -> "audio"
                    else -> "unknown"
                }
                preloadQueue.add(PreloadTask(key, priority, type))
                preloadPriority[key] = priority
                preloadQueue.sortByDescending { it.priority }
            }
        }
        
        if (!isPreloading) {
            executor.submit {
                processPreloadQueue()
            }
        }
    }
    
    fun preloadCriticalResources(packId: String, resourcePaths: List<String>) {
        resourcePaths.forEachIndexed { index, path ->
            preloadResource(packId, path, priority = 100 - index)
        }
    }
    
    fun clearCache(packId: String? = null) {
        lock.write {
            if (packId != null) {
                imageCache.keys.removeAll { it.startsWith("$packId:") }
                audioCache.keys.removeAll { it.startsWith("$packId:") }
            } else {
                imageCache.clear()
                audioCache.clear()
            }
        }
    }
    
    fun getResourcePath(packId: String, resourcePath: String): Path? {
        val key = "$packId:$resourcePath"
        return lock.read {
            resourcePaths[key]
        }
    }
    
    private fun scanResources(directory: Path, packId: String, resourceType: String) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return
        }
        
        try {
            Files.walk(directory).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .forEach { file ->
                        val extension = file.fileName.toString()
                            .substringAfterLast(".", "")
                            .lowercase()
                        
                        if (imageExtensions.contains(extension) || audioExtensions.contains(extension)) {
                            val relativePath = directory.relativize(file)
                            val resourcePath = relativePath.toString().replace("\\", "/")
                            val key = "$packId:$resourceType/$resourcePath"
                            resourcePaths[key] = file
                        }
                    }
            }
        } catch (e: Exception) {
        }
    }
    
    private fun processPreloadQueue() {
        isPreloading = true
        try {
            while (true) {
                val tasks = lock.write {
                    if (preloadQueue.isEmpty()) {
                        isPreloading = false
                        return
                    }
                    val batch = preloadQueue.take(batchSize)
                    preloadQueue.removeAll(batch)
                    batch
                }
                
                if (tasks.isEmpty()) {
                    isPreloading = false
                    return
                }
                
                tasks.forEach { task ->
                    val key = task.key
                    if (preloadProgress[key] == true) {
                        return@forEach
                    }
                    
                    val (packId, resourcePath) = key.split(":", limit = 2)
                    val filePath = lock.read { resourcePaths[key] } ?: run {
                        lock.write { preloadProgress[key] = false }
                        return@forEach
                    }
                    
                    when (task.resourceType) {
                        "image" -> {
                            if (!imageCache.containsKey(key)) {
                                try {
                                    val image = ImageIO.read(filePath.toFile())
                                    lock.write {
                                        imageCache[key] = image
                                        preloadProgress[key] = true
                                    }
                                } catch (e: Exception) {
                                    lock.write { preloadProgress[key] = false }
                                }
                            } else {
                                lock.write { preloadProgress[key] = true }
                            }
                        }
                        "audio" -> {
                            if (!audioCache.containsKey(key)) {
                                try {
                                    val location = ResourceLocation.parse("${packId}:${resourcePath.replace("\\", "/")}")
                                    lock.write {
                                        audioCache[key] = location
                                        preloadProgress[key] = true
                                    }
                                } catch (e: Exception) {
                                    lock.write { preloadProgress[key] = false }
                                }
                            } else {
                                lock.write { preloadProgress[key] = true }
                            }
                        }
                    }
                }
            }
        } finally {
            isPreloading = false
        }
    }
    
    fun clearPreloadQueue() {
        lock.write {
            preloadQueue.clear()
            preloadPriority.clear()
            preloadProgress.clear()
        }
    }
    
    fun getPreloadProgress(): Double {
        return lock.read {
            val total = preloadProgress.size
            if (total == 0) return@read 0.0
            val completed = preloadProgress.values.count { it }
            completed.toDouble() / total
        }
    }
    
    fun setBatchSize(size: Int) {
        batchSize = size.coerceIn(1, 50)
    }
    
    fun getPreloadQueueSize(): Int {
        return lock.read {
            preloadQueue.size
        }
    }
    
    fun shutdown() {
        executor.shutdown()
    }
}

