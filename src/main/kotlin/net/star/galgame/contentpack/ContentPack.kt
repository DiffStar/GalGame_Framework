package net.star.galgame.contentpack

import java.nio.file.Path

data class ContentPack(
    val manifest: ContentPackManifest,
    val packPath: Path,
    val scripts: Map<String, ScriptData> = emptyMap(),
    val loaded: Boolean = false,
    val loadErrors: List<String> = emptyList()
)

data class ScriptData(
    val id: String,
    val path: Path,
    val format: ScriptFormat,
    val content: String,
    val parsed: Boolean = false,
    val parseErrors: List<String> = emptyList()
)

enum class ScriptFormat {
    JSON,
    YAML,
    DSL
}

