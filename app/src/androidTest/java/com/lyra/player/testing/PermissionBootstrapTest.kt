package com.lyra.player.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lyra.player.testutil.grantRuntimePermissions
import com.lyra.player.testutil.playbackRuntimePermissions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionBootstrapTest {

    @Test
    fun grantPlaybackRuntimePermissions() {
        grantRuntimePermissions(*playbackRuntimePermissions())
    }
}
