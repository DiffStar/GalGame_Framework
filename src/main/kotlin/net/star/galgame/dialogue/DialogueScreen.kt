package net.star.galgame.dialogue

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.star.galgame.dialogue.character.CharacterRenderer
import net.star.galgame.dialogue.control.DialogueController
import net.star.galgame.dialogue.i18n.I18nHelper
import net.star.galgame.dialogue.text.TextRenderer
import net.star.galgame.dialogue.text.TypewriterEffect
import org.lwjgl.glfw.GLFW

class DialogueScreen(
    private val script: DialogueScript
) : Screen(Component.literal("Dialogue")) {
    private val controller = DialogueController(script)
    private val typewriter = TypewriterEffect("", 0.03f)
    private val characterRenderers = mutableMapOf<String, CharacterRenderer>()
    private var lastTime = System.currentTimeMillis()
    private var showingHistory = false
    private var historyScrollOffset = 0

    init {
        script.entries.forEach { entry ->
            if (entry.characterId != null && !characterRenderers.containsKey(entry.characterId)) {
                characterRenderers[entry.characterId!!] = CharacterRenderer()
            }
        }
    }

    override fun init() {
        super.init()
        updateCurrentDialogue()
    }

    private fun updateCurrentDialogue() {
        val entry = controller.getCurrentEntry() ?: return
        val character = if (entry.characterId != null) {
            DialogueManager.getCharacter(entry.characterId)
        } else null

        val text = entry.text
        typewriter.reset()
        typewriter.fullText = text
        typewriter.speed = if (controller.isFastForwarding()) 0.001f else 0.03f

        characterRenderers.values.forEach { it.fadeOut() }
        if (character != null) {
            characterRenderers[character.id]?.fadeIn()
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)

        val currentTime = System.currentTimeMillis()
        val deltaTime = ((currentTime - lastTime) / 1000.0f).coerceIn(0f, 0.1f)
        lastTime = currentTime

        if (showingHistory) {
            renderHistory(graphics, mouseX, mouseY)
        } else {
            renderDialogue(graphics, mouseX, mouseY, deltaTime)
        }

        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun renderDialogue(graphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTime: Float) {
        val entry = controller.getCurrentEntry() ?: return
        val character = if (entry.characterId != null) {
            DialogueManager.getCharacter(entry.characterId)
        } else null

        typewriter.update(deltaTime)
        characterRenderers.values.forEach { it.updateFade() }

        val screenWidth = width
        val screenHeight = height

        val dialogueBoxY = screenHeight - 120
        val dialogueBoxHeight = 100
        val padding = 20

        graphics.fill(0, dialogueBoxY, screenWidth, screenHeight, 0x80000000.toInt())
        graphics.fill(0, dialogueBoxY, screenWidth, dialogueBoxY + 2, 0xFFFFFFFF.toInt())

        if (character != null) {
            val characterRenderer = characterRenderers[character.id]
            if (characterRenderer != null) {
                val expression = entry.expression
                val portraitPath = character.expressions[expression] ?: character.portraitPath

            val portraitWidth = 200
            val portraitHeight = 300
            val portraitX = when (entry.position) {
                CharacterPosition.LEFT -> padding
                CharacterPosition.CENTER -> screenWidth / 2 - portraitWidth / 2
                CharacterPosition.RIGHT -> screenWidth - portraitWidth - padding
            }

                characterRenderer.render(
                    graphics,
                    portraitPath,
                    entry.position,
                    portraitX,
                    dialogueBoxY,
                    portraitWidth,
                    portraitHeight,
                    0.8f
                )

                val nameY = dialogueBoxY - 20
                graphics.drawString(
                    font,
                    character.name,
                    padding + 10,
                    nameY,
                    0xFFFFFF,
                    false
                )
            }
        }

        val textX = padding + 10
        val textY = dialogueBoxY + 15
        val textWidth = screenWidth - textX - padding - 10

        val currentText = typewriter.getCurrentText()
        val textComponent = TextRenderer.parseRichText(currentText)
        TextRenderer.renderWrappedText(
            graphics,
            font,
            textComponent,
            textX,
            textY,
            textWidth,
            0xFFFFFF
        )

        if (typewriter.isComplete) {
            val continueY = screenHeight - 30
            val continueText = if (controller.isComplete()) {
                if (I18nHelper.hasTranslation("galgame.dialogue.click_close")) {
                    Component.translatable("galgame.dialogue.click_close")
                } else {
                    Component.literal("点击关闭")
                }
            } else {
                if (I18nHelper.hasTranslation("galgame.dialogue.click_continue")) {
                    Component.translatable("galgame.dialogue.click_continue")
                } else {
                    Component.literal("点击继续...")
                }
            }
            graphics.drawString(
                font,
                continueText,
                screenWidth - font.width(continueText) - padding,
                continueY,
                0xCCCCCC,
                false
            )
        }
    }

    private fun renderHistory(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val history = controller.getHistory()
        val screenWidth = width
        val screenHeight = height
        val padding = 20
        val startY = padding + 20
        var currentY = startY - historyScrollOffset

        graphics.fill(0, 0, screenWidth, screenHeight, 0xE0000000.toInt())

        val historyTitle = if (I18nHelper.hasTranslation("galgame.dialogue.history")) {
            Component.translatable("galgame.dialogue.history")
        } else {
            Component.literal("对话历史 (按H关闭)")
        }
        graphics.drawString(
            font,
            historyTitle,
            padding,
            padding,
            0xFFFFFF,
            false
        )

        for (entry in history) {
            if (currentY > screenHeight) break
            if (currentY + 50 < 0) {
                currentY += 50
                continue
            }

            val character = if (entry.characterId != null) {
                DialogueManager.getCharacter(entry.characterId)
            } else null

            if (character != null) {
                graphics.drawString(
                    font,
                    character.name,
                    padding,
                    currentY,
                    0xFFFF00,
                    false
                )
                currentY += font.lineHeight + 5
            }

            val textComponent = TextRenderer.parseRichText(entry.text)
            val textHeight = TextRenderer.getTextHeight(font, textComponent, screenWidth - padding * 2)
            TextRenderer.renderWrappedText(
                graphics,
                font,
                textComponent,
                padding,
                currentY,
                screenWidth - padding * 2,
                0xFFFFFF
            )
            currentY += textHeight + 20
        }
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.key
        if (showingHistory) {
            if (keyCode == GLFW.GLFW_KEY_H) {
                showingHistory = false
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                historyScrollOffset = (historyScrollOffset - 10).coerceAtLeast(0)
                return true
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                historyScrollOffset += 10
                return true
            }
            return super.keyPressed(event)
        }

        when (keyCode) {
            GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_ENTER -> {
                if (typewriter.isComplete) {
                    if (controller.next()) {
                        updateCurrentDialogue()
                    } else {
                        onClose()
                    }
                } else {
                    typewriter.skip()
                }
                return true
            }
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> {
                controller.fastForward()
                return true
            }
            GLFW.GLFW_KEY_H -> {
                showingHistory = true
                historyScrollOffset = 0
                return true
            }
            GLFW.GLFW_KEY_S -> {
                controller.skip()
                updateCurrentDialogue()
                return true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                onClose()
                return true
            }
        }

        return super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        val keyCode = event.key
        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            controller.stopFastForward()
            return true
        }
        return super.keyReleased(event)
    }

    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (showingHistory) {
            return super.mouseClicked(event, captured)
        }

        if (event.button() == 0) {
            if (typewriter.isComplete) {
                if (controller.next()) {
                    updateCurrentDialogue()
                } else {
                    onClose()
                }
            } else {
                typewriter.skip()
            }
            return true
        }

        return super.mouseClicked(event, captured)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (showingHistory) {
            historyScrollOffset = (historyScrollOffset - scrollY.toInt() * 10).coerceAtLeast(0)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun isPauseScreen(): Boolean = false
}

