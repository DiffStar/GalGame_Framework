package net.star.galgame.dialogue.achievement

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.star.galgame.dialogue.condition.Condition

data class AchievementDefinition(
    val id: String,
    val name: Component,
    val description: Component,
    val icon: ResourceLocation?,
    val condition: Condition,
    val hidden: Boolean = false,
    val category: String = "default",
    val points: Int = 0
)

