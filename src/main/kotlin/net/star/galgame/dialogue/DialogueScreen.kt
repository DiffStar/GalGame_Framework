package net.star.galgame.dialogue

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.star.galgame.dialogue.character.CharacterRenderer
import net.star.galgame.dialogue.choice.ChoiceRenderer
import net.star.galgame.dialogue.control.DialogueController
import net.star.galgame.dialogue.i18n.I18nHelper
import net.star.galgame.dialogue.save.SaveManager
import net.star.galgame.dialogue.save.SaveScreen
import net.star.galgame.dialogue.save.LoadScreen
import net.star.galgame.dialogue.menu.SettingsScreen
import net.star.galgame.dialogue.text.TextRenderer
import net.star.galgame.dialogue.text.TypewriterEffect
import net.star.galgame.dialogue.visual.BackgroundManager
import net.star.galgame.dialogue.visual.BackgroundConfig
import net.star.galgame.dialogue.visual.BackgroundType
import net.star.galgame.dialogue.visual.ThemeManager
import net.star.galgame.dialogue.visual.AnimationManager
import net.star.galgame.dialogue.visual.AnimationType
import net.star.galgame.dialogue.visual.UIRenderer
import net.star.galgame.dialogue.visual.UITheme
import net.star.galgame.dialogue.audio.AudioManager
import net.star.galgame.dialogue.achievement.AchievementManager
import net.star.galgame.dialogue.achievement.AchievementNotification
import net.star.galgame.dialogue.statistics.StatisticsManager
import org.lwjgl.glfw.GLFW

