package com.fathtube.app.discord

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiscordFossIsolationTest {
    @Test
    fun `foss classpath excludes functional Discord implementation`() {
        val forbiddenClasses = listOf(
            "com.fathtube.app.discord.DiscordTokenStore",
            "com.fathtube.app.discord.DiscordAuthTokens",
            "com.fathtube.app.discord.DiscordPlaybackSource",
            "com.fathtube.app.discord.DiscordPresenceCoordinator",
            "com.fathtube.app.discord.KizzyDiscordPresenceTransport",
            "com.fathtube.app.discord.KizzyGatewayProtocol",
        )

        forbiddenClasses.forEach { className ->
            assertThat(runCatching { Class.forName(className) }.isFailure).isTrue()
        }
    }

    @Test
    fun `foss runtime reports Discord unavailable`() {
        assertThat(DiscordPresenceRuntime.settingsState.value.isAvailable).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.isEnabled).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.summary)
            .isEqualTo(DiscordSettingsSummary.UNAVAILABLE)
    }
}
