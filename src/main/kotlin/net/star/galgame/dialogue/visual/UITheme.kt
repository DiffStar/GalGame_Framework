package net.star.galgame.dialogue.visual

data class DialogBoxStyle(
    val backgroundColor: Int = 0x80000000.toInt(),
    val borderColor: Int = 0xFFFFFFFF.toInt(),
    val borderThickness: Int = 2,
    val cornerRadius: Int = 0,
    val blur: Float = 0f,
    val padding: Int = 20
)

data class TextStyle(
    val color: Int = 0xFFFFFF,
    val shadow: Boolean = false,
    val size: Float = 1f
)

data class NameBoxStyle(
    val backgroundColor: Int = 0xCC000000.toInt(),
    val textColor: Int = 0xFFFFFF,
    val borderColor: Int = 0xFFFFFFFF.toInt(),
    val padding: Int = 10
)

data class UITheme(
    val name: String,
    val dialogBox: DialogBoxStyle,
    val text: TextStyle,
    val nameBox: NameBoxStyle,
    val choiceBox: DialogBoxStyle,
    val continueIndicator: TextStyle
) {
    companion object {
        val DEFAULT = UITheme(
            name = "default",
            dialogBox = DialogBoxStyle(),
            text = TextStyle(),
            nameBox = NameBoxStyle(),
            choiceBox = DialogBoxStyle(backgroundColor = 0x80000000.toInt()),
            continueIndicator = TextStyle(color = 0xCCCCCC)
        )

        val GLASS = UITheme(
            name = "glass",
            dialogBox = DialogBoxStyle(
                backgroundColor = 0x40000000.toInt(),
                blur = 0.5f,
                cornerRadius = 10
            ),
            text = TextStyle(),
            nameBox = NameBoxStyle(
                backgroundColor = 0x60000000.toInt(),
                borderColor = 0x80FFFFFF.toInt()
            ),
            choiceBox = DialogBoxStyle(
                backgroundColor = 0x40000000.toInt(),
                blur = 0.3f,
                cornerRadius = 5
            ),
            continueIndicator = TextStyle(color = 0xFFFFFF)
        )

        val MODERN = UITheme(
            name = "modern",
            dialogBox = DialogBoxStyle(
                backgroundColor = 0xE0000000.toInt(),
                borderColor = 0xFF00FF00.toInt(),
                borderThickness = 3,
                cornerRadius = 15
            ),
            text = TextStyle(color = 0xFFFFFF, shadow = true),
            nameBox = NameBoxStyle(
                backgroundColor = 0xFF000000.toInt(),
                textColor = 0xFF00FF00.toInt(),
                borderColor = 0xFF00FF00.toInt()
            ),
            choiceBox = DialogBoxStyle(
                backgroundColor = 0xE0000000.toInt(),
                borderColor = 0xFF00FF00.toInt(),
                borderThickness = 2,
                cornerRadius = 10
            ),
            continueIndicator = TextStyle(color = 0xFF00FF00.toInt())
        )
    }
}

class ThemeManager {
    private var currentTheme: UITheme = UITheme.DEFAULT
    private val themes = mapOf(
        UITheme.DEFAULT.name to UITheme.DEFAULT,
        UITheme.GLASS.name to UITheme.GLASS,
        UITheme.MODERN.name to UITheme.MODERN
    )

    fun setTheme(themeName: String) {
        themes[themeName]?.let { currentTheme = it }
    }

    fun setCustomTheme(theme: UITheme) {
        currentTheme = theme
    }

    fun getCurrentTheme(): UITheme = currentTheme

    fun getAvailableThemes(): List<String> = themes.keys.toList()
}

