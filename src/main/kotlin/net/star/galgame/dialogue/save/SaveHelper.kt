package net.star.galgame.dialogue.save

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.util.Base64

object SaveHelper {
    private const val SAVE_DIR = "galgame_saves"
    private const val SCREENSHOT_DIR = "screenshots"
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    
    private val saveDirectory: Path by lazy {
        val mc = Minecraft.getInstance()
        val gameDir = Paths.get(mc.gameDirectory.absolutePath)
        val savesDir = gameDir.resolve(SAVE_DIR)
        Files.createDirectories(savesDir)
        savesDir
    }
    
    private val screenshotDirectory: Path by lazy {
        val screenshots = saveDirectory.resolve(SCREENSHOT_DIR)
        Files.createDirectories(screenshots)
        screenshots
    }
    
    private fun getSecretKey(): SecretKeySpec {
        val mc = Minecraft.getInstance()
        val playerUuid = mc.player?.uuid?.toString() ?: "default"
        val keyString = "GALGAME_SAVE_${playerUuid}_KEY_2024"
        val key = keyString.toByteArray()
        val sha = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha.digest(key)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }
    
    fun captureScreenshot(slotId: Int): String? {
        return try {
            val mc = Minecraft.getInstance()
            val screenshotFile = screenshotFile(slotId)
            Files.createDirectories(screenshotFile.parent)
            
            val future = CompletableFuture<String?>()
            Screenshot.takeScreenshot(mc.mainRenderTarget) { image ->
                try {
                    image.writeToFile(screenshotFile)
                    future.complete(screenshotFile.toString())
                } catch (e: Exception) {
                    future.complete(null)
                } finally {
                    image.close()
                }
            }
            
            future.get()
        } catch (e: Exception) {
            null
        }
    }
    
    fun screenshotFile(slotId: Int): Path {
        return screenshotDirectory.resolve("save_${slotId}.png")
    }
    
    fun saveFile(slotId: Int): Path {
        return saveDirectory.resolve("save_${slotId}.dat")
    }
    
    fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return cipher.doFinal(data)
    }
    
    fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey())
        return cipher.doFinal(data)
    }
    
    fun calculateChecksum(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data)
        return Base64.getEncoder().encodeToString(hash)
    }
    
    fun verifyChecksum(data: ByteArray, checksum: String): Boolean {
        return calculateChecksum(data) == checksum
    }
}

