package net.star.galgame.contentpack.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.star.galgame.contentpack.ContentPackManager
import net.star.galgame.contentpack.ContentPackValidator
import net.star.galgame.contentpack.InstalledPackInfo
import net.star.galgame.contentpack.ValidationReport
import org.lwjgl.glfw.GLFW
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ContentPackManagementScreen(
    private val parent: Screen?,
    private val packsDirectory: Path = Paths.get("contentpacks")
) : Screen(Component.literal("内容包管理")) {
    private var packs: List<InstalledPackInfo> = emptyList()
    private var selectedPack: InstalledPackInfo? = null
    private var scrollOffset = 0
    private var showingDetails = false
    private var validationReport: ValidationReport? = null
    private val packsPerPage = 8
    
    override fun init() {
        super.init()
        refreshPacks()
        
        addRenderableWidget(
            Button.builder(Component.literal("导入内容包")) {
                minecraft?.setScreen(ContentPackImportScreen(this, packsDirectory) {
                    refreshPacks()
                    minecraft?.setScreen(this@ContentPackManagementScreen)
                })
            }
                .bounds(width / 2 - 200, height - 30, 120, 20)
                .build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("验证")) {
                selectedPack?.let { pack ->
                    val manifest = ContentPackManager.getPack(pack.packId)?.manifest
                        ?: ContentPackManager.discoverPacks(packsDirectory)
                            .firstOrNull { it.pack.manifest.id == pack.packId }
                            ?.pack?.manifest
                        ?: return@let
                    val report = ContentPackValidator.validatePack(pack.packPath, manifest)
                    validationReport = report
                    showingDetails = true
                }
            }
                .bounds(width / 2 - 60, height - 30, 60, 20)
                .build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("刷新")) {
                refreshPacks()
            }
                .bounds(width / 2 + 10, height - 30, 60, 20)
                .build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("返回")) {
                onClose()
            }
                .bounds(width / 2 + 80, height - 30, 60, 20)
                .build()
        )
    }
    
    private fun refreshPacks() {
        packs = ContentPackManager.getAllInstalledPacks(packsDirectory)
        selectedPack = null
        showingDetails = false
        validationReport = null
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        graphics.drawString(font, Component.literal("内容包管理"), width / 2 - font.width("内容包管理") / 2, 20, 0xFFFFFF, false)
        
        if (showingDetails && selectedPack != null) {
            renderPackDetails(graphics, selectedPack!!, validationReport)
        } else {
            renderPackList(graphics, mouseX, mouseY)
        }
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    private fun renderPackList(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val startIndex = scrollOffset
        val endIndex = (startIndex + packsPerPage).coerceAtMost(packs.size)
        
        var yOffset = 50
        for (i in startIndex until endIndex) {
            val pack = packs[i]
            val packY = yOffset + (i - startIndex) * 60
            renderPackEntry(graphics, pack, 20, packY, width - 40, 55, mouseX, mouseY, pack == selectedPack)
        }
        
        if (packs.size > packsPerPage) {
            val scrollBarX = width - 25
            val scrollBarY = 50
            val scrollBarHeight = packsPerPage * 60
            val scrollBarThumbHeight = (scrollBarHeight * packsPerPage / packs.size.toFloat()).toInt().coerceAtLeast(20)
            val scrollBarThumbY = scrollBarY + (scrollOffset * scrollBarHeight / packs.size)
            
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 10, scrollBarY + scrollBarHeight, 0x80000000.toInt())
            graphics.fill(scrollBarX, scrollBarThumbY, scrollBarX + 10, scrollBarThumbY + scrollBarThumbHeight, 0xFF808080.toInt())
        }
        
        if (packs.isEmpty()) {
            graphics.drawString(font, Component.literal("未找到内容包"), width / 2 - font.width("未找到内容包") / 2, height / 2, 0x888888, false)
        }
    }
    
    private fun renderPackEntry(
        graphics: GuiGraphics,
        pack: InstalledPackInfo,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        isSelected: Boolean
    ) {
        val isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        
        val bgColor = when {
            isSelected -> 0xFF4A90E2.toInt()
            isHovered -> 0xFF555555.toInt()
            else -> 0xFF333333.toInt()
        }
        
        graphics.fill(x, y, x + width, y + height, bgColor)
        graphics.fill(x, y, x + width, y + 2, 0xFFFFFFFF.toInt())
        
        val statusColor = when {
            !pack.isLoaded -> 0xFF888888.toInt()
            pack.isEnabled -> 0xFF00FF00.toInt()
            else -> 0xFFFF0000.toInt()
        }
        val statusText = when {
            !pack.isLoaded -> "未加载"
            pack.isEnabled -> "已启用"
            else -> "已禁用"
        }
        
        graphics.drawString(font, Component.literal(pack.name), x + 10, y + 5, 0xFFFFFF, false)
        graphics.drawString(font, Component.literal("版本: ${pack.version}"), x + 10, y + 20, 0xCCCCCC, false)
        graphics.drawString(font, Component.literal(statusText), x + width - 80, y + 5, statusColor, false)
        
        if (pack.author != null) {
            graphics.drawString(font, Component.literal("作者: ${pack.author}"), x + 10, y + 35, 0xAAAAAA, false)
        }
        
        if (pack.loadErrors.isNotEmpty()) {
            graphics.drawString(font, Component.literal("错误: ${pack.loadErrors.size}"), x + width - 80, y + 20, 0xFFFF00, false)
        }
        
        val buttonY = y + 30
        val buttonWidth = 50
        val buttonHeight = 20
        val buttonX1 = x + width - 200
        val buttonX2 = x + width - 140
        val buttonX3 = x + width - 80
        
        val button1Hovered = mouseX >= buttonX1 && mouseX <= buttonX1 + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight
        val button2Hovered = mouseX >= buttonX2 && mouseX <= buttonX2 + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight
        val button3Hovered = mouseX >= buttonX3 && mouseX <= buttonX3 + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight
        
        val enableText = if (pack.isLoaded && pack.isEnabled) "禁用" else if (pack.isLoaded) "启用" else "加载"
        val button1Color = if (button1Hovered) 0xFFFFFF00.toInt() else 0xFFFFFFFF.toInt()
        val button2Color = if (button2Hovered) 0xFFFFFF00.toInt() else 0xFFFFFFFF.toInt()
        val button3Color = if (button3Hovered) 0xFFFFFF00.toInt() else 0xFFFFFFFF.toInt()
        
        graphics.drawString(font, Component.literal(enableText), buttonX1, buttonY + 5, button1Color, false)
        graphics.drawString(font, Component.literal("卸载"), buttonX2, buttonY + 5, button2Color, false)
        graphics.drawString(font, Component.literal("详情"), buttonX3, buttonY + 5, button3Color, false)
    }
    
    private fun renderPackDetails(
        graphics: GuiGraphics,
        pack: InstalledPackInfo,
        report: ValidationReport?
    ) {
        graphics.fill(20, 50, width - 20, height - 80, 0xE0000000.toInt())
        
        var y = 60
        graphics.drawString(font, Component.literal("内容包信息"), 30, y, 0xFFFFFF, false)
        y += 20
        
        graphics.drawString(font, Component.literal("名称: ${pack.name}"), 30, y, 0xCCCCCC, false)
        y += 15
        graphics.drawString(font, Component.literal("ID: ${pack.packId}"), 30, y, 0xCCCCCC, false)
        y += 15
        graphics.drawString(font, Component.literal("版本: ${pack.version}"), 30, y, 0xCCCCCC, false)
        y += 15
        
        if (pack.author != null) {
            graphics.drawString(font, Component.literal("作者: ${pack.author}"), 30, y, 0xCCCCCC, false)
            y += 15
        }
        
        if (pack.description != null) {
            val descLines = wrapText(pack.description, width - 60)
            descLines.forEach { line ->
                graphics.drawString(font, Component.literal(line), 30, y, 0xAAAAAA, false)
                y += 15
            }
        }
        
        y += 10
        
        val statusText = when {
            !pack.isLoaded -> "状态: 未加载"
            pack.isEnabled -> "状态: 已启用"
            else -> "状态: 已禁用"
        }
        graphics.drawString(font, Component.literal(statusText), 30, y, 0xCCCCCC, false)
        y += 15
        
        if (pack.loadErrors.isNotEmpty()) {
            graphics.drawString(font, Component.literal("加载错误:"), 30, y, 0xFFFF00, false)
            y += 15
            pack.loadErrors.take(5).forEach { error ->
                graphics.drawString(font, Component.literal("  - $error"), 30, y, 0xFF6666, false)
                y += 15
            }
        }
        
        if (report != null) {
            y += 10
            graphics.drawString(font, Component.literal("验证报告:"), 30, y, 0xFFFFFF, false)
            y += 15
            
            val reportColor = if (report.isValid) 0xFF00FF00.toInt() else 0xFFFF0000.toInt()
            graphics.drawString(font, Component.literal("状态: ${if (report.isValid) "通过" else "失败"}"), 30, y, reportColor, false)
            y += 15
            
            if (report.errors.isNotEmpty()) {
                graphics.drawString(font, Component.literal("错误 (${report.errors.size}):"), 30, y, 0xFFFF00, false)
                y += 15
                report.errors.take(5).forEach { error ->
                    graphics.drawString(font, Component.literal("  - $error"), 30, y, 0xFF6666, false)
                    y += 15
                }
            }
            
            if (report.warnings.isNotEmpty()) {
                graphics.drawString(font, Component.literal("警告 (${report.warnings.size}):"), 30, y, 0xFFFF00, false)
                y += 15
                report.warnings.take(5).forEach { warning ->
                    graphics.drawString(font, Component.literal("  - $warning"), 30, y, 0xFFFFAA, false)
                    y += 15
                }
            }
        }
    }
    
    private fun wrapText(text: String, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (font.width(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() == 0 && !showingDetails) {
            val startIndex = scrollOffset
            val endIndex = (startIndex + packsPerPage).coerceAtMost(packs.size)
            val mouseX = event.x()
            val mouseY = event.y()
            
            for (i in startIndex until endIndex) {
                val pack = packs[i]
                val packY = 50 + (i - startIndex) * 60
                
                if (mouseX >= 20.0 && mouseX <= width - 20.0 &&
                    mouseY >= packY.toDouble() && mouseY <= packY + 55.0) {
                    selectedPack = pack
                    
                    val buttonY = packY + 30
                    val buttonWidth = 50
                    val buttonHeight = 20
                    
                    val buttonX1 = 20 + width - 40 - 200
                    val buttonX2 = 20 + width - 40 - 140
                    val buttonX3 = 20 + width - 40 - 80
                    
                    if (mouseX >= buttonX1.toDouble() && mouseX <= buttonX1 + buttonWidth &&
                        mouseY >= buttonY.toDouble() && mouseY <= buttonY + buttonHeight) {
                        if (pack.isLoaded) {
                            ContentPackManager.setPackEnabled(pack.packId, !pack.isEnabled)
                            refreshPacks()
                        } else {
                            ContentPackManager.loadPack(pack.packPath)
                            refreshPacks()
                        }
                        return true
                    }
                    
                    if (mouseX >= buttonX2.toDouble() && mouseX <= buttonX2 + buttonWidth &&
                        mouseY >= buttonY.toDouble() && mouseY <= buttonY + buttonHeight) {
                        minecraft?.setScreen(ConfirmScreen(
                            Component.literal("确定要卸载内容包 '${pack.name}' 吗？"),
                            { confirmed ->
                                if (confirmed) {
                                    ContentPackManager.deletePack(pack.packId, packsDirectory)
                                    refreshPacks()
                                }
                                minecraft?.setScreen(this@ContentPackManagementScreen)
                            }
                        ))
                        return true
                    }
                    
                    if (mouseX >= buttonX3.toDouble() && mouseX <= buttonX3 + buttonWidth &&
                        mouseY >= buttonY.toDouble() && mouseY <= buttonY + buttonHeight) {
                        showingDetails = true
                        validationReport = null
                        return true
                    }
                    
                    return true
                }
            }
        }
        
        return super.mouseClicked(event, captured)
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (!showingDetails && packs.size > packsPerPage) {
            scrollOffset = (scrollOffset - scrollY.toInt()).coerceIn(0, packs.size - packsPerPage)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            if (showingDetails) {
                showingDetails = false
                validationReport = null
                return true
            } else {
                onClose()
                return true
            }
        }
        return super.keyPressed(event)
    }
    
    override fun onClose() {
        minecraft?.setScreen(parent)
    }
}

