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
    private val preloadQueue = mutableListOf<String>()
    private val lock = ReentrantReadWriteLock()
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    
    private val imageExtensions = setOf("png", "jpg", "jpeg", "webp")
    private val audioExtensions = setOf("ogg", "mp3", "wav")
    
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
    
    fun preloadResources(packId: String, resourceType: String? = null) {
        lock.read {
            val keys = if (resourceType != null) {
                resourcePaths.keys.filter { it.startsWith("$packId:") && it.contains("/$resourceType/") }
            } else {
                resourcePaths.keys.filter { it.startsWith("$packId:") }
            }
            
            preloadQueue.addAll(keys)
        }
        
        executor.submit {
            processPreloadQueue()
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
        while (preloadQueue.isNotEmpty()) {
            val key = lock.write {
                if (preloadQueue.isEmpty()) return
                preloadQueue.removeAt(0)
            } ?: continue
            
            val (packId, resourcePath) = key.split(":", limit = 2)
            val filePath = resourcePaths[key] ?: continue
            
            when {
                imageExtensions.any { resourcePath.endsWith(".$it", ignoreCase = true) } -> {
                    if (!imageCache.containsKey(key)) {
                        try {
                            val image = ImageIO.read(filePath.toFile())
                            lock.write {
                                imageCache[key] = image
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                audioExtensions.any { resourcePath.endsWith(".$it", ignoreCase = true) } -> {
                    if (!audioCache.containsKey(key)) {
                        try {
                            val location = ResourceLocation.parse("${packId}:${resourcePath.replace("\\", "/")}")
                            lock.write {
                                audioCache[key] = location
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }
    
    fun shutdown() {
        executor.shutdown()
    }
}

