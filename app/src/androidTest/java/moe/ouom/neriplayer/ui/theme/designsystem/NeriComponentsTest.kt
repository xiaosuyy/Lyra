package moe.ouom.neriplayer.ui.theme.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import moe.ouom.neriplayer.data.model.SongItem
import moe.ouom.neriplayer.testutil.assumeComposeHostAvailable
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NeriComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun assumeDeviceUnlocked() {
        assumeComposeHostAvailable()
    }

    private fun song(name: String, artist: String) = SongItem(
        id = name.hashCode().toLong(),
        name = name,
        artist = artist,
        album = "Album",
        albumId = 0L,
        durationMs = 100_000L,
        coverUrl = null
    )

    @Test
    fun songRowRendersAndClickWorks() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                NeriSongRow(
                    song = song("测试歌曲", "测试艺术家"),
                    onClick = { clicked = true },
                    onMore = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("测试歌曲").assertIsDisplayed()
        composeRule.onNodeWithText("测试艺术家").assertIsDisplayed()
        composeRule.onNodeWithText("测试歌曲").performClick()
        assertTrue(clicked)
    }

    @Test
    fun sectionHeaderShowsTitleAndMoreClickWorks() {
        var moreClicked = false
        composeRule.setContent {
            MaterialTheme {
                NeriSectionHeader(
                    title = "分区标题",
                    subtitle = "副标题",
                    moreText = "更多",
                    onMoreClick = { moreClicked = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("分区标题").assertIsDisplayed()
        composeRule.onNodeWithText("更多").assertIsDisplayed().performClick()
        assertTrue(moreClicked)
    }

    @Test
    fun primaryButtonRendersAndClicks() {
        var clicked = false
        composeRule.setContent {
            MaterialTheme {
                NeriPrimaryButton(onClick = { clicked = true }) { androidx.compose.material3.Text("播放") }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("播放").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }

    @Test
    fun cardRendersTitle() {
        composeRule.setContent {
            MaterialTheme {
                NeriAlbumCard(title = "专辑名", subtitle = "歌手", coverUrl = null, onClick = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("专辑名").assertIsDisplayed()
    }
}
