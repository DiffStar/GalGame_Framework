# GalGame Framework API 使用文档

## 目录

- [开发者 API](#开发者-api)
- [内容包开发 API](#内容包开发-api)
- [脚本系统 API](#脚本系统-api)
- [事件系统 API](#事件系统-api)
- [自定义 UI 组件 API](#自定义-ui-组件-api)

---

## 开发者 API

### GalGameAPI

核心 API 入口，提供框架的主要功能。

#### 注册角色

```kotlin
import net.star.galgame.api.developer.GalGameAPI
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component

GalGameAPI.registerCharacter(
    id = "alice",
    name = Component.literal("爱丽丝"),
    portraitPath = ResourceLocation("galgame", "characters/alice.png"),
    expressions = mapOf(
        "happy" to ResourceLocation("galgame", "characters/alice_happy.png"),
        "sad" to ResourceLocation("galgame", "characters/alice_sad.png")
    )
)
```

#### 获取角色

```kotlin
val character = GalGameAPI.getCharacter("alice")
```

#### 注册脚本

```kotlin
import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.dialogue.DialogueEntry

val script = DialogueScript(
    id = "intro",
    entries = listOf(
        DialogueEntry(
            id = "1",
            characterId = "alice",
            text = "你好，欢迎来到这个世界！"
        )
    )
)
GalGameAPI.registerScript(script)
```

#### 内容包管理

```kotlin
import java.nio.file.Paths

val pack = GalGameAPI.loadContentPack(Paths.get("path/to/pack"))
GalGameAPI.setContentPackEnabled("pack_id", true)
GalGameAPI.reloadContentPack("pack_id")
```

### ExtensionPoint

扩展点系统，允许注册自定义扩展。

```kotlin
import net.star.galgame.api.developer.ExtensionPoint
import net.star.galgame.api.developer.ExtensionRegistry

class MyExtension : ExtensionPoint {
    override val id = "my_extension"
    override val version = "1.0.0"
}

val extension = MyExtension()
ExtensionRegistry.register(extension)

val retrieved = ExtensionRegistry.get("my_extension", MyExtension::class.java)
val allExtensions = ExtensionRegistry.getAll(MyExtension::class.java)
```

### PluginManager

插件管理系统。

```kotlin
import net.star.galgame.api.developer.IPlugin
import net.star.galgame.api.developer.PluginManager
import java.nio.file.Paths

class MyPlugin : IPlugin {
    override val id = "my_plugin"
    override val name = "我的插件"
    override val version = "1.0.0"

    override fun onLoad() {
        println("插件加载中...")
    }

    override fun onEnable() {
        println("插件已启用")
    }

    override fun onDisable() {
        println("插件已禁用")
    }
}

val plugin = PluginManager.loadPlugin(Paths.get("plugins/my_plugin.jar"))
PluginManager.setPluginEnabled("my_plugin", true)
```

---

## 内容包开发 API

### ContentPackBuilder

构建内容包的构建器模式。

```kotlin
import net.star.galgame.api.contentpack.ContentPackBuilderFactory
import net.star.galgame.contentpack.ScriptFormat
import java.nio.file.Paths

val pack = ContentPackBuilderFactory.create()
    .manifest(
        ContentPackManifestBuilderFactory.create()
            .id("my_pack")
            .name("我的内容包")
            .version("1.0.0")
            .author("作者名")
            .description("内容包描述")
            .frameworkVersion("1.21.10")
            .addDependency("required_pack", "1.0.0", required = true)
            .build()
    )
    .path(Paths.get("packs/my_pack"))
    .addScript(
        id = "script1",
        path = Paths.get("scripts/script1.json"),
        format = ScriptFormat.JSON,
        content = """
        {
            "id": "script1",
            "entries": [
                {
                    "id": "1",
                    "characterId": "alice",
                    "text": "你好！"
                }
            ]
        }
        """.trimIndent()
    )
    .build()
```

### ContentPackManifestBuilder

构建内容包清单。

```kotlin
import net.star.galgame.api.contentpack.ContentPackManifestBuilderFactory
import net.star.galgame.contentpack.ResourceStructure

val manifest = ContentPackManifestBuilderFactory.create()
    .id("my_pack")
    .name("我的内容包")
    .version("1.0.0")
    .author("作者名")
    .description("这是一个示例内容包")
    .frameworkVersion("1.21.10")
    .minFrameworkVersion("1.21.0")
    .addDependency("base_pack", "1.0.0", required = true)
    .addDependency("optional_pack", "1.0.0", required = false)
    .resourceStructure(
        ResourceStructure(
            scripts = "scripts",
            images = "images",
            audio = "audio"
        )
    )
    .build()
```

### ContentPackRegistry

内容包注册表。

```kotlin
import net.star.galgame.api.contentpack.ContentPackRegistry

ContentPackRegistry.register(pack)
val registeredPack = ContentPackRegistry.get("my_pack")
val allPacks = ContentPackRegistry.getAll()
val isRegistered = ContentPackRegistry.isRegistered("my_pack")
```

---

## 脚本系统 API

### IScriptEngine

自定义脚本引擎。

```kotlin
import net.star.galgame.api.script.IScriptEngine
import net.star.galgame.api.script.ScriptEngineRegistry
import net.star.galgame.api.script.ScriptParseResult
import net.star.galgame.api.script.ScriptValidationResult
import net.star.galgame.api.script.ScriptExecutionContext
import net.star.galgame.api.script.ScriptExecutionResult
import net.star.galgame.contentpack.ScriptFormat
import net.star.galgame.dialogue.DialogueScript

class CustomScriptEngine : IScriptEngine {
    override fun parse(content: String, format: ScriptFormat): ScriptParseResult {
        return ScriptParseResult(
            script = null,
            errors = listOf("解析错误示例"),
            warnings = emptyList()
        )
    }

    override fun validate(script: DialogueScript): ScriptValidationResult {
        return ScriptValidationResult(
            isValid = true,
            errors = emptyList(),
            warnings = emptyList()
        )
    }

    override fun execute(
        script: DialogueScript,
        context: ScriptExecutionContext
    ): ScriptExecutionResult {
        return ScriptExecutionResult(
            success = true,
            result = null,
            errors = emptyList()
        )
    }
}

ScriptEngineRegistry.registerEngine(ScriptFormat.JSON, CustomScriptEngine())
val engine = ScriptEngineRegistry.getEngine(ScriptFormat.JSON)
```

### ScriptHook

脚本钩子系统，监听脚本执行事件。

```kotlin
import net.star.galgame.api.script.IScriptHook
import net.star.galgame.api.script.ScriptHookRegistry
import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.dialogue.DialogueEntry

class MyScriptHook : IScriptHook {
    override fun onScriptLoad(script: DialogueScript) {
        println("脚本加载: ${script.id}")
    }

    override fun onScriptUnload(scriptId: String) {
        println("脚本卸载: $scriptId")
    }

    override fun onEntryExecute(script: DialogueScript, entry: DialogueEntry) {
        println("执行条目: ${entry.id}")
    }

    override fun onEntryComplete(script: DialogueScript, entry: DialogueEntry) {
        println("完成条目: ${entry.id}")
    }
}

val hook = MyScriptHook()
ScriptHookRegistry.register(hook)
```

---

## 事件系统 API

### EventBus

事件总线系统。

```kotlin
import net.star.galgame.api.event.EventBus
import net.star.galgame.api.event.DialogueStartEvent
import net.star.galgame.api.event.DialogueEndEvent
import net.star.galgame.api.event.ChoiceSelectEvent
import net.star.galgame.api.event.VariableChangeEvent

EventBus.subscribe(DialogueStartEvent::class.java) { event ->
    println("对话开始: ${event.scriptId}")
}

EventBus.subscribe(DialogueEndEvent::class.java) { event ->
    println("对话结束: ${event.scriptId}")
}

EventBus.subscribe(ChoiceSelectEvent::class.java) { event ->
    println("选择选项: ${event.choiceId}")
}

EventBus.subscribe(VariableChangeEvent::class.java) { event ->
    println("变量变更: ${event.variableName} = ${event.newValue}")
}

val startEvent = DialogueStartEvent(
    id = "event_1",
    scriptId = "intro"
)
EventBus.post(startEvent)

EventBus.postAsync(startEvent)
```

### 自定义事件

```kotlin
import net.star.galgame.api.event.IEvent

data class CustomEvent(
    override val id: String,
    val customData: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val cancelled: Boolean = false
) : IEvent

EventBus.subscribe(CustomEvent::class.java) { event ->
    println("自定义事件: ${event.customData}")
}

val customEvent = CustomEvent(
    id = "custom_1",
    customData = "示例数据"
)
EventBus.post(customEvent)
```

---

## 自定义 UI 组件 API

### IUIComponent

创建自定义 UI 组件。

```kotlin
import net.star.galgame.api.ui.BaseUIComponent
import net.star.galgame.api.ui.IUIComponent
import net.star.galgame.api.ui.UIComponentRegistry
import net.minecraft.client.gui.GuiGraphics

class CustomButton(
    id: String,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val text: String
) : BaseUIComponent(id, x, y, width, height) {

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        if (!visible) return
        
        val color = if (enabled) 0xFFFFFFFF.toInt() else 0xFF808080.toInt()
        graphics.fill(x, y, x + width, y + height, 0x80000000.toInt())
        graphics.drawString(
            graphics.font,
            text,
            x + width / 2 - graphics.font.width(text) / 2,
            y + height / 2 - 4,
            color
        )
    }

    override fun onMouseClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (enabled && isPointInBounds(mouseX, mouseY)) {
            println("按钮被点击: $id")
            return true
        }
        return false
    }

    private fun isPointInBounds(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height
    }
}

val button = CustomButton("my_button", 10, 10, 100, 20, "点击我")
UIComponentRegistry.register(button)

val component = UIComponentRegistry.get("my_button")
val allComponents = UIComponentRegistry.getAll()
```

### UIComponentFactory

UI 组件工厂。

```kotlin
import net.star.galgame.api.ui.UIComponentFactory

UIComponentFactory.registerFactory(CustomButton::class.java) { id, args ->
    CustomButton(
        id = id,
        x = args[0] as Int,
        y = args[1] as Int,
        width = args[2] as Int,
        height = args[3] as Int,
        text = args[4] as String
    )
}

val button = UIComponentFactory.create(
    CustomButton::class.java,
    "factory_button",
    10, 10, 100, 20, "工厂创建"
)
```

### UITheme

UI 主题系统。

```kotlin
import net.star.galgame.api.ui.UITheme
import net.star.galgame.api.ui.UIThemeRegistry
import net.minecraft.resources.ResourceLocation

val darkTheme = UITheme(
    id = "dark",
    name = "深色主题",
    backgroundColor = 0xFF1E1E1E.toInt(),
    textColor = 0xFFFFFFFF.toInt(),
    accentColor = 0xFF4A90E2.toInt(),
    borderColor = 0xFF333333.toInt(),
    hoverColor = 0xFF2D2D2D.toInt(),
    disabledColor = 0xFF808080.toInt(),
    font = ResourceLocation("galgame", "fonts/default"),
    backgroundTexture = ResourceLocation("galgame", "textures/ui/background.png")
)

UIThemeRegistry.register(darkTheme)
UIThemeRegistry.setActiveTheme("dark")
val activeTheme = UIThemeRegistry.getActiveTheme()
```

---

## 完整示例

### 创建一个简单的内容包

```kotlin
import net.star.galgame.api.developer.GalGameAPI
import net.star.galgame.api.contentpack.ContentPackBuilderFactory
import net.star.galgame.api.contentpack.ContentPackManifestBuilderFactory
import net.star.galgame.contentpack.ScriptFormat
import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.dialogue.DialogueEntry
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component
import java.nio.file.Paths

fun createExamplePack() {
    GalGameAPI.registerCharacter(
        id = "npc1",
        name = Component.literal("NPC"),
        portraitPath = ResourceLocation("galgame", "characters/npc1.png")
    )

    val script = DialogueScript(
        id = "example_script",
        entries = listOf(
            DialogueEntry(
                id = "1",
                characterId = "npc1",
                text = "这是一个示例对话"
            )
        )
    )
    GalGameAPI.registerScript(script)

    val pack = ContentPackBuilderFactory.create()
        .manifest(
            ContentPackManifestBuilderFactory.create()
                .id("example_pack")
                .name("示例内容包")
                .version("1.0.0")
                .author("开发者")
                .description("这是一个示例内容包")
                .frameworkVersion("1.21.10")
                .build()
        )
        .path(Paths.get("packs/example_pack"))
        .addScript(
            id = "example_script",
            path = Paths.get("scripts/example.json"),
            format = ScriptFormat.JSON,
            content = """
            {
                "id": "example_script",
                "entries": [
                    {
                        "id": "1",
                        "characterId": "npc1",
                        "text": "你好！"
                    }
                ]
            }
            """.trimIndent()
        )
        .build()

    GalGameAPI.loadContentPack(pack.packPath)
}
```

---

## 注意事项

1. 所有 API 都是线程安全的，可以在多线程环境中使用
2. 注册的组件和扩展需要在适当的时候注销，避免内存泄漏
3. 事件处理器应该快速执行，避免阻塞主线程
4. UI 组件需要在渲染线程中操作
5. 内容包加载失败时会返回包含错误信息的对象

---

## 版本兼容性

- 框架版本: 1.21.10
- Minecraft 版本: 1.21
- NeoForge 版本: 21.10.64
- Kotlin 版本: 2.1.10

---

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这些 API。

