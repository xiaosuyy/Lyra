package com.lyra.player.data.settings

import com.lyra.player.R
import com.lyra.player.data.settings.generated.AutoSettingsMetadata
import com.lyra.player.data.settings.generated.AutoSettingsSections
import com.lyra.player.ksp.annotations.SettingUiType
import com.lyra.player.ksp.annotations.SettingValueType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BiliSponsorBlockSettingTest {
    @Test
    fun `bili sponsor block switch is opt in and belongs to playback`() {
        val setting = AutoSettingsSchema.playback.biliSponsorBlockEnabled
        val metadata = AutoSettingsMetadata.setting("bili_sponsor_block_enabled")

        assertEquals("bili_sponsor_block_enabled", setting.key)
        assertFalse(setting.defaultValue)
        assertEquals(SettingValueType.Boolean, metadata?.valueType)
        assertEquals(SettingUiType.Switch, metadata?.ui)
        assertEquals(AutoSettingsSections.playback, metadata?.section)
        assertEquals(R.string.settings_bili_sponsor_block, metadata?.titleRes)
    }
}
