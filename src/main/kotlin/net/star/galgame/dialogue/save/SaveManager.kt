package net.star.galgame.dialogue.save

import net.minecraft.client.Minecraft
import net.star.galgame.dialogue.DialogueEntry
import net.star.galgame.dialogue.control.DialogueController
import net.star.galgame.dialogue.variable.VariableManager
import net.star.galgame.dialogue.state.GameStateManager
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardOpenOption

object SaveManager {
    private const val MAX_SLOTS = 20
    private const val AUTO_SAVE_SLOT = 0
    
    fun getMaxSlots(): Int = MAX_SLOTS
    
    fun saveGame(
        slotId: Int,
        scriptId: String,
        controller: DialogueController,
        progress: String = ""
    ): Boolean {
        if (slotId < 0 || slotId >= MAX_SLOTS) return false
        
        return try {
            val mc = Minecraft.getInstance()
            val worldName = mc.level?.let { 
                if (it.isClientSide) mc.getSingleplayerServer()?.worldData?.levelName else null
            }
            
            val screenshotPath = SaveHelper.captureScreenshot(slotId)
            
            val serializableHistory = controller.getHistory().map { entry ->
                net.star.galgame.dialogue.save.SerializableDialogueEntry(
                    id = entry.id,
                    characterId = entry.characterId,
                    text = entry.text,
                    expression = entry.expression,
                    position = entry.position.name,
                    read = entry.read
                )
            }
            
            val serializableGlobalVariables = VariableManager.getAllGlobal().mapValues { (_, value) ->
                serializeVariableValue(value)
            }
            
            val localScopes = mutableMapOf<String, Map<String, SerializableVariableValue>>()
            val gameState = GameStateManager.getState()
            val serializableGameState = net.star.galgame.dialogue.save.SerializableGameState(
                currentScene = gameState.currentScene,
                currentChapter = gameState.currentChapter,
                readFlags = gameState.readFlags,
                achievements = gameState.achievements.mapValues { (_, progress) ->
                    net.star.galgame.dialogue.save.SerializableAchievementProgress(
                        isUnlocked = progress.isUnlocked,
                        unlockTime = progress.unlockTime,
                        progress = progress.progress
                    )
                },
                statistics = gameState.statistics,
                sceneHistory = gameState.sceneHistory.map { record ->
                    net.star.galgame.dialogue.save.SerializableSceneRecord(
                        sceneId = record.sceneId,
                        timestamp = record.timestamp,
                        action = record.action.name
                    )
                }
            )
            
            val saveData = SaveData(
                slotId = slotId,
                scriptId = scriptId,
                currentIndex = getCurrentIndex(controller),
                history = serializableHistory,
                globalVariables = serializableGlobalVariables,
                localVariables = localScopes,
                gameState = serializableGameState,
                timestamp = System.currentTimeMillis(),
                worldName = worldName,
                progress = progress,
                screenshotPath = screenshotPath
            )
            
            val file = SaveHelper.saveFile(slotId)
            val baos = ByteArrayOutputStream()
            val oos = ObjectOutputStream(baos)
            oos.writeObject(saveData)
            oos.close()
            
            val data = baos.toByteArray()
            val checksum = SaveHelper.calculateChecksum(data)
            val encrypted = SaveHelper.encrypt(data)
            
            val output = ByteArrayOutputStream()
            val dos = DataOutputStream(output)
            dos.writeUTF(checksum)
            dos.writeInt(encrypted.size)
            dos.write(encrypted)
            dos.close()
            
            Files.write(file, output.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun loadGame(slotId: Int): SaveData? {
        if (slotId < 0 || slotId >= MAX_SLOTS) return null
        
        return try {
            val file = SaveHelper.saveFile(slotId)
            if (!Files.exists(file)) return null
            
            val data = Files.readAllBytes(file)
            val dis = DataInputStream(ByteArrayInputStream(data))
            val checksum = dis.readUTF()
            val encryptedSize = dis.readInt()
            val encrypted = ByteArray(encryptedSize)
            dis.readFully(encrypted)
            dis.close()
            
            val decrypted = SaveHelper.decrypt(encrypted)
            
            if (!SaveHelper.verifyChecksum(decrypted, checksum)) {
                return null
            }
            
            val ois = ObjectInputStream(ByteArrayInputStream(decrypted))
            val saveData = ois.readObject() as SaveData
            ois.close()
            
            saveData
        } catch (e: Exception) {
            null
        }
    }
    
    fun applySaveData(saveData: SaveData, controller: DialogueController) {
        VariableManager.clearGlobal()
        saveData.globalVariables.forEach { (name, value) ->
            deserializeVariableValue(name, value)?.let { (varName, varValue) ->
                VariableManager.setGlobal(varName, varValue)
            }
        }
        
        saveData.localVariables.forEach { (scope, variables) ->
            variables.forEach { (name, value) ->
                deserializeVariableValue(name, value)?.let { (varName, varValue) ->
                    VariableManager.setLocal(scope, varName, varValue)
                }
            }
        }
        
        val gameState = net.star.galgame.dialogue.state.GameState(
            currentScene = saveData.gameState.currentScene,
            currentChapter = saveData.gameState.currentChapter,
            readFlags = saveData.gameState.readFlags,
            achievements = saveData.gameState.achievements.mapValues { (_, progress) ->
                net.star.galgame.dialogue.state.AchievementProgress(
                    isUnlocked = progress.isUnlocked,
                    unlockTime = progress.unlockTime
                ).apply {
                    this.progress = progress.progress
                }
            },
            statistics = saveData.gameState.statistics,
            sceneHistory = saveData.gameState.sceneHistory.map { record ->
                net.star.galgame.dialogue.state.SceneRecord(
                    sceneId = record.sceneId,
                    timestamp = record.timestamp,
                    action = net.star.galgame.dialogue.state.SceneAction.valueOf(record.action)
                )
            }
        )
        GameStateManager.applyState(gameState)
        
        controller.reset()
        controller.setCurrentIndex(saveData.currentIndex)
        
        val script = net.star.galgame.dialogue.DialogueManager.getScript(saveData.scriptId) ?: return
        val historyField = DialogueController::class.java.getDeclaredField("history")
        historyField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val history = historyField.get(controller) as? MutableList<net.star.galgame.dialogue.DialogueEntry>
        history?.clear()
        
        saveData.history.forEach { serializableEntry ->
            val entry = script.entries.find { it.id == serializableEntry.id }
            if (entry != null) {
                val restoredEntry = entry.copy(
                    read = serializableEntry.read,
                    expression = serializableEntry.expression,
                    position = net.star.galgame.dialogue.CharacterPosition.valueOf(serializableEntry.position)
                )
                history?.add(restoredEntry)
            }
        }
    }
    
    private fun serializeVariableValue(value: net.star.galgame.dialogue.variable.VariableValue): SerializableVariableValue {
        return when (value) {
            is net.star.galgame.dialogue.variable.VariableValue.Integer -> 
                SerializableVariableValue.Integer(value.value)
            is net.star.galgame.dialogue.variable.VariableValue.Long -> 
                SerializableVariableValue.Long(value.value)
            is net.star.galgame.dialogue.variable.VariableValue.Float -> 
                SerializableVariableValue.Float(value.value)
            is net.star.galgame.dialogue.variable.VariableValue.Number -> 
                SerializableVariableValue.Number(value.value)
            is net.star.galgame.dialogue.variable.VariableValue.Boolean -> 
                SerializableVariableValue.Boolean(value.value)
            is net.star.galgame.dialogue.variable.VariableValue.String -> 
                SerializableVariableValue.String(value.value)
        }
    }
    
    private fun deserializeVariableValue(name: String, value: SerializableVariableValue): Pair<String, Any>? {
        val actualValue = when (value) {
            is SerializableVariableValue.Integer -> value.value
            is SerializableVariableValue.Long -> value.value
            is SerializableVariableValue.Float -> value.value
            is SerializableVariableValue.Number -> value.value
            is SerializableVariableValue.Boolean -> value.value
            is SerializableVariableValue.String -> value.value
        }
        return Pair(name, actualValue)
    }
    
    fun getSaveSlot(slotId: Int): SaveSlot {
        if (slotId < 0 || slotId >= MAX_SLOTS) {
            return SaveSlot(slotId, null, false, false)
        }
        
        val saveData = loadGame(slotId)
        return if (saveData != null) {
            SaveSlot(slotId, saveData, true, false)
        } else {
            val file = SaveHelper.saveFile(slotId)
            val exists = Files.exists(file)
            SaveSlot(slotId, null, false, exists)
        }
    }
    
    fun getAllSaveSlots(): List<SaveSlot> {
        return (0 until MAX_SLOTS).map { getSaveSlot(it) }
    }
    
    fun deleteSave(slotId: Int): Boolean {
        if (slotId < 0 || slotId >= MAX_SLOTS) return false
        
        return try {
            val file = SaveHelper.saveFile(slotId)
            if (Files.exists(file)) {
                Files.delete(file)
            }
            val screenshot = SaveHelper.screenshotFile(slotId)
            if (Files.exists(screenshot)) {
                Files.delete(screenshot)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun autoSave(scriptId: String, controller: DialogueController, progress: String = ""): Boolean {
        return saveGame(AUTO_SAVE_SLOT, scriptId, controller, progress)
    }
    
    fun getAutoSave(): SaveData? {
        return loadGame(AUTO_SAVE_SLOT)
    }
    
    private fun getCurrentIndex(controller: DialogueController): Int {
        return controller.getCurrentIndex()
    }
}

