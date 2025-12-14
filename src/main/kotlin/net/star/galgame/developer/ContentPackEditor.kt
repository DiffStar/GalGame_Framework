package net.star.galgame.developer

import net.star.galgame.contentpack.ContentPack
import net.star.galgame.contentpack.ScriptData
import net.star.galgame.contentpack.ScriptFormat
import net.star.galgame.contentpack.script.ScriptParser
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class ContentPackEditor(private val contentPack: ContentPack) {
    private val scriptCache = ConcurrentHashMap<String, String>()
    private val parser = ScriptParser()
    
    fun getScripts(): Map<String, ScriptData> = contentPack.scripts
    
    fun getScript(id: String): ScriptData? = contentPack.scripts[id]
    
    fun loadScriptContent(id: String): String? {
        val script = contentPack.scripts[id] ?: return null
        
        if (scriptCache.containsKey(id)) {
            return scriptCache[id]
        }
        
        return try {
            val content = Files.readString(script.path)
            scriptCache[id] = content
            content
        } catch (e: Exception) {
            DevLogger.error("ContentPackEditor", "加载脚本失败: $id", e)
            null
        }
    }
    
    fun saveScriptContent(id: String, content: String): Boolean {
        val script = contentPack.scripts[id] ?: return false
        
        return try {
            Files.writeString(script.path, content)
            scriptCache[id] = content
            DevLogger.info("ContentPackEditor", "脚本已保存: $id")
            true
        } catch (e: Exception) {
            DevLogger.error("ContentPackEditor", "保存脚本失败: $id", e)
            false
        }
    }
    
    fun validateScript(id: String): ValidationResult {
        val script = contentPack.scripts[id] ?: return ValidationResult(false, listOf("脚本不存在: $id"))
        
        val content = loadScriptContent(id) ?: return ValidationResult(false, listOf("无法加载脚本内容"))
        
        val result = parser.parse(content, script.format)
        
        return if (result.errors.isEmpty()) {
            ValidationResult(true, emptyList(), result.script)
        } else {
            ValidationResult(false, result.errors)
        }
    }
    
    fun createScript(id: String, format: ScriptFormat, content: String = ""): Boolean {
        val scriptsDir = contentPack.packPath.resolve("scripts")
        
        try {
            Files.createDirectories(scriptsDir)
            
            val extension = when (format) {
                ScriptFormat.JSON -> "json"
                ScriptFormat.YAML -> "yaml"
                ScriptFormat.DSL -> "dsl"
            }
            
            val scriptPath = scriptsDir.resolve("$id.$extension")
            Files.writeString(scriptPath, content)
            
            DevLogger.info("ContentPackEditor", "创建脚本: $id")
            return true
        } catch (e: Exception) {
            DevLogger.error("ContentPackEditor", "创建脚本失败: $id", e)
            return false
        }
    }
    
    fun deleteScript(id: String): Boolean {
        val script = contentPack.scripts[id] ?: return false
        
        return try {
            Files.deleteIfExists(script.path)
            scriptCache.remove(id)
            DevLogger.info("ContentPackEditor", "删除脚本: $id")
            true
        } catch (e: Exception) {
            DevLogger.error("ContentPackEditor", "删除脚本失败: $id", e)
            false
        }
    }
    
    fun clearCache() {
        scriptCache.clear()
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
        val script: net.star.galgame.dialogue.DialogueScript? = null
    )
}