class ContentPackImportScreen(
    private val parent: Screen,
    private val packsDirectory: Path,
    private val onImportComplete: () -> Unit
) : Screen(Component.literal("导入内容包")) {
    override fun init() {
        super.init()
        
        addRenderableWidget(
            Button.builder(Component.literal("选择文件夹")) {
                minecraft?.setScreen(ContentPackFolderSelectScreen(this, packsDirectory) { selectedPath ->
                    if (selectedPath != null) {
                        ContentPackManager.importPack(selectedPath, packsDirectory)
                        onImportComplete()
                    }
                    minecraft?.setScreen(this@ContentPackImportScreen)
                })
            }
                .bounds(width / 2 - 100, height / 2 - 20, 200, 20)
                .build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("返回")) {
                onClose()
            }
                .bounds(width / 2 - 100, height / 2 + 10, 200, 20)
                .build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, Component.literal("导入内容包"), width / 2, 50, 0xFFFFFF)
        graphics.drawCenteredString(font, Component.literal("选择内容包文件夹"), width / 2, height / 2 - 50, 0xCCCCCC)
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun onClose() {
        minecraft?.setScreen(parent)
    }
}

class ContentPackFolderSelectScreen(
    private val parent: Screen,
    private val defaultPath: Path,
    private val onSelect: (Path?) -> Unit
) : Screen(Component.literal("选择内容包文件夹")) {
    private var currentPath: Path = defaultPath
    private var directories: List<Path> = emptyList()
    private var scrollOffset = 0
    private val itemsPerPage = 10
    
    override fun init() {
        super.init()
        refreshDirectories()
        
        addRenderableWidget(
            Button.builder(Component.literal("选择当前文件夹")) {
                if (Files.exists(currentPath.resolve("manifest.toml"))) {
                    onSelect(currentPath)
                    onClose()
                }
            }
                .bounds(width / 2 - 100, height - 50, 200, 20)
                .build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("返回")) {
                onClose()
            }
                .bounds(width / 2 - 100, height - 25, 200, 20)
                .build()
        )
    }
    
    private fun refreshDirectories() {
        directories = if (Files.exists(currentPath) && Files.isDirectory(currentPath)) {
            try {
                Files.list(currentPath)
                    .filter { Files.isDirectory(it) }
                    .sorted()
                    .toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        
        graphics.drawCenteredString(font, Component.literal("选择内容包文件夹"), width / 2, 20, 0xFFFFFF)
        graphics.drawString(font, Component.literal("当前路径: $currentPath"), 20, 50, 0xCCCCCC, false)
        
        var y = 70
        val startIndex = scrollOffset
        val endIndex = (startIndex + itemsPerPage).coerceAtMost(directories.size)
        
        for (i in startIndex until endIndex) {
            val dir = directories[i]
            val dirY = y + (i - startIndex) * 20
            val hasManifest = Files.exists(dir.resolve("manifest.toml"))
            val color = if (hasManifest) 0xFF00FF00.toInt() else 0xCCCCCC.toInt()
            graphics.drawString(font, Component.literal(dir.fileName.toString()), 30, dirY, color, false)
        }
        
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun mouseClicked(event: MouseButtonEvent, captured: Boolean): Boolean {
        if (event.button() == 0) {
            val startIndex = scrollOffset
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(directories.size)
            val mouseY = event.y()
            
            for (i in startIndex until endIndex) {
                val dirY = 70 + (i - startIndex) * 20
                if (mouseY >= dirY.toDouble() && mouseY <= dirY + 20.0) {
                    currentPath = directories[i]
                    refreshDirectories()
                    scrollOffset = 0
                    return true
                }
            }
        }
        return super.mouseClicked(event, captured)
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (directories.size > itemsPerPage) {
            scrollOffset = (scrollOffset - scrollY.toInt()).coerceIn(0, directories.size - itemsPerPage)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }
    
    override fun onClose() {
        onSelect(null)
        minecraft?.setScreen(parent)
    }
}

class ConfirmScreen(
    private val message: Component,
    private val onConfirm: (Boolean) -> Unit
) : Screen(Component.literal("确认")) {
    override fun init() {
        super.init()
        
        addRenderableWidget(
            Button.builder(Component.literal("确认")) { onConfirm(true) }
                .bounds(width / 2 - 105, height / 2 + 20, 100, 20)
                .build()
        )
        
        addRenderableWidget(
            Button.builder(Component.literal("取消")) { onConfirm(false) }
                .bounds(width / 2 + 5, height / 2 + 20, 100, 20)
                .build()
        )
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics, mouseX, mouseY, partialTick)
        graphics.drawCenteredString(font, message, width / 2, height / 2 - 20, 0xFFFFFF)
        super.render(graphics, mouseX, mouseY, partialTick)
    }
    
    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == GLFW.GLFW_KEY_ESCAPE) {
            onConfirm(false)
            return true
        }
        return super.keyPressed(event)
    }
}

