package com.example

import com.example.data.cloud.ClubNicknameStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `invite deep link parser extracts uppercase alphanumeric club codes`() {
        assertEquals(
            "K7M9P2",
            ClubNicknameStore.parseInviteCodeFromUrl("pushupclub://join?code=k7m9p2")
        )
        assertEquals(
            "AB34CD56",
            ClubNicknameStore.parseInviteCodeFromUrl("https://play.google.com/store/apps/details?id=com.aistudio.pushuptracker.fylsqb&code=ab34cd56&ref=share")
        )
        assertEquals("PUSH7X", ClubNicknameStore.parseInviteCodeFromUrl("  push-7x  "))
        assertNull(ClubNicknameStore.parseInviteCodeFromUrl("pushupclub://join?code=12"))
        assertNull(ClubNicknameStore.parseInviteCodeFromUrl(null))
    }

    @Test
    fun `nickname sanitizer trims collapses whitespace and generates anonymized fallback`() {
        assertEquals("Iron Plank", ClubNicknameStore.sanitizeNickname("  Iron   Plank  "))
        assertEquals("Athlete-8F9A", ClubNicknameStore.defaultAnonymizedName("user_8f9a"))
        assertEquals("Athlete", ClubNicknameStore.defaultAnonymizedName(null))
    }
}
