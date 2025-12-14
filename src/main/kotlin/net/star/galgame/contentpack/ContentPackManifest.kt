package net.star.galgame.contentpack

data class ContentPackManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val description: String? = null,
    val frameworkVersion: String,
    val minFrameworkVersion: String? = null,
    val dependencies: List<PackDependency> = emptyList(),
    val resources: ResourceStructure = ResourceStructure()
)

data class PackDependency(
    val packId: String,
    val version: String? = null,
    val required: Boolean = true
)

data class ResourceStructure(
    val scripts: String = "scripts",
    val images: String = "images",
    val audio: String = "audio",
    val characters: String = "characters",
    val backgrounds: String = "backgrounds",
    val voices: String = "voices",
    val bgm: String = "bgm",
    val se: String = "se"
)

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    companion object {
        fun parse(versionString: String): Version? {
            val parts = versionString.split(".")
            if (parts.size != 3) return null
            return try {
                Version(
                    parts[0].toInt(),
                    parts[1].toInt(),
                    parts[2].toInt()
                )
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    fun isCompatibleWith(minVersion: Version): Boolean {
        return when {
            major > minVersion.major -> true
            major < minVersion.major -> false
            minor > minVersion.minor -> true
            minor < minVersion.minor -> false
            else -> patch >= minVersion.patch
        }
    }

    override fun toString(): String = "$major.$minor.$patch"
}