class DialogueScreen(
    private val script: DialogueScript,
    controller: DialogueController? = null
) : Screen(Component.literal("Dialogue")) {
    private val controller = controller ?: DialogueController(script)
    private val typewriter = TypewriterEffect("", 0.03f)
    private val characterRenderers = mutableMapOf<String, CharacterRenderer>()
    private val choiceRenderer = ChoiceRenderer()
    private var lastTime = System.currentTimeMillis()
    private var showingHistory = false
    private var historyScrollOffset = 0

    private var lastAutoSaveTime = System.currentTimeMillis()
    private val autoSaveInterval = 30000L
    private var showingQuickMenu = false

    private val backgroundManager = BackgroundManager()
    private val themeManager = ThemeManager()
    private val animationManager = AnimationManager()
    private val uiRenderer = UIRenderer()
    private val audioManager = AudioManager()
    private var dialogueBoxAnimation: net.star.galgame.dialogue.visual.UIAnimation? = null
    private var nameBoxAnimation: net.star.galgame.dialogue.visual.UIAnimation? = null
    
    init {
        script.entries.forEach { entry ->
            if (entry.characterId != null && !characterRenderers.containsKey(entry.characterId)) {
                characterRenderers[entry.characterId!!] = CharacterRenderer()
            }
        }
    }

    override fun init() {
        super.init()
        dialogueBoxAnimation = net.star.galgame.dialogue.visual.UIAnimation(AnimationType.FADE_IN, 0.3f)
        nameBoxAnimation = net.star.galgame.dialogue.visual.UIAnimation(AnimationType.SLIDE_UP, 0.3f)
        animationManager.addAnimation("dialogueBox", dialogueBoxAnimation!!)
        animationManager.addAnimation("nameBox", nameBoxAnimation!!)
        dialogueBoxAnimation?.start()
        nameBoxAnimation?.start()
        StatisticsManager.startGame()
        AchievementManager.addUnlockListener { achievementId ->
            AchievementNotification.showNotification(achievementId)
        }
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

        dialogueBoxAnimation?.start()
        nameBoxAnimation?.start()

        audioManager.se.playSceneChange(ResourceLocation.parse("galgame:scene_change"))
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        backgroundManager.render(graphics, width, height)

        val currentTime = System.currentTimeMillis()
        val deltaTime = ((currentTime - lastTime) / 1000.0f).coerceIn(0f, 0.1f)
        lastTime = currentTime
        
        backgroundManager.update(deltaTime)
        animationManager.update(deltaTime)
        audioManager.update(deltaTime)
        StatisticsManager.update(deltaTime)
        AchievementNotification.update(deltaTime)
        AchievementManager.checkAchievements()
        net.star.galgame.world.scene.SceneManager.update(deltaTime)
        net.star.galgame.world.scene.transition.SceneTransition.update(deltaTime)
        
        if (currentTime - lastAutoSaveTime >= autoSaveInterval) {
            SaveManager.autoSave(script.id, controller)
            lastAutoSaveTime = currentTime
        }

        if (showingHistory) {
            renderHistory(graphics, mouseX, mouseY)
        } else if (showingQuickMenu) {
            renderDialogue(graphics, mouseX, mouseY, deltaTime)
            renderQuickMenu(graphics, mouseX, mouseY)
        } else {
            renderDialogue(graphics, mouseX, mouseY, deltaTime)
            renderQuickMenuButton(graphics, mouseX, mouseY)
            if (controller.hasChoices() && typewriter.isComplete) {
                val choices = controller.getVisibleChoices()
                val screenWidth = width
                val screenHeight = height
                val dialogueBoxY = screenHeight - 120
                val choiceBoxY = dialogueBoxY - choices.size * 35 - 20
                val choiceBoxX = screenWidth / 2 - 200
                val choiceBoxWidth = 400

                val hoveredIndex = choiceRenderer.getChoiceAt(
                    choices,
                    choiceBoxX,
                    choiceBoxY,
                    choiceBoxWidth,
                    mouseX,
                    mouseY
                )
                if (hoveredIndex != null) {
                    choiceRenderer.setHovered(hoveredIndex)
                }
            }
        }

        AchievementNotification.render(graphics, width, height)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun renderDialogue(graphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTime: Float) {
        val entry = controller.getCurrentEntry() ?: return
        val character = if (entry.characterId != null) {
            DialogueManager.getCharacter(entry.characterId)
        } else null

        typewriter.update(deltaTime)
        characterRenderers.values.forEach { it.updateAnimation(deltaTime) }
        choiceRenderer.update(deltaTime)

        val screenWidth = width
        val screenHeight = height
        val theme = themeManager.getCurrentTheme()
        val padding = theme.dialogBox.padding

        val dialogueBoxY = screenHeight - 120
        val dialogueBoxHeight = 100
        val hasChoices = controller.hasChoices() && typewriter.isComplete

        uiRenderer.renderDialogBox(
            graphics,
            theme,
            0,
            dialogueBoxY,
            screenWidth,
            dialogueBoxHeight,
            dialogueBoxAnimation
        )

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
                uiRenderer.renderNameBox(
                    graphics,
                    font,
                    theme,
                    character.name.string,
                    padding + 10,
                    nameY,
                    nameBoxAnimation
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
            theme.text.color
        )

        if (hasChoices) {
            val choices = controller.getVisibleChoices()
            val choiceBoxY = dialogueBoxY - choices.size * 35 - 20
            val choiceBoxX = screenWidth / 2 - 200
            val choiceBoxWidth = 400

            val choiceTheme = UITheme(
                name = theme.name,
                dialogBox = theme.choiceBox,
                text = theme.text,
                nameBox = theme.nameBox,
                choiceBox = theme.choiceBox,
                continueIndicator = theme.continueIndicator
            )
            uiRenderer.renderDialogBox(
                graphics,
                choiceTheme,
                choiceBoxX,
                choiceBoxY,
                choiceBoxWidth,
                choices.size * 35 + 10
            )

            choiceRenderer.render(
                graphics,
                font,
                choices,
                choiceBoxX,
                choiceBoxY,
                choiceBoxWidth,
                mouseX,
                mouseY
            )
        } else if (typewriter.isComplete) {
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
            uiRenderer.renderText(
                graphics,
                font,
                theme,
                continueText,
                screenWidth - font.width(continueText) - padding,
                continueY,
                font.width(continueText)
            )
        }

        val voiceSubtitle = audioManager.voice.getSubtitle()
        if (voiceSubtitle != null) {
            val subtitleY = dialogueBoxY - 40
            graphics.drawString(
                font,
                voiceSubtitle,
                padding + 10,
                subtitleY,
                0xFFFFFF,
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

    private fun renderQuickMenuButton(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val buttonSize = 30
        val buttonX = width - buttonSize - 10
        val buttonY = 10
        val isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonSize &&
                mouseY >= buttonY && mouseY <= buttonY + buttonSize

        val bgColor = if (isHovered) 0xCC000000.toInt() else 0x80000000.toInt()
        graphics.fill(buttonX, buttonY, buttonX + buttonSize, buttonY + buttonSize, bgColor)
        graphics.fill(buttonX, buttonY, buttonX + buttonSize, buttonY + 2, 0xFFFFFFFF.toInt())
        graphics.fill(buttonX, buttonY + buttonSize - 2, buttonX + buttonSize, buttonY + buttonSize, 0xFFFFFFFF.toInt())
        graphics.fill(buttonX, buttonY, buttonX + 2, buttonY + buttonSize, 0xFFFFFFFF.toInt())
        graphics.fill(buttonX + buttonSize - 2, buttonY, buttonX + buttonSize, buttonY + buttonSize, 0xFFFFFFFF.toInt())

        val menuText = "☰"
        val textX = buttonX + buttonSize / 2 - font.width(menuText) / 2
        val textY = buttonY + buttonSize / 2 - font.lineHeight / 2
        graphics.drawString(font, menuText, textX, textY, 0xFFFFFF, false)
    }

    private fun renderQuickMenu(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        graphics.fill(0, 0, width, height, 0xE0000000.toInt())

        val menuWidth = 200
        val menuHeight = 200
        val menuX = width / 2 - menuWidth / 2
        val menuY = height / 2 - menuHeight / 2

        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xFF2C2C2C.toInt())
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + 2, 0xFFFFFFFF.toInt())
        graphics.fill(menuX, menuY + menuHeight - 2, menuX + menuWidth, menuY + menuHeight, 0xFFFFFFFF.toInt())
        graphics.fill(menuX, menuY, menuX + 2, menuY + menuHeight, 0xFFFFFFFF.toInt())
        graphics.fill(menuX + menuWidth - 2, menuY, menuX + menuWidth, menuY + menuHeight, 0xFFFFFFFF.toInt())

        val title = Component.literal("快捷菜单")
        graphics.drawString(font, title, menuX + menuWidth / 2 - font.width(title) / 2, menuY + 15, 0xFFFFFF, false)

        val buttonHeight = 30
        val buttonSpacing = 5
        var currentY = menuY + 40

        val buttons = listOf(
            Pair("存档 (F5)") { openSaveScreen() },
            Pair("读档 (F9)") { openLoadScreen() },
            Pair("设置") { openSettings() },
            Pair("返回游戏") { showingQuickMenu = false }
        )

        for ((text, action) in buttons) {
            val isHovered = mouseX >= menuX + 10 && mouseX <= menuX + menuWidth - 10 &&
                    mouseY >= currentY && mouseY <= currentY + buttonHeight
            val bgColor = if (isHovered) 0xFF4A90E2.toInt() else 0xFF3C3C3C.toInt()
            graphics.fill(menuX + 10, currentY, menuX + menuWidth - 10, currentY + buttonHeight, bgColor)
            graphics.drawString(font, text, menuX + menuWidth / 2 - font.width(text) / 2,
                currentY + buttonHeight / 2 - font.lineHeight / 2, 0xFFFFFF, false)
            currentY += buttonHeight + buttonSpacing
        }
    }

    private fun openSaveScreen() {
        minecraft?.setScreen(SaveScreen(script.id, controller) {
            minecraft?.setScreen(this@DialogueScreen)
        })
        showingQuickMenu = false
    }

    private fun openLoadScreen() {
        minecraft?.setScreen(LoadScreen(
            onLoadComplete = { loadedScreen ->
                minecraft?.setScreen(loadedScreen)
            },
            onCancel = {
                minecraft?.setScreen(this@DialogueScreen)
            }
        ))
        showingQuickMenu = false
    }

    private fun openSettings() {
        minecraft?.setScreen(SettingsScreen {
            minecraft?.setScreen(this@DialogueScreen)
        })
        showingQuickMenu = false
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
                if (controller.hasChoices() && typewriter.isComplete) {
                    return true
                }
                if (typewriter.isComplete) {
                    if (controller.next()) {
                        StatisticsManager.incrementDialogueCount()
                        updateCurrentDialogue()
                    } else {
                        onClose()
                    }
                } else {
                    typewriter.skip()
                }
                return true
            }
            GLFW.GLFW_KEY_1 -> {
                if (controller.hasChoices() && typewriter.isComplete) {
                    handleChoiceSelection(0)
                    return true
                }
            }
            GLFW.GLFW_KEY_2 -> {
                if (controller.hasChoices() && typewriter.isComplete) {
                    handleChoiceSelection(1)
                    return true
                }
            }
            GLFW.GLFW_KEY_3 -> {
                if (controller.hasChoices() && typewriter.isComplete) {
                    handleChoiceSelection(2)
                    return true
                }
            }
            GLFW.GLFW_KEY_4 -> {
                if (controller.hasChoices() && typewriter.isComplete) {
                    handleChoiceSelection(3)
                    return true
                }
            }
            GLFW.GLFW_KEY_5 -> {
                if (controller.hasChoices() && typewriter.isComplete) {
                    handleChoiceSelection(4)
                    return true
                }
            }
            GLFW.GLFW_KEY_6 -> {
                if (controller.hasChoices() && typewriter.isComplete) {
                    handleChoiceSelection(5)
                    return true
                }
            }
            GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> {
                controller.fastForward()
                return true
            }
            GLFW.GLFW_KEY_H -> {
                if (showingQuickMenu) {
                    showingQuickMenu = false
                } else {
                    showingHistory = true
                    historyScrollOffset = 0
                }
                return true
            }
            GLFW.GLFW_KEY_M -> {
                showingQuickMenu = !showingQuickMenu
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
            GLFW.GLFW_KEY_F5 -> {
                minecraft?.setScreen(SaveScreen(script.id, controller) {
                    minecraft?.setScreen(this@DialogueScreen)
                })
                return true
            }
            GLFW.GLFW_KEY_F9 -> {
                minecraft?.setScreen(LoadScreen(
                    onLoadComplete = { loadedScreen ->
                        minecraft?.setScreen(loadedScreen)
                    },
                    onCancel = {
                        minecraft?.setScreen(this@DialogueScreen)
                    }
                ))
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

        if (showingQuickMenu) {
            val menuWidth = 200
            val menuHeight = 200
            val menuX = width / 2 - menuWidth / 2
            val menuY = height / 2 - menuHeight / 2

            if (event.button() == 0) {
                val mouseX = event.x().toInt()
                val mouseY = event.y().toInt()

                if (mouseX < menuX || mouseX > menuX + menuWidth ||
                    mouseY < menuY || mouseY > menuY + menuHeight) {
                    showingQuickMenu = false
                    return true
                }

                val buttonHeight = 30
                val buttonSpacing = 5
                var currentY = menuY + 40

                val buttons = listOf(
                    { openSaveScreen() },
                    { openLoadScreen() },
                    { openSettings() },
                    { showingQuickMenu = false }
                )

                for (action in buttons) {
                    if (mouseY >= currentY && mouseY <= currentY + buttonHeight) {
                        action()
                        return true
                    }
                    currentY += buttonHeight + buttonSpacing
                }
            }
            return true
        }

        if (event.button() == 0) {
            val buttonSize = 30
            val buttonX = width - buttonSize - 10
            val buttonY = 10
            val mouseX = event.x().toInt()
            val mouseY = event.y().toInt()

            if (mouseX >= buttonX && mouseX <= buttonX + buttonSize &&
                mouseY >= buttonY && mouseY <= buttonY + buttonSize) {
                showingQuickMenu = true
                return true
            }
        }

        if (event.button() == 0) {
            if (controller.hasChoices() && typewriter.isComplete) {
                val choices = controller.getVisibleChoices()
                val screenWidth = width
                val screenHeight = height
                val dialogueBoxY = screenHeight - 120
                val choiceBoxY = dialogueBoxY - choices.size * 35 - 20
                val choiceBoxX = screenWidth / 2 - 200
                val choiceBoxWidth = 400

                val choiceIndex = choiceRenderer.getChoiceAt(
                    choices,
                    choiceBoxX,
                    choiceBoxY,
                    choiceBoxWidth,
                    event.x().toInt(),
                    event.y().toInt()
                )

                if (choiceIndex != null) {
                    handleChoiceSelection(choiceIndex)
                    return true
                }
            } else if (typewriter.isComplete) {
                audioManager.se.playClick(ResourceLocation.parse("galgame:click"))
                if (controller.next()) {
                    StatisticsManager.incrementDialogueCount()
                    updateCurrentDialogue()
                } else {
                    onClose()
                }
            } else {
                typewriter.skip()
                if (audioManager.voice.isPlaying()) {
                    audioManager.voice.skip()
                }
            }
            return true
        }

        return super.mouseClicked(event, captured)
    }

    private fun handleChoiceSelection(choiceIndex: Int) {
        audioManager.se.playChoice(ResourceLocation.parse("galgame:choice"))
        if (controller.selectChoice(choiceIndex)) {
            StatisticsManager.incrementChoiceCount()
            updateCurrentDialogue()
            choiceRenderer.reset()
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (showingHistory) {
            historyScrollOffset = (historyScrollOffset - scrollY.toInt() * 10).coerceAtLeast(0)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        audioManager.stopAll()
        StatisticsManager.stopGame()
        net.star.galgame.world.scene.SceneManager.resetCamera()
        super.onClose()
    }
}

