package com.lyra.player.core.startup.player

internal data class PlayerStartupBootstrapResult(
    val serviceStart: PlayerStartupServiceStart?
)

internal data class PlayerStartupServiceStart(
    val source: String,
    val forceForeground: Boolean
)
