package net.star.galgame.contentpack.script

import net.star.galgame.contentpack.ContentPack
import net.star.galgame.contentpack.ContentPackManager
import net.star.galgame.dialogue.DialogueManager
import net.star.galgame.dialogue.DialogueScript
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ScriptHotReloader(private val enabled: Boolean = false) {
    private val watchServices = ConcurrentHashMap<Path, WatchService>()
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ScriptHotReloader").apply { isDaemon = true }
    }
    private val parser = ScriptParser()
    private val validator = ScriptValidator()
    private val lastModified = ConcurrentHashMap<Path, Long>()
    
    fun startWatching(pack: ContentPack) {
        if (!enabled) return
        
        try {
            val watchService = pack.packPath.fileSystem.newWatchService()
            val scriptsPath = pack.packPath.resolve(pack.manifest.resources.scripts)
            
            if (Files.exists(scriptsPath) && Files.isDirectory(scriptsPath)) {
                scriptsPath.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE
                )
                
                watchServices[pack.packPath] = watchService
                
                executor.submit {
                    watchLoop(watchService, pack)
                }
            }
        } catch (e: Exception) {
        }
    }
    
    fun stopWatching(packId: String) {
        val pack = ContentPackManager.getPack(packId) ?: return
        watchServices.remove(pack.packPath)?.close()
    }
    
    fun stopAll() {
        watchServices.values.forEach { it.close() }
        watchServices.clear()
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
    
    private fun watchLoop(watchService: WatchService, pack: ContentPack) {
        while (true) {
            try {
                val key = watchService.take()
                
                key.pollEvents().forEach { event ->
                    val kind = event.kind()
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY ||
                        kind == StandardWatchEventKinds.ENTRY_CREATE ||
                        kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        
                        val context = event.context() as? Path ?: return@forEach
                        val scriptPath = pack.packPath.resolve(pack.manifest.resources.scripts).resolve(context)
                        
                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            handleScriptDeleted(scriptPath, pack)
                        } else {
                            handleScriptChanged(scriptPath, pack)
                        }
                    }
                }
                
                if (!key.reset()) {
                    break
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
            }
        }
    }
    
    private fun handleScriptChanged(scriptPath: Path, pack: ContentPack) {
        if (!Files.exists(scriptPath) || !Files.isRegularFile(scriptPath)) {
            return
        }
        
        val currentModified = Files.getLastModifiedTime(scriptPath).toMillis()
        val lastModifiedTime = lastModified[scriptPath] ?: 0L
        
        if (currentModified <= lastModifiedTime) {
            return
        }
        
        lastModified[scriptPath] = currentModified
        
        try {
            Thread.sleep(100)
            
            val scriptData = pack.scripts.values.find { it.path == scriptPath }
            if (scriptData == null) return
            
            val content = Files.readString(scriptPath)
            val parseResult = parser.parse(content, scriptData.format)
            
            if (parseResult.script != null) {
                val validation = validator.validate(parseResult.script)
                if (validation.isValid) {
                    DialogueManager.registerScript(parseResult.script)
                }
            }
        } catch (e: Exception) {
        }
    }
    
    private fun handleScriptDeleted(scriptPath: Path, pack: ContentPack) {
        lastModified.remove(scriptPath)
    }
}

