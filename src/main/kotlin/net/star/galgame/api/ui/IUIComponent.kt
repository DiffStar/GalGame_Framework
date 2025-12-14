package net.star.galgame.api.ui

import net.minecraft.client.gui.GuiGraphics

interface IUIComponent {
    val id: String
    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float)
    fun onMouseClick(mouseX: Double, mouseY: Double, button: Int): Boolean
    fun onMouseRelease(mouseX: Double, mouseY: Double, button: Int): Boolean
    fun onMouseDrag(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean
    fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean
    fun onKeyRelease(keyCode: Int, scanCode: Int, modifiers: Int): Boolean
    fun onCharTyped(char: Char, modifiers: Int): Boolean
    fun tick()
    fun isVisible(): Boolean
    fun setVisible(visible: Boolean)
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
    fun getX(): Int
    fun getY(): Int
    fun getWidth(): Int
    fun getHeight(): Int
    fun setPosition(x: Int, y: Int)
    fun setSize(width: Int, height: Int)
}

abstract class BaseUIComponent(
    override val id: String,
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0
) : IUIComponent {
    private var _x: Int = x
    private var _y: Int = y
    private var _width: Int = width
    private var _height: Int = height
    private var _visible: Boolean = true
    private var _enabled: Boolean = true

    override fun getX(): Int = _x
    override fun getY(): Int = _y
    override fun getWidth(): Int = _width
    override fun getHeight(): Int = _height

    override fun setPosition(x: Int, y: Int) {
        this._x = x
        this._y = y
    }

    override fun setSize(width: Int, height: Int) {
        this._width = width
        this._height = height
    }

    override fun isVisible(): Boolean = _visible
    override fun setVisible(visible: Boolean) {
        this._visible = visible
    }

    override fun isEnabled(): Boolean = _enabled
    override fun setEnabled(enabled: Boolean) {
        this._enabled = enabled
    }

    override fun onMouseClick(mouseX: Double, mouseY: Double, button: Int): Boolean = false
    override fun onMouseRelease(mouseX: Double, mouseY: Double, button: Int): Boolean = false
    override fun onMouseDrag(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = false
    override fun onKeyPress(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun onKeyRelease(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun onCharTyped(char: Char, modifiers: Int): Boolean = false
    override fun tick() {}
}

