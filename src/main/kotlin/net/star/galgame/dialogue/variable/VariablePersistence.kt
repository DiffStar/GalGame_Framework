package net.star.galgame.dialogue.variable

import net.minecraft.client.Minecraft
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object VariablePersistence {
    private const val PERSISTENCE_DIR = "galgame_variables"
    private const val GLOBAL_VARS_FILE = "global_vars.dat"
    private val lock = ReentrantReadWriteLock()
    
    private val persistenceDirectory: Path by lazy {
        val mc = Minecraft.getInstance()
        val gameDir = Paths.get(mc.gameDirectory.absolutePath)
        val varsDir = gameDir.resolve(PERSISTENCE_DIR)
        Files.createDirectories(varsDir)
        varsDir
    }
    
    fun saveGlobalVariables() {
        lock.write {
            try {
                val file = persistenceDirectory.resolve(GLOBAL_VARS_FILE)
                val baos = ByteArrayOutputStream()
                val oos = ObjectOutputStream(baos)
                
                val serializableVars = VariableManager.getAllGlobal().mapValues { (_, value) ->
                    serializeValue(value)
                }
                
                oos.writeObject(serializableVars)
                oos.close()
                
                Files.write(file, baos.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            } catch (e: Exception) {
            }
        }
    }
    
    fun loadGlobalVariables() {
        lock.read {
            try {
                val file = persistenceDirectory.resolve(GLOBAL_VARS_FILE)
                if (!Files.exists(file)) return
                
                val data = Files.readAllBytes(file)
                val ois = ObjectInputStream(ByteArrayInputStream(data))
                @Suppress("UNCHECKED_CAST")
                val serializableVars = ois.readObject() as? Map<String, SerializableVariableValue> ?: return
                ois.close()
                
                VariableManager.clearGlobal()
                serializableVars.forEach { (name, value) ->
                    deserializeValue(name, value)?.let { (varName, varValue) ->
                        VariableManager.setGlobal(varName, varValue)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun saveLocalVariables(scope: String) {
        lock.write {
            try {
                val file = persistenceDirectory.resolve("local_${scope}.dat")
                val baos = ByteArrayOutputStream()
                val oos = ObjectOutputStream(baos)
                
                val serializableVars = VariableManager.getAllLocal(scope).mapValues { (_, value) ->
                    serializeValue(value)
                }
                
                oos.writeObject(serializableVars)
                oos.close()
                
                Files.write(file, baos.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            } catch (e: Exception) {
            }
        }
    }
    
    fun loadLocalVariables(scope: String) {
        lock.read {
            try {
                val file = persistenceDirectory.resolve("local_${scope}.dat")
                if (!Files.exists(file)) return
                
                val data = Files.readAllBytes(file)
                val ois = ObjectInputStream(ByteArrayInputStream(data))
                @Suppress("UNCHECKED_CAST")
                val serializableVars = ois.readObject() as? Map<String, SerializableVariableValue> ?: return
                ois.close()
                
                VariableManager.clearScope(scope)
                serializableVars.forEach { (name, value) ->
                    deserializeValue(name, value)?.let { (varName, varValue) ->
                        VariableManager.setLocal(scope, varName, varValue)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
    
    fun deleteLocalVariables(scope: String) {
        lock.write {
            try {
                val file = persistenceDirectory.resolve("local_${scope}.dat")
                if (Files.exists(file)) {
                    Files.delete(file)
                }
            } catch (e: Exception) {
            }
        }
    }
    
    private fun serializeValue(value: VariableValue): SerializableVariableValue {
        return when (value) {
            is VariableValue.Integer -> SerializableVariableValue.Integer(value.value)
            is VariableValue.Long -> SerializableVariableValue.Long(value.value)
            is VariableValue.Float -> SerializableVariableValue.Float(value.value)
            is VariableValue.Number -> SerializableVariableValue.Number(value.value)
            is VariableValue.Boolean -> SerializableVariableValue.Boolean(value.value)
            is VariableValue.String -> SerializableVariableValue.String(value.value)
        }
    }
    
    private fun deserializeValue(name: String, value: SerializableVariableValue): Pair<String, Any>? {
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
    
    private sealed class SerializableVariableValue : Serializable {
        data class Integer(val value: Int) : SerializableVariableValue()
        data class Long(val value: kotlin.Long) : SerializableVariableValue()
        data class Float(val value: kotlin.Float) : SerializableVariableValue()
        data class Number(val value: Double) : SerializableVariableValue()
        data class Boolean(val value: kotlin.Boolean) : SerializableVariableValue()
        data class String(val value: kotlin.String) : SerializableVariableValue()
    }
}

