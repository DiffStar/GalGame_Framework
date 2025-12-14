package net.star.galgame.dialogue.i18n

import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component

object I18nHelper {
    fun translate(key: String, vararg args: Any): String {
        return I18n.get(key, *args)
    }

    fun translateComponent(key: String, vararg args: Any): Component {
        return Component.translatable(key, *args)
    }

    fun hasTranslation(key: String): Boolean {
        return I18n.exists(key)
    }
}

