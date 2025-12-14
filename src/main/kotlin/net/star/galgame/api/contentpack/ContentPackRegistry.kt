package net.star.galgame.api.contentpack

import net.star.galgame.contentpack.ContentPack
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

interface IContentPackRegistry {
    fun register(pack: ContentPack)
    fun unregister(packId: String)
    fun get(packId: String): ContentPack?
    fun getAll(): Map<String, ContentPack>
    fun isRegistered(packId: String): Boolean
}

object ContentPackRegistry : IContentPackRegistry {
    private val registeredPacks = ConcurrentHashMap<String, ContentPack>()

    override fun register(pack: ContentPack) {
        registeredPacks[pack.manifest.id] = pack
    }

    override fun unregister(packId: String) {
        registeredPacks.remove(packId)
    }

    override fun get(packId: String): ContentPack? {
        return registeredPacks[packId]
    }

    override fun getAll(): Map<String, ContentPack> {
        return registeredPacks.toMap()
    }

    override fun isRegistered(packId: String): Boolean {
        return registeredPacks.containsKey(packId)
    }
}

