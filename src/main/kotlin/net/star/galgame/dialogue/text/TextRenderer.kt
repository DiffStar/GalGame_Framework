package net.star.galgame.dialogue.text

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.util.regex.MatchResult
import java.util.regex.Pattern

object TextRenderer {
    private val COLOR_PATTERN = Pattern.compile("\\[color=([0-9a-fA-F]{6})\\](.*?)\\[/color\\]")
    private val BOLD_PATTERN = Pattern.compile("\\[b\\](.*?)\\[/b\\]")
    private val ITALIC_PATTERN = Pattern.compile("\\[i\\](.*?)\\[/i\\]")
    private val UNDERLINE_PATTERN = Pattern.compile("\\[u\\](.*?)\\[/u\\]")
    private val STRIKETHROUGH_PATTERN = Pattern.compile("\\[s\\](.*?)\\[/s\\]")

    fun parseRichText(text: String): Component {
        var result: MutableComponent = Component.literal("")
        var lastIndex = 0

        val matcher = COLOR_PATTERN.matcher(text)
        val matches = mutableListOf<MatchResult>()
        while (matcher.find()) {
            matches.add(matcher.toMatchResult())
        }

        if (matches.isEmpty()) {
            val processed = processSimpleTags(text)
            return Component.literal(processed)
        }

        for (match in matches) {
            if (lastIndex < match.start()) {
                val before = processSimpleTags(text.substring(lastIndex, match.start()))
                result.append(Component.literal(before))
            }
            
            val color = match.group(1)
            val content = match.group(2)
            try {
                val colorInt = Integer.parseInt(color, 16)
                val styledContent = processSimpleTags(content)
                result.append(Component.literal(styledContent).withStyle(Style.EMPTY.withColor(colorInt)))
            } catch (e: Exception) {
                val styledContent = processSimpleTags(content)
                result.append(Component.literal(styledContent))
            }
            
            lastIndex = match.end()
        }

        if (lastIndex < text.length) {
            val after = processSimpleTags(text.substring(lastIndex))
            result.append(Component.literal(after))
        }

        return result
    }

    private fun processSimpleTags(text: String): String {
        var processed = text
        processed = BOLD_PATTERN.matcher(processed).replaceAll("\u00A7l$1\u00A7r")
        processed = ITALIC_PATTERN.matcher(processed).replaceAll("\u00A7o$1\u00A7r")
        processed = UNDERLINE_PATTERN.matcher(processed).replaceAll("\u00A7n$1\u00A7r")
        processed = STRIKETHROUGH_PATTERN.matcher(processed).replaceAll("\u00A7m$1\u00A7r")
        return processed
    }

    fun renderWrappedText(
        graphics: GuiGraphics,
        font: Font,
        text: Component,
        x: Int,
        y: Int,
        maxWidth: Int,
        color: Int
    ): Int {
        val lines = font.split(text, maxWidth)
        var currentY = y
        
        for (line in lines) {
            graphics.drawString(font, line, x, currentY, color, false)
            currentY += font.lineHeight + 2
        }
        
        return currentY - y
    }

    fun getTextHeight(font: Font, text: Component, maxWidth: Int): Int {
        val lines = font.split(text, maxWidth)
        return lines.size * (font.lineHeight + 2)
    }
}

