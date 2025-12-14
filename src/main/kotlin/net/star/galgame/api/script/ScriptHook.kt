package net.star.galgame.api.script

import net.star.galgame.dialogue.DialogueScript
import net.star.galgame.dialogue.DialogueEntry

interface IScriptHook {
    fun onScriptLoad(script: DialogueScript)
    fun onScriptUnload(scriptId: String)
    fun onEntryExecute(script: DialogueScript, entry: DialogueEntry)
    fun onEntryComplete(script: DialogueScript, entry: DialogueEntry)
}

interface IScriptHookRegistry {
    fun register(hook: IScriptHook)
    fun unregister(hook: IScriptHook)
    fun getAll(): List<IScriptHook>
}

object ScriptHookRegistry : IScriptHookRegistry {
    private val hooks = mutableListOf<IScriptHook>()

    override fun register(hook: IScriptHook) {
        hooks.add(hook)
    }

    override fun unregister(hook: IScriptHook) {
        hooks.remove(hook)
    }

    override fun getAll(): List<IScriptHook> {
        return hooks.toList()
    }
}

