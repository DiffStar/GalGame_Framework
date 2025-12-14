package net.star.galgame.api.contentpack

import net.star.galgame.contentpack.ContentPackManifest
import net.star.galgame.contentpack.PackDependency
import net.star.galgame.contentpack.ResourceStructure

class ContentPackManifestBuilder {
    private var id: String? = null
    private var name: String? = null
    private var version: String? = null
    private var author: String? = null
    private var description: String? = null
    private var frameworkVersion: String? = null
    private var minFrameworkVersion: String? = null
    private val dependencies = mutableListOf<PackDependency>()
    private var resourceStructure: ResourceStructure = ResourceStructure()

    fun id(id: String): ContentPackManifestBuilder {
        this.id = id
        return this
    }

    fun name(name: String): ContentPackManifestBuilder {
        this.name = name
        return this
    }

    fun version(version: String): ContentPackManifestBuilder {
        this.version = version
        return this
    }

    fun author(author: String): ContentPackManifestBuilder {
        this.author = author
        return this
    }

    fun description(description: String): ContentPackManifestBuilder {
        this.description = description
        return this
    }

    fun frameworkVersion(version: String): ContentPackManifestBuilder {
        this.frameworkVersion = version
        return this
    }

    fun minFrameworkVersion(version: String): ContentPackManifestBuilder {
        this.minFrameworkVersion = version
        return this
    }

    fun addDependency(packId: String, version: String? = null, required: Boolean = true): ContentPackManifestBuilder {
        dependencies.add(PackDependency(packId, version, required))
        return this
    }

    fun resourceStructure(structure: ResourceStructure): ContentPackManifestBuilder {
        this.resourceStructure = structure
        return this
    }

    fun build(): ContentPackManifest {
        val id = this.id ?: throw IllegalStateException("ID is required")
        val name = this.name ?: throw IllegalStateException("Name is required")
        val version = this.version ?: throw IllegalStateException("Version is required")
        val frameworkVersion = this.frameworkVersion ?: "1.21.10"
        return ContentPackManifest(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            frameworkVersion = frameworkVersion,
            minFrameworkVersion = minFrameworkVersion,
            dependencies = dependencies.toList(),
            resources = resourceStructure
        )
    }
}

object ContentPackManifestBuilderFactory {
    fun create(): ContentPackManifestBuilder {
        return ContentPackManifestBuilder()
    }
}

