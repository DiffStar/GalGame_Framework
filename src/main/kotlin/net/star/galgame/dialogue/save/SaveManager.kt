package net.star.galgame.dialogue.save

import net.minecraft.client.Minecraft
import net.star.galgame.dialogue.DialogueEntry
import net.star.galgame.dialogue.control.DialogueController
import net.star.galgame.dialogue.variable.VariableManager
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
            
            val serializableVariables = VariableManager.getAll().mapValues { (_, value) ->
                when (value) {
                    is net.star.galgame.dialogue.variable.VariableValue.Number -> 
                        SerializableVariableValue.Number(value.value)
                    is net.star.galgame.dialogue.variable.VariableValue.Boolean -> 
                        SerializableVariableValue.Boolean(value.value)
                    is net.star.galgame.dialogue.variable.VariableValue.String -> 
                        SerializableVariableValue.String(value.value)
                }
            }
            
            val saveData = SaveData(
                slotId = slotId,
                scriptId = scriptId,
                currentIndex = getCurrentIndex(controller),
                history = serializableHistory,
                variables = serializableVariables,
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
        VariableManager.clear()
        saveData.variables.forEach { (name, value) ->
            when (value) {
                is SerializableVariableValue.Number -> 
                    VariableManager.set(name, value.value)
                is SerializableVariableValue.Boolean -> 
                    VariableManager.set(name, value.value)
                is SerializableVariableValue.String -> 
                    VariableManager.set(name, value.value)
            }
        }
        
        controller.reset()
        controller.setCurrentIndex(saveData.currentIndex)
        
        val script = net.star.galgame.dialogue.DialogueManager.getScript(saveData.scriptId) ?: return
        val historyField = DialogueController::class.java.getDeclaredField("history")
        historyField.isAccessible = true
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

