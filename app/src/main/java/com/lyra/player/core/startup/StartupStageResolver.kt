package com.lyra.player.core.startup

internal object StartupStageResolver {
    fun resolve(disclaimerAccepted: Boolean?): StartupStage {
        return when (disclaimerAccepted) {
            null -> StartupStage.Loading
            false -> StartupStage.Disclaimer
            true -> StartupStage.Main
        }
    }

    fun resolve(
        disclaimerAccepted: Boolean?,
        pendingDisclaimerAccepted: Boolean
    ): StartupStage {
        if (!pendingDisclaimerAccepted) {
            return resolve(disclaimerAccepted)
        }
        return resolve(disclaimerAccepted = true)
    }
}
