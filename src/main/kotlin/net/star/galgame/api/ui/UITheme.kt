package net.star.galgame.api.ui

import net.minecraft.resources.ResourceLocation

data class UITheme(
    val id: String,
    val name: String,
    val backgroundColor: Int,
    val textColor: Int,
    val accentColor: Int,
    val borderColor: Int,
    val hoverColor: Int,
    val disabledColor: Int,
    val font: ResourceLocation? = null,
    val backgroundTexture: ResourceLocation? = null
)

interface IUIThemeRegistry {
    fun register(theme: UITheme)
    fun unregister(themeId: String)
    fun get(themeId: String): UITheme?
    fun getAll(): Map<String, UITheme>
    fun setActiveTheme(themeId: String)
    fun getActiveTheme(): UITheme?
}

object UIThemeRegistry : IUIThemeRegistry {
    private val themes = mutableMapOf<String, UITheme>()
    private var activeThemeId: String? = null

    override fun register(theme: UITheme) {
        themes[theme.id] = theme
    }

    override fun unregister(themeId: String) {
        themes.remove(themeId)
        if (activeThemeId == themeId) {
            activeThemeId = null
        }
    }

    override fun get(themeId: String): UITheme? {
        return themes[themeId]
    }

    override fun getAll(): Map<String, UITheme> {
        return themes.toMap()
    }

    override fun setActiveTheme(themeId: String) {
        if (themes.containsKey(themeId)) {
            activeThemeId = themeId
        }
    }

    override fun getActiveTheme(): UITheme? {
        return activeThemeId?.let { themes[it] }
    }
}

