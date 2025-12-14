package net.star.galgame.api.contentpack

import net.star.galgame.contentpack.ContentPackManifest
import net.star.galgame.contentpack.ContentPack
import net.star.galgame.contentpack.ScriptData
import net.star.galgame.contentpack.ScriptFormat
import java.nio.file.Path

class ContentPackBuilder {
    private var manifest: ContentPackManifest? = null
    private var packPath: Path? = null
    private val scripts = mutableMapOf<String, ScriptData>()

    fun manifest(manifest: ContentPackManifest): ContentPackBuilder {
        this.manifest = manifest
        return this
    }

    fun path(path: Path): ContentPackBuilder {
        this.packPath = path
        return this
    }

    fun addScript(id: String, path: Path, format: ScriptFormat, content: String, parsed: Boolean = false): ContentPackBuilder {
        scripts[id] = ScriptData(id, path, format, content, parsed)
        return this
    }

    fun build(): ContentPack {
        val manifest = this.manifest ?: throw IllegalStateException("Manifest is required")
        val packPath = this.packPath ?: throw IllegalStateException("Pack path is required")
        return ContentPack(manifest, packPath, scripts.toMap(), loaded = true)
    }
}

object ContentPackBuilderFactory {
    fun create(): ContentPackBuilder {
        return ContentPackBuilder()
    }
}

